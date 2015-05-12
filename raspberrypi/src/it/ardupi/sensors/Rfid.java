package it.ardupi.sensors;

public class Rfid extends Sensor {
	
	private String cardId;
	private long timestamp = 0;

	@Override
	public void incomingValues(String[] values) {
		
		if (values.length == 2) {
			if (cardId == null || !cardId.equals(values[1]) || System.currentTimeMillis() - timestamp > 1000 ) {
				cardId = values[1];
				timestamp = System.currentTimeMillis();
				notifyValueChangeListeners();
			}
		}
	}
	
	@Override
	public void setValues(String... values) {}

	public String getCardId() {
		return cardId;
	}

	public void setCardId(String cardId) {
		this.cardId = cardId;
	}
	
	
}
