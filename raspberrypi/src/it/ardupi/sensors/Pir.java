package it.ardupi.sensors;

public class Pir extends Sensor {
	
	private int status = 0;
	private long timestamp = 0;

	@Override
	public void incomingValues(String[] values) {
		if (values.length == 2) {
			if (System.currentTimeMillis() - timestamp > 1000 ) {
				try {
					status = Integer.parseInt(values[1]);
					notifyValueChangeListeners();
				} catch (Exception e) {}
				
				timestamp = System.currentTimeMillis();
			}
		}
	}

	@Override
	public void setValues(String... values) {}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
}
