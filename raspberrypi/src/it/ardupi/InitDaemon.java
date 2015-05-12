package it.ardupi;

import com.pi4j.io.i2c.I2CBus;

import it.ardupi.sensors.Buzzer;
import it.ardupi.sensors.Pir;
import it.ardupi.sensors.Rfid;
import it.ardupi.sensors.Sensor;
import it.ardupi.sensors.Temp;
import it.ardupi.sensors.Sensor.ValueChangeListener;
import it.ardupi.utils.Utils;

public class InitDaemon {
	public static void main(String[] args) {
		
		DomoBus bus = new DomoBus(I2CBus.BUS_1);
		final DomoNode node = new DomoNode(0x04);
		bus.addNode(node);
		
		node.addSensor(10, Sensor.RC522, new int[]{10,9});
		node.addSensor(11, Sensor.GENERIC_RELAY_MODULE, 2);
		node.addSensor(12, Sensor.VOLTMETER, 1);
		node.addSensor(13, Sensor.DHT11, 5);
		node.addSensor(14, Sensor.ACTIVE_BUZZER, 3);
		node.addSensor(15, Sensor.GENERIC_PIR, 4);
		
		node.getSensor(13).addValueChangeListener(new ValueChangeListener() {

			@Override
			public void onValueChangeListener() {
				int temperature = ((Temp)node.getSensor(13)).getTemperature();
				int humidity = ((Temp)node.getSensor(13)).getHumidity();
				
				System.out.println("temperatura: " + temperature + "°C, umidità: " + humidity + "%");
			}
			
		});
		
		node.getSensor(10).addValueChangeListener(new ValueChangeListener() {
			@Override
			public void onValueChangeListener() {
				String cardId = ((Rfid) (node.getSensor(10))).getCardId();
				System.out.println("nuova card letta: " + cardId);
				if (cardId.equals("56877365"))
					node.getSensor(11).setValues(new String[] {""+1});
				else if (cardId.equals("736adec7"))
					node.getSensor(11).setValues(new String[] {""+0});
				node.getSensor(14).setValues(new String[] {""+2,""+Buzzer.FAST});
			}
		});
		
		node.getSensor(15).addValueChangeListener(new ValueChangeListener() {

			@Override
			public void onValueChangeListener() {
				int value = ((Pir) (node.getSensors().get(15))).getStatus();
				
				if (value == 1) {
					node.getSensor(11).setValues(new String[] {""+1});
				}
				else {
					node.getSensor(11).setValues(new String[] {""+0});
				}
			}
			
		});
		
		/*
		node.getSensor(12).addValueChangeListener(new ValueChangeListener() {

			@Override
			public void onValueChangeListener() {
				int value = ((Voltmeter) (node.getSensors().get(12))).getVoltmeterValue();
				
				System.out.println("nuovo valore luminosità: " + value);
				
				if (value > 600) {
					node.getSensors().get(11).setValues(new String[] {""+0});
				}
				else {
					node.getSensors().get(11).setValues(new String[] {""+1});
				}
			}
			
		});*/
		bus.startCommunication();
		System.out.println("ok");
		Utils.sleep(5000);
		System.out.println("Resetto");
		node.resetNode();
		
	}
}
