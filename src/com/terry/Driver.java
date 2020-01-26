package com.terry;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Driver {
	private static Robot robot;
	
	private static int DELAY_POINT; //time taken to move cursor, in ms
	private static int DELAY_TYPE;	//time taken to press a key, in ms
	private static int DELAY_CLICK;	//time taken to click a mouse button
	private static int ITER_MAX;	//max iterations a single action should require
	private static int POINT_SIZE;	//collision box width around the cursor
	
	private static Dimension screen;
	
	public static void init() throws DriverException {
		ITER_MAX = 50;
		POINT_SIZE = 20;
		
		try {
			robot = new Robot();
			robot.setAutoDelay(10);
			robot.setAutoWaitForIdle(true);
			
			DELAY_POINT = 5;
			DELAY_TYPE = 20;
			DELAY_CLICK = 100;
			
			screen = Toolkit.getDefaultToolkit().getScreenSize();
			
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
		
		double s = 0.1; //speed
		
		try {
			int i=0;
			
			while (i < ITER_MAX && (Math.abs(mx-x) > POINT_SIZE || Math.abs(my-y) > POINT_SIZE)) {
				mx += (double)(x-mx) * s;
				my += (double)(y-my) * s;
				
				/*
				 * See issue here for why this is necessary: https://stackoverflow.com/q/48837741/10200417
				 */
				while (mouse.x != mx || mouse.y != my) {
					robot.mouseMove(mx, my);
					mouse = MouseInfo.getPointerInfo().getLocation();
				}
				
				Thread.sleep(DELAY_POINT);				
				i++;
			}
			
			robot.mouseMove(x, y);
		}
		catch (InterruptedException e) {
			Logger.logError("driver pointing interrupted");
		}
	}
	
	public static void type(String str) {
		char[] chars = str.toCharArray();
		int key;
		
		try {
			for (int c=0; c<chars.length; c++) {
				key = KeyEvent.getExtendedKeyCodeForChar(chars[c]);
				
				if (key == KeyEvent.VK_UNDEFINED) {
					Logger.logError("unknown keycode for char " + chars[c]);
				}
				else {
					robot.keyPress(key);
					robot.keyRelease(key);
				}
				
				Thread.sleep(DELAY_TYPE);
			}
		}
		catch (InterruptedException e) {
			Logger.logError("driver typing interrupted");
		}
	}
	
	public static void clickLeft() {
		robot.mousePress(InputEvent.BUTTON1_MASK);
	    robot.delay(DELAY_CLICK);
	    robot.mouseRelease(InputEvent.BUTTON1_MASK);
	    robot.delay(DELAY_CLICK);
	}
	
	public static void clickRight() {
		robot.mousePress(InputEvent.BUTTON2_MASK);
	    robot.delay(DELAY_CLICK);
	    robot.mouseRelease(InputEvent.BUTTON2_MASK);
	    robot.delay(DELAY_CLICK);
	}
	
	public static Dimension getScreen() {
		return screen;
	}
	
	public static abstract class DriverThread extends Thread {
		@Override
		public abstract void run();
		
		/*
		 * quick way to allow other threads to interrupt this one without throwing
		 * an access exception.
		 */
		public void quit() {
			interrupt();
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
