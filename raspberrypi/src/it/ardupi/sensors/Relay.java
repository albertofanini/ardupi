package it.ardupi.sensors;

public class Relay extends Sensor {
	
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
	public void setValues(String... values) {
		if (values.length == 1) {
			try {
				status = Integer.parseInt(values[0]);
				
				notifySetValueListeners();
			} catch (Exception e) {}
		}
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}
	
	
	
}
