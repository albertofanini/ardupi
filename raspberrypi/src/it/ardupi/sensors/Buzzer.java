package it.ardupi.sensors;

public class Buzzer extends Sensor {

	public static final int SLOW = 2;
	public static final int NORMAL = 0;
	public static final int FAST = 1;
	
	private int buzzNum = 0;
	private int buzzType = 0;

	@Override
	public void incomingValues(String[] values) {}

	@Override
	public void setValues(String... values) {
		
		if (values.length == 2) {
			try {
				buzzNum = Integer.parseInt(values[0]);
				buzzType = Integer.parseInt(values[1]);
				
				notifySetValueListeners();
			} catch (Exception e) {}
		}
	}

	public int getBuzzNum() {
		return buzzNum;
	}

	public int getBuzzType() {
		return buzzType;
	}

	public void setBuzzNum(int buzzNum) {
		this.buzzNum = buzzNum;
	}

	public void setBuzzType(int buzzType) {
		this.buzzType = buzzType;
	}
	
	

}
