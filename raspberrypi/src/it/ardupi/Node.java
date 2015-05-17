package it.ardupi;

import it.ardupi.sensors.Buzzer;
import it.ardupi.sensors.Pir;
import it.ardupi.sensors.Relay;
import it.ardupi.sensors.Rfid;
import it.ardupi.sensors.Sensor;
import it.ardupi.sensors.Temp;
import it.ardupi.sensors.Voltmeter;
import it.ardupi.sensors.Sensor.SetValueListener;
import it.ardupi.utils.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.pi4j.io.i2c.I2CDevice;

public class Node {
	
	private I2CDevice device;
	private Bus bus;
	private int deviceAddr;
	private boolean lastSetupFailed;
	private int failures;
	
	private HashMap<Integer, Sensor> sensors;
	
	public static final char SETUP_MSG = '*';
	public static final char SUPER_SEPARATOR = '|';
	public static final char SEPARATOR = ';';
	public static final char START_MSG = '@';
	public static final char RESET_MSG = '#';
	public static final char END_MSG = '!';
	public static final char END_CHSUM = '^';
	
	
	public Node(int deviceAddr){
		this.deviceAddr = deviceAddr;
		sensors = new HashMap<Integer, Sensor>();
		lastSetupFailed = false;
	}
	
	private boolean initNode(){
		try {
			if (bus != null && bus.getBus() == null)
				return false;
			
			device = bus.getBus().getDevice(deviceAddr);
			
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public boolean addSensor(int id, int type, int pin) {
		return addSensor(id, type, new int[]{pin});
	}
	
	public boolean addSensor(final int id, int type, int[] pins) {
		Sensor s;
		switch (type) {
			case Sensor.RC522:
				s = new Rfid();
				break;
			case Sensor.ACTIVE_BUZZER:
				s = new Buzzer();
				s.addSetValueListener(new SetValueListener() {
					@Override
					public void onSetValueListener() {
						writeMessageAndCheck(getSetMessage(new String[] {
								"" + id,
								"" + ((Buzzer)Node.this.getSensors().get(id)).getBuzzNum(),
								"" + ((Buzzer)Node.this.getSensors().get(id)).getBuzzType()})
								);
					}
				});
				break;
			case Sensor.GENERIC_PIR:
				s = new Pir();
				break;
			case Sensor.GENERIC_RELAY_MODULE:
				s = new Relay();
				s.addSetValueListener(new SetValueListener() {
					@Override
					public void onSetValueListener() {
						if (!
						writeMessageAndCheck(getSetMessage(new String[] {
								"" + id,
								"" + ((Relay)Node.this.getSensors().get(id)).getStatus()})
								)) {
							System.out.println("errore");
						}
					}
				});
				break;
			case Sensor.DHT11:
				s = new Temp();
				break;
			case Sensor.VOLTMETER:
				s = new Voltmeter();
				break;
			default:
				return false;
		}
		s.setId(id);
		s.setType(type);
		
		if (pins.length > 0)
			s.setPins(pins);
		else return false;
		
		return addSensor(s);
	}
	
	private boolean addSensor(Sensor s) {

		if (getBus() == null)
			return false;
		
		s.setBus(this.getBus());
		
		if (!sensors.containsKey(s.getId())) {
			sensors.put(s.getId(), s);
			return true;
		}
		else
			return false;
	}
	
	private boolean setupSensors() {
		
		Utils.sleep(10);
		
		Iterator<Entry<Integer, Sensor>> it = sensors.entrySet().iterator();
		
		while (it.hasNext()) {
			String setupMsg = "" + START_MSG + SETUP_MSG;
			
			Map.Entry<Integer, Sensor> pair = (Map.Entry<Integer, Sensor>)it.next();
			Sensor s = pair.getValue();
			setupMsg += "" + pair.getKey() + SEPARATOR + s.getType();
			for (int pin : s.getPins()) {
				setupMsg += "" + SEPARATOR + pin;
			}
			
			setupMsg += END_MSG;
			setupMsg += "" + getChecksum(setupMsg) + END_CHSUM;
			
			if (!writeMessageAndCheck(setupMsg))
				return false;
			
			Utils.sleep(10);
		}
		
		if (readMessage().startsWith("@nosetup!"))
			return false;
		else return true;
	}
	
	public void resetNode() {
		writeMessageAndCheck(getSetMessage(new String[] {"#"}));
	}
	
	public void loop() {
		
		// check if node was initialized
		if (device == null) {
			initNode();
		}
		
		// get message from node
		String message = readMessage();
		
		// check if setup was done
		if (message.startsWith("@nosetup!") || lastSetupFailed) {
			lastSetupFailed = !setupSensors();
			if (lastSetupFailed) {
				return;
			}
		}
		
		// parse sensors status
		message = message.split(""+END_MSG)[0];
		if (message.length() > 1)
			message = message.substring(1);
		else
			return;
		
		String[] sensorMessages = message.split("\\"+SUPER_SEPARATOR);
		
		for (String sensorMessage : sensorMessages) {
			
			String[] values = sensorMessage.split(""+SEPARATOR);
			if (values.length > 0) {
				
				try {
					Integer.parseInt(values[0]);
				} catch (Exception e) {
					continue;
				}
				
				Sensor s = sensors.get(Integer.parseInt(values[0]));
				if (s != null) {
					s.incomingValues(values);
				}
			}
		}
	}
	
	private boolean writeMessageAndCheck(String message) {
		boolean result = false;
		int tries = 5;
		
		do {
			tries--;
			writeMessage(message);
			result = readMessage().equals(message);
		} while (!result && tries >= 0);
		
		return result;
	}
	
	private boolean writeMessage(String message) {
		synchronized(this) {
			Utils.sleep(10);
			
			try {
				device.write(message.getBytes(), 0, message.getBytes().length);
				Utils.sleep(10);
				return true;
			} catch (IOException e) {
				// let's try a second time
				return false;
			}
		}
	}
	
	private String readMessage() {
		synchronized(this) {
			String message = "";
			while (true) {
				try {
					int c = device.read();
					
					message += (char)c;
					
					if (c == START_MSG)
						message = "" + START_MSG;
					if (c == END_CHSUM) {
						String[] msgSplit = message.split("" + END_MSG);
						if (msgSplit.length == 2) {
							String checksum = msgSplit[1].substring(0, msgSplit[1].length()-1);
							String realMessage = "" + msgSplit[0] + END_MSG;
							
							int localChecksum = getChecksum(realMessage);
							
							if (Integer.parseInt(checksum) != localChecksum) {
								message = "";
							}
						}
						else
							message = "";
						
						break;
					}
					failures = 0;
				} catch (Exception e) {
					failures++;
					if (failures > 5)
						Utils.sleep(1000);
					System.out.println("I/O Error");
				}
			}
			return message;
		}
	}
	
	private String getSetMessage(String... values) {
		if (values.length > 0) {
			String message = "" + START_MSG;
			for (int i = 0; i < values.length; i++) {
				if (i != 0)
					message += "" + SEPARATOR;
				message += values[i];
			}
			message += "" + END_MSG;
			message += "" + getChecksum(message) + "" + END_CHSUM;
			return message;
		}
		else return "";
	}
	
	private int getChecksum(String msg) {
		int checksum = 0;
		
		for (int i = 0; i < msg.length(); i++) {
			checksum ^= msg.charAt(i);
		}
		
		return checksum;
	}

	public HashMap<Integer, Sensor> getSensors() {
		return sensors;
	}
	
	public Sensor getSensor(int key) {
		return sensors.get(key);
	}

	public int getDeviceAddr() {
		return deviceAddr;
	}

	public I2CDevice getDevice() {
		return device;
	}

	public Bus getBus() {
		return bus;
	}

	public void setBus(Bus bus) {
		this.bus = bus;
	}
}