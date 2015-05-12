package it.ardupi.sensors;

public class Temp extends Sensor {
	
	private int humidity;
	private int temperature;

	@Override
	public void incomingValues(String[] values) {
		if (values.length == 3) {
			try {
				int newTemperature = Integer.parseInt(values[1]);
				int newHumidity = Integer.parseInt(values[2]);
				if (temperature != newTemperature || humidity != newHumidity) {
					temperature = newTemperature;
					humidity = newHumidity;
					notifyValueChangeListeners();
				}
			} catch (Exception e) {}
		}
	}

	@Override
	public void setValues(String... values) {}

	public int getHumidity() {
		return humidity;
	}

	public void setHumidity(int humidity) {
		this.humidity = humidity;
	}

	public int getTemperature() {
		return temperature;
	}

	public void setTemperature(int temperature) {
		this.temperature = temperature;
	}
}
