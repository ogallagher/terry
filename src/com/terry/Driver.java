package com.terry;

import java.awt.AWTException;
import java.awt.Robot;

public class Driver {
	private static Robot robot;
	
	public static void init() throws DriverException {
		try {
			robot = new Robot();
		}
		catch (AWTException e) {
			throw new DriverException("could not connect to the device display");
		}
		catch (SecurityException e) {
			throw new DriverException("do not have permission to control the mouse and keyboard");
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
