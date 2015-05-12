package it.ardupi;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;

import it.ardupi.utils.Utils;

public class DomoBus {
	private HashMap<Integer, DomoNode> nodes;
	private I2CBus bus;
	private int address;
	private Thread loopThread;
	private boolean loopRunning;
	
	public DomoBus(int address) {
		this.address = address;
		nodes = new HashMap<Integer, DomoNode>();
		loopRunning= false;
	}
	
	private boolean initBus() {
		try {
			bus = I2CFactory.getInstance(I2CBus.BUS_1);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	public void addNode(DomoNode node){
		node.setBus(this);
		nodes.put(node.getDeviceAddr(), node);
	}

	public I2CBus getBus() {
		return bus;
	}

	public int getAddress() {
		return address;
	}

	public HashMap<Integer, DomoNode> getNodes() {
		return nodes;
	}
	
	private void startLoop() {
		loopRunning = true;
		loopThread = new Thread() {

			@Override
			public void run() {
				
				while (true) {
					
					if (bus == null)
						if (initBus())
							continue;
					
					if (!loopRunning)
						return;
					
					Iterator<Entry<Integer, DomoNode>> it = nodes.entrySet().iterator();
					
					while (it.hasNext()) {
						Map.Entry<Integer, DomoNode> pair = (Map.Entry<Integer, DomoNode>)it.next();
						
						pair.getValue().loop();
						Utils.sleep(10);
					}
				}
			}
			
		};
		loopThread.start();
	}

	public Thread getLoopThread() {
		return loopThread;
	}

	public boolean isLoopRunning() {
		return loopRunning;
	}

	public void startCommunication() {
		startLoop();
		this.loopRunning = true;
	}
	
	public void stopCommunication() {
		this.loopRunning = false;
	}
	
	

}
