package it.ardupi.sensors;

public class Voltmeter extends Sensor {
	
	private int voltmeterValue;
	private long timestamp = 0;

	@Override
	public void incomingValues(String[] values) {
		if (values.length == 2) {
			if (System.currentTimeMillis() - timestamp > 1000 ) {
				try {
					voltmeterValue = Integer.parseInt(values[1]);
					notifyValueChangeListeners();
				} catch (Exception e) {}
				
				timestamp = System.currentTimeMillis();
			}
		}
	}

	@Override
	public void setValues(String... values) {}

	public int getVoltmeterValue() {
		return voltmeterValue;
	}

	public void setVoltmeterValue(int voltmeterValue) {
		this.voltmeterValue = voltmeterValue;
	}
}
