package it.ardupi.utils;

public class Utils {
	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {}  
	}
}
