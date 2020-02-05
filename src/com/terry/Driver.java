package com.terry;

import java.awt.AWTException;
import java.awt.AWTPermission;
import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.security.AccessControlException;
import java.security.AccessController;
import java.util.Iterator;
import java.util.LinkedList;

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
		
		AWTPermission robotPermission = new AWTPermission("createRobot", null);
		
		try {
			//AccessController.checkPermission(robotPermission); this does not work, always throws security exception
			
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
		char[] chars = str.toLowerCase().toCharArray();
		int key = 0;
		char[] alias = new char[3];
		
		try {
			for (int c=0; c<chars.length; c++) {
				key = KeyEvent.getExtendedKeyCodeForChar(chars[c]);
				
				if (key == KeyEvent.VK_UNDEFINED) {
					Logger.logError("unknown keycode for char " + chars[c]);
				}
				else if ((key >= KeyEvent.VK_0 && key <= KeyEvent.VK_9) || (key >= KeyEvent.VK_A && key <= KeyEvent.VK_Z)) { 
					//alphanumeric
					robot.keyPress(key);
					robot.keyRelease(key);
				}
				else if ((key == KeyEvent.VK_NUMBER_SIGN)) {
					//control chars that cannot be expressed as chars in a string are escaped and given codes. ex: #cmd+del) #shf) #up)
					c++; //skip hashtag
					
					char chr = '#';
					boolean go = true;
					LinkedList<Integer> keys = new LinkedList<Integer>();
					
					for (int i=0; c<chars.length && go; c++) {
						chr = chars[c];
						
						if (chr == '+') {
							//key in combo, including shifts
							keys.addAll(Utilities.keyCodesFromAlias(String.valueOf(alias)));
							
							i=0;
						}
						else if (chr == ')') {
							//keys done
							keys.addAll(Utilities.keyCodesFromAlias(String.valueOf(alias)));
							
							Iterator<Integer> iterator = keys.iterator();
							while (iterator.hasNext()) {
								robot.keyPress(iterator.next());
							}
							iterator = keys.descendingIterator();
							while (iterator.hasNext()) {
								robot.keyRelease(iterator.next());
							}
							
							go = false;
							c--; //otherwise incremented twice by outer loop
						}
						else {
							alias[i] = chr;
							i++;
						}
					}
				}
				else { 
					//control, punctuation
					switch (key) {
						case KeyEvent.VK_SPACE:
						case KeyEvent.VK_PERIOD:
						case KeyEvent.VK_COMMA:
						case KeyEvent.VK_SLASH:
						case KeyEvent.VK_SEMICOLON:
						case KeyEvent.VK_QUOTE:
						case KeyEvent.VK_OPEN_BRACKET:
						case KeyEvent.VK_CLOSE_BRACKET:
						case KeyEvent.VK_BACK_SLASH:
						case KeyEvent.VK_BACK_QUOTE:
						case KeyEvent.VK_EQUALS:
						case KeyEvent.VK_ENTER:
						case KeyEvent.VK_ACCEPT:
						case KeyEvent.VK_TAB:
						case KeyEvent.VK_MINUS:
							robot.keyPress(key);
							robot.keyRelease(key);
							break;
					}
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
	
	public static BufferedImage captureScreen() throws DriverException {
		try {
			//BufferedImage capture = robot.createScreenCapture(new Rectangle(screen.width,screen.height));
			BufferedImage capture = robot.createScreenCapture(new Rectangle(0,0,500,60)); //apple icon
			
			Widget testWidget = new Widget("test widget");
			testWidget.setAppearance(capture);
			
			return capture;
		}
		catch (SecurityException e) {
			throw new DriverException("not permitted to view the screen");
		}
	}
	
	public static Dimension getScreen() {
		return screen;
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
