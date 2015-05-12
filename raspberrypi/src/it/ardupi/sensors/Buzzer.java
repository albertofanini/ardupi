package it.ardupi.sensors;

public class Buzzer extends Sensor {

	public static final int SLOW = 2;
	public static final int NORMAL = 0;
	public static final int FAST = 1;
	
	private int buzz_num = 0;
	private int buzz_type = 0;

	@Override
	public void incomingValues(String[] values) {}

	@Override
	public void setValues(String... values) {
		
		if (values.length == 2) {
			try {
				buzz_num = Integer.parseInt(values[0]);
				buzz_type = Integer.parseInt(values[1]);
				
				notifySetValueListeners();
			} catch (Exception e) {}
		}
	}

	public int getBuzzNum() {
		return buzz_num;
	}

	public int getBuzzType() {
		return buzz_type;
	}

	public void setBuzzNum(int buzz_num) {
		this.buzz_num = buzz_num;
	}

	public void setBuzzType(int buzz_type) {
		this.buzz_type = buzz_type;
	}
	
	

}
