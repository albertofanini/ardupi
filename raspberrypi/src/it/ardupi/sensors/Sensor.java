package it.ardupi.sensors;

import java.util.ArrayList;
import java.util.List;

import it.ardupi.DomoBus;
import it.ardupi.DomoNode;

public abstract class Sensor {

	List<ValueChangeListener> valueChangeListeners = new ArrayList<ValueChangeListener>();
	List<SetValueListener> setValueListeners = new ArrayList<SetValueListener>();
	
	public static final int UNDEFINED = 0;
	public static final int DHT11 = 1;
	public static final int RC522 = 2;
	public static final int GENERIC_PIR = 3;
	public static final int ACTIVE_BUZZER = 4;
	public static final int GENERIC_RELAY_MODULE = 5;
	public static final int VOLTMETER = 6;
	
	private int id;
	private int type;
	private int[] pins;
	private DomoNode node;
	private DomoBus bus;
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public int[] getPins() {
		return pins;
	}
	public void setPins(int[] pins) {
		this.pins = pins;
	}
	public DomoNode getNode() {
		return node;
	}
	public void setNode(DomoNode node) {
		this.node = node;
	}
	public DomoBus getBus() {
		return bus;
	}
	public void setBus(DomoBus bus) {
		this.bus = bus;
	}

	public abstract void incomingValues(String[] values);
	
	public abstract void setValues(String... values);
	
	public void notifyValueChangeListeners() {
        for (ValueChangeListener hl : valueChangeListeners)
            hl.onValueChangeListener();
    }
	public void notifySetValueListeners() {
        for (SetValueListener hl : setValueListeners)
            hl.onSetValueListener();
    }
	public void addValueChangeListener(ValueChangeListener listener) {
        valueChangeListeners.add(listener);
    }
	public void addSetValueListener(SetValueListener listener) {
		setValueListeners.add(listener);
    }
	public interface ValueChangeListener {
	    public void onValueChangeListener();
	}
	
	public interface SetValueListener {
	    public void onSetValueListener();
	}
}
