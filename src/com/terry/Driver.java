package com.terry;

import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.KeyEvent;

public class Driver {
	private static Robot robot;
	
	private static int DELAY_POINT; //time taken to move cursor, in ms
	private static int DELAY_TYPE;	//time taken to press a key, in ms
	
	public static void init() throws DriverException {
		try {
			robot = new Robot();
			
			DELAY_POINT = 250;
			DELAY_TYPE = 10;
			
			Logger.log("driver init success");
		}
		catch (SecurityException e) {
			throw new DriverException("do not have permission to control the mouse and keyboard");
		} 
		catch (AWTException e) {
			throw new DriverException("could not connect driver to screen");
		}
	}
	
	public static void point(int x, int y) {
		Point mouse = MouseInfo.getPointerInfo().getLocation();
		int mx = mouse.x;
		int my = mouse.y;
		
		double s = 1.0/DELAY_POINT; //speed
		
		while (mx != x || my != y) {
			mx += (x-mx) * s;
			my += (y-my) * s;
			
			robot.mouseMove(mx, my);
		}
	}
	
	public static void type(String str) {
		char[] chars = str.toCharArray();
		int key;
		
		try {
			for (int c=0; c<chars.length; c++) {
				key = KeyEvent.getExtendedKeyCodeForChar(chars[c]);
				
				robot.keyPress(key);
				robot.keyRelease(key);
				
				Thread.sleep(DELAY_TYPE);
			}
		}
		catch (InterruptedException e) {
			Logger.logError("typing interrupted");
		}
	}
	
	public static class DriverException extends Exception {
		private static final long serialVersionUID = -6905991461353507875L;
		private String message;

		public DriverException(String message) {
			this.message = message;
		}
		
		public DriverException() {
			this.message = "driver failed for unknown reason";
		}
		
		@Override
		public String getMessage() {
			return message;
		}
	}
}
