package com.terry;

import java.util.logging.Level;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseInputListener;

public class Watcher {	
	private static NativeKeyListener nativeKeyListener;
	private static NativeMouseInputListener nativeMouseListener;
	
	//keys not accounted for by org.jnativehook.keyboard.NativeKeyEvent
	private static final int VC_SHIFT_R = 3638;
	
	public static void init() throws WatcherException {
		//disable jnativehook logging
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.WARNING);
		logger.setUseParentHandlers(false);
		
		//register native hook for terry
		try {
			GlobalScreen.registerNativeHook();
		} 
		catch (NativeHookException e) {
			throw new WatcherException("terry failed to register native hooks for mouse and keyboard events");
		} 
		
		nativeKeyListener = new NativeKeyListener() {
			//only works with control keys
			public void nativeKeyPressed(NativeKeyEvent e) {
				int key = e.getKeyCode();
				boolean alphanumeric = (key >= NativeKeyEvent.VC_A && key <= NativeKeyEvent.VC_Z) || 
									   (key >= NativeKeyEvent.VC_1 && key <= NativeKeyEvent.VC_0);
				
				String message = "native key " + key + " pressed, name = ";
				String keyString;
				
				switch (key) {
					case NativeKeyEvent.VC_SHIFT:
					case VC_SHIFT_R:
						keyString = "SHIFT";
						break;
						
					case NativeKeyEvent.VC_CAPS_LOCK:
						keyString = "CAPS_LOCK";
						break;
						
					case NativeKeyEvent.VC_CONTROL:
						keyString = "CONTROL";
						break;
						
					case NativeKeyEvent.VC_ALT:
						keyString = "ALT/OPTION";
						break;
						
					case NativeKeyEvent.VC_META:
						keyString = "COMMAND";
						break;
						
					default:
						keyString = "UNKNOWN";
						break;
				}
				
				Logger.log(message + keyString);
			}
			
			//only works with control keys
			public void nativeKeyReleased(NativeKeyEvent e) {
				int key = e.getKeyCode();
				boolean alphanumeric = (key >= NativeKeyEvent.VC_A && key <= NativeKeyEvent.VC_Z) || 
									   (key >= NativeKeyEvent.VC_1 && key <= NativeKeyEvent.VC_0);
				
				//Logger.log("native key " + key + " released, alphanumeric=" + alphanumeric);
			}
			
			public void nativeKeyTyped(NativeKeyEvent e) {
				int keychar = e.getKeyChar();
				Logger.log("native key char " + keychar + " typed");
			}
		};
		GlobalScreen.addNativeKeyListener(nativeKeyListener);
		
		nativeMouseListener = new NativeMouseInputListener() {
			public void nativeMouseClicked(NativeMouseEvent e) {
				
			}

			public void nativeMousePressed(NativeMouseEvent e) {
				
			}

			public void nativeMouseReleased(NativeMouseEvent e) {
				
			}
			
			public void nativeMouseMoved(NativeMouseEvent e) {
				System.out.println("native mouse movement " + e.getX() + " " + e.getY());
				Logger.log("native mouse movement " + e.getX() + " " + e.getY());
			}

			public void nativeMouseDragged(NativeMouseEvent e) {
				
			}
		};
		
		Logger.log("watcher init success");
	}
	
	public static void stop() throws WatcherException {
		Logger.log("stopping watcher");
		GlobalScreen.removeNativeKeyListener(nativeKeyListener);
		try {
			GlobalScreen.unregisterNativeHook();
		} 
		catch (NativeHookException e) {
			throw new WatcherException("unable to unregister native hooks for watcher");
		}
	}
	
	public static class WatcherException extends Exception {
		private static final long serialVersionUID = -7558575917442213184L;
		
		public WatcherException(String message) {
			super(message);
		}
	}
}
