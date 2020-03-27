package com.terry;

import java.awt.Dimension;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.util.Iterator;
import java.util.LinkedList;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.robot.*;
import javafx.scene.shape.Rectangle;

public class Driver {
	private static Robot robot;
	
	private static int DELAY_POINT; //time taken to move cursor, in ms
	private static int DELAY_TYPE;	//time taken to press a key, in ms
	private static int DELAY_CLICK;	//time taken to click a mouse button
	private static int ITER_MAX;	//max iterations a single action should require
	private static int POINT_SIZE;	//collision box width around the cursor
	
	private static Dimension screen;
	public static WritableImage capture;
	public static SimpleObjectProperty<Boolean> captured;
	
	public static void init() throws DriverException {
		ITER_MAX = 50;
		POINT_SIZE = 20;
		
		//AWTPermission robotPermission = new AWTPermission("createRobot", null);
		
		try {
			//AccessController.checkPermission(robotPermission); this does not work, always throws security exception
			
			Platform.runLater(new Runnable() {
				public void run() {
					robot = new javafx.scene.robot.Robot();
				}
			});
			
			DELAY_POINT = 8;
			DELAY_TYPE = 20;
			DELAY_CLICK = 100;
			
			screen = Toolkit.getDefaultToolkit().getScreenSize();
			capture = null;
			captured = new SimpleObjectProperty<>(false);
			
			Logger.log("driver init success");
		}
		catch (SecurityException e) {
			throw new DriverException("do not have permission to control the mouse and keyboard");
		} 
	}
	
	private static void pointfx(int x, int y) {
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
	
	public static void point(int x, int y) {	
		Platform.runLater(new Runnable() {
			public void run() {
				pointfx(x,y);
			}
		});
	}
	
	private static void typefx(String str) {
		char[] chars = str.toLowerCase().toCharArray();
		KeyCode key = KeyCode.UNDEFINED;
		char[] alias = new char[3];
		
		try {
			for (int c=0; c<chars.length; c++) {
				key = KeyCode.getKeyCode(String.valueOf(chars[c]));
				
				if (key == KeyCode.UNDEFINED) {
					Logger.logError("unknown keycode for char " + chars[c]);
				}
				else if ((key.compareTo(KeyCode.DIGIT0) >= 0 && key.compareTo(KeyCode.DIGIT9) <= 0) || (key.compareTo(KeyCode.A) >= 0 && key.compareTo(KeyCode.Z) <= 0)) { 
					//alphanumeric
					robot.keyPress(key);
					robot.keyRelease(key);
				}
				else if ((key == KeyCode.NUMBER_SIGN)) {
					//control chars that cannot be expressed as chars in a string are escaped and given codes. ex: #cmd+del) #shf) #up)
					c++; //skip hashtag
					
					char chr = '#';
					boolean go = true;
					LinkedList<KeyCode> keys = new LinkedList<>();
					
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
							
							Iterator<KeyCode> iterator = keys.iterator();
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
						case SPACE:
						case PERIOD:
						case COMMA:
						case SLASH:
						case SEMICOLON:
						case QUOTE:
						case OPEN_BRACKET:
						case CLOSE_BRACKET:
						case BACK_SLASH:
						case BACK_QUOTE:
						case EQUALS:
						case ENTER:
						case ACCEPT:
						case TAB:
						case MINUS:
							robot.keyPress(key);
							robot.keyRelease(key);
							break;
							
						default:
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
	
	public static void type(String str) {
		Platform.runLater(new Runnable() {
			public void run() {
				typefx(str);
			}
		});
	}
	
	public static void clickLeft() {
		Platform.runLater(new Runnable() {
			public void run() {
				try {
					robot.mousePress(MouseButton.PRIMARY);
					robot.wait(DELAY_CLICK);
				    robot.mouseRelease(MouseButton.PRIMARY);
				    robot.wait(DELAY_CLICK);
				}
				catch (InterruptedException e) {
					//fail quietly
				}
			}
		});
	}
	
	public static void clickRight() {
		Platform.runLater(new Runnable() {
			public void run() {
				try {
					robot.mousePress(MouseButton.SECONDARY);
					robot.wait(DELAY_CLICK);
				    robot.mouseRelease(MouseButton.SECONDARY);
				    robot.wait(DELAY_CLICK);
				}
				catch (InterruptedException e) {
					//fail quietly
				}
			}
		});
	}
	
	public static void captureScreen() {
		captureScreen(new Rectangle(screen.width,screen.height));
	}
	
	public static void captureScreen(Rectangle region) {
		captured.set(false);
		
		Platform.runLater(new Runnable() {
			public void run() {
				try {
					capture = new WritableImage((int)region.getWidth(),(int)region.getHeight());
					robot.getScreenCapture(capture, new Rectangle2D(region.getX(),region.getY(),region.getWidth(),region.getHeight()));
					captured.set(true);
				}
				catch (SecurityException e) {
					Logger.logError("not permitted to view the screen");
				}
			}
		});
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
