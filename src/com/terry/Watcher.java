package com.terry;

import java.util.ArrayList;
import java.util.logging.Level;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseInputListener;

import javafx.scene.input.KeyCode;

public class Watcher {	
	private static NativeKeyListener nativeKeyListener;
	private static NativeMouseInputListener nativeMouseListener;
	
	//keys not accounted for by org.jnativehook.keyboard.NativeKeyEvent
	private static final int VC_SHIFT_R = 3638;
	
	private static ArrayList<KeyCode> keysPressed; 
	
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
		
		//handle keyboard input
		keysPressed = new ArrayList<>();
		nativeKeyListener = new NativeKeyListener() {
			//only works with control keys
			public void nativeKeyPressed(NativeKeyEvent e) {
				KeyCode fxKey = nativeKeyToFxKey(e.getKeyCode());
				
				if (!keysPressed.contains(fxKey)) {
					keysPressed.add(fxKey);
					
					//check scribe key combo
					boolean combo = true;
					for (KeyCode k : Prompter.keyComboScribe) {
						if (!keysPressed.contains(k) && k != KeyCode.UNDEFINED) {
							combo = false;
							break;
						}
					}
					
					if (combo) {
						Terry.triggerScribe();
					}
				}
			}
			
			//only works with control keys
			public void nativeKeyReleased(NativeKeyEvent e) {
				keysPressed.remove(nativeKeyToFxKey(e.getKeyCode()));
			}
			
			public void nativeKeyTyped(NativeKeyEvent e) {
				//do nothing
			}
		};
		GlobalScreen.addNativeKeyListener(nativeKeyListener);
		
		//handle mouse input
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
	
	//adapted from https://github.com/kwhat/jnativehook/pull/209
	private static KeyCode nativeKeyToFxKey(int nativeKey) {
		KeyCode fxKey = KeyCode.UNDEFINED;
		
        switch (nativeKey) {
            case NativeKeyEvent.VC_ESCAPE:
                fxKey = KeyCode.ESCAPE;
                break;
                
            // Begin Function Keys
            case NativeKeyEvent.VC_F1:
                fxKey = KeyCode.F1;
                break;

            case NativeKeyEvent.VC_F2:
                fxKey = KeyCode.F2;
                break;

            case NativeKeyEvent.VC_F3:
                fxKey = KeyCode.F3;
                break;

            case NativeKeyEvent.VC_F4:
                fxKey = KeyCode.F4;
                break;

            case NativeKeyEvent.VC_F5:
                fxKey = KeyCode.F5;
                break;

            case NativeKeyEvent.VC_F6:
                fxKey = KeyCode.F6;
                break;

            case NativeKeyEvent.VC_F7:
                fxKey = KeyCode.F7;
                break;

            case NativeKeyEvent.VC_F8:
                fxKey = KeyCode.F8;
                break;

            case NativeKeyEvent.VC_F9:
                fxKey = KeyCode.F9;
                break;

            case NativeKeyEvent.VC_F10:
                fxKey = KeyCode.F10;
                break;

            case NativeKeyEvent.VC_F11:
                fxKey = KeyCode.F11;
                break;

            case NativeKeyEvent.VC_F12:
                fxKey = KeyCode.F12;
                break;

            case NativeKeyEvent.VC_F13:
                fxKey = KeyCode.F13;
                break;

            case NativeKeyEvent.VC_F14:
                fxKey = KeyCode.F14;
                break;

            case NativeKeyEvent.VC_F15:
                fxKey = KeyCode.F15;
                break;

            case NativeKeyEvent.VC_F16:
                fxKey = KeyCode.F16;
                break;

            case NativeKeyEvent.VC_F17:
                fxKey = KeyCode.F17;
                break;

            case NativeKeyEvent.VC_F18:
                fxKey = KeyCode.F18;
                break;

            case NativeKeyEvent.VC_F19:
                fxKey = KeyCode.F19;
                break;
            case NativeKeyEvent.VC_F20:
                fxKey = KeyCode.F20;
                break;

            case NativeKeyEvent.VC_F21:
                fxKey = KeyCode.F21;
                break;

            case NativeKeyEvent.VC_F22:
                fxKey = KeyCode.F22;
                break;

            case NativeKeyEvent.VC_F23:
                fxKey = KeyCode.F23;
                break;

            case NativeKeyEvent.VC_F24:
                fxKey = KeyCode.F24;
                break;
            // End Function Keys


            // Begin Alphanumeric Zone
            case NativeKeyEvent.VC_BACKQUOTE:
                fxKey = KeyCode.BACK_QUOTE;
                break;

            case NativeKeyEvent.VC_1:
                fxKey = KeyCode.DIGIT1;
                break;

            case NativeKeyEvent.VC_2:
                fxKey = KeyCode.DIGIT2;
                break;

            case NativeKeyEvent.VC_3:
                fxKey = KeyCode.DIGIT3;
                break;

            case NativeKeyEvent.VC_4:
                fxKey = KeyCode.DIGIT4;
                break;

            case NativeKeyEvent.VC_5:
                fxKey = KeyCode.DIGIT5;
                break;

            case NativeKeyEvent.VC_6:
                fxKey = KeyCode.DIGIT6;
                break;

            case NativeKeyEvent.VC_7:
                fxKey = KeyCode.DIGIT7;
                break;

            case NativeKeyEvent.VC_8:
                fxKey = KeyCode.DIGIT8;
                break;

            case NativeKeyEvent.VC_9:
                fxKey = KeyCode.DIGIT9;
                break;

            case NativeKeyEvent.VC_0:
                fxKey = KeyCode.DIGIT0;
                break;


            case NativeKeyEvent.VC_MINUS:
                fxKey = KeyCode.MINUS;
                break;

            case NativeKeyEvent.VC_EQUALS:
                fxKey = KeyCode.EQUALS;
                break;

            case NativeKeyEvent.VC_BACKSPACE:
                fxKey = KeyCode.BACK_SPACE;
                break;


            case NativeKeyEvent.VC_TAB:
                fxKey = KeyCode.TAB;
                break;

            case NativeKeyEvent.VC_CAPS_LOCK:
                fxKey = KeyCode.CAPS;
                break;


            case NativeKeyEvent.VC_A:
                fxKey = KeyCode.A;
                break;

            case NativeKeyEvent.VC_B:
                fxKey = KeyCode.B;
                break;

            case NativeKeyEvent.VC_C:
                fxKey = KeyCode.C;
                break;

            case NativeKeyEvent.VC_D:
                fxKey = KeyCode.D;
                break;

            case NativeKeyEvent.VC_E:
                fxKey = KeyCode.E;
                break;

            case NativeKeyEvent.VC_F:
                fxKey = KeyCode.F;
                break;

            case NativeKeyEvent.VC_G:
                fxKey = KeyCode.G;
                break;

            case NativeKeyEvent.VC_H:
                fxKey = KeyCode.H;
                break;

            case NativeKeyEvent.VC_I:
                fxKey = KeyCode.I;
                break;

            case NativeKeyEvent.VC_J:
                fxKey = KeyCode.J;
                break;

            case NativeKeyEvent.VC_K:
                fxKey = KeyCode.K;
                break;

            case NativeKeyEvent.VC_L:
                fxKey = KeyCode.L;
                break;

            case NativeKeyEvent.VC_M:
                fxKey = KeyCode.M;
                break;

            case NativeKeyEvent.VC_N:
                fxKey = KeyCode.N;
                break;

            case NativeKeyEvent.VC_O:
                fxKey = KeyCode.O;
                break;

            case NativeKeyEvent.VC_P:
                fxKey = KeyCode.P;
                break;

            case NativeKeyEvent.VC_Q:
                fxKey = KeyCode.Q;
                break;

            case NativeKeyEvent.VC_R:
                fxKey = KeyCode.R;
                break;

            case NativeKeyEvent.VC_S:
                fxKey = KeyCode.S;
                break;

            case NativeKeyEvent.VC_T:
                fxKey = KeyCode.T;
                break;

            case NativeKeyEvent.VC_U:
                fxKey = KeyCode.U;
                break;

            case NativeKeyEvent.VC_V:
                fxKey = KeyCode.V;
                break;

            case NativeKeyEvent.VC_W:
                fxKey = KeyCode.W;
                break;

            case NativeKeyEvent.VC_X:
                fxKey = KeyCode.X;
                break;

            case NativeKeyEvent.VC_Y:
                fxKey = KeyCode.Y;
                break;

            case NativeKeyEvent.VC_Z:
                fxKey = KeyCode.Z;
                break;


            case NativeKeyEvent.VC_OPEN_BRACKET:
                fxKey = KeyCode.OPEN_BRACKET;
                break;

            case NativeKeyEvent.VC_CLOSE_BRACKET:
                fxKey = KeyCode.CLOSE_BRACKET;
                break;

            case NativeKeyEvent.VC_BACK_SLASH:
                fxKey = KeyCode.BACK_SLASH;
                break;


            case NativeKeyEvent.VC_SEMICOLON:
                fxKey = KeyCode.SEMICOLON;
                break;

            case NativeKeyEvent.VC_QUOTE:
                fxKey = KeyCode.QUOTE;
                break;

            case NativeKeyEvent.VC_ENTER:
                fxKey = KeyCode.ENTER;
                break;


            case NativeKeyEvent.VC_COMMA:
                fxKey = KeyCode.COMMA;
                break;

            case NativeKeyEvent.VC_PERIOD:
                fxKey = KeyCode.PERIOD;
                break;

            case NativeKeyEvent.VC_SLASH:
                fxKey = KeyCode.SLASH;
                break;

            case NativeKeyEvent.VC_SPACE:
                fxKey = KeyCode.SPACE;
                break;
            // End Alphanumeric Zone


            case NativeKeyEvent.VC_PRINTSCREEN:
                fxKey = KeyCode.PRINTSCREEN;
                break;

            case NativeKeyEvent.VC_SCROLL_LOCK:
                fxKey = KeyCode.SCROLL_LOCK;
                break;

            case NativeKeyEvent.VC_PAUSE:
                fxKey = KeyCode.PAUSE;
                break;


            // Begin Edit Key Zone
            case NativeKeyEvent.VC_INSERT:
                fxKey = KeyCode.INSERT;
                break;

            case NativeKeyEvent.VC_DELETE:
                fxKey = KeyCode.DELETE;
                break;

            case NativeKeyEvent.VC_HOME:
                fxKey = KeyCode.HOME;
                break;

            case NativeKeyEvent.VC_END:
                fxKey = KeyCode.END;
                break;

            case NativeKeyEvent.VC_PAGE_UP:
                fxKey = KeyCode.PAGE_UP;
                break;

            case NativeKeyEvent.VC_PAGE_DOWN:
                fxKey = KeyCode.PAGE_DOWN;
                break;
            // End Edit Key Zone


            // Begin Cursor Key Zone
            case NativeKeyEvent.VC_UP:
                fxKey = KeyCode.UP;
                break;
            case NativeKeyEvent.VC_LEFT:
                fxKey = KeyCode.LEFT;
                break;
            case NativeKeyEvent.VC_CLEAR:
                fxKey = KeyCode.CLEAR;
                break;
            case NativeKeyEvent.VC_RIGHT:
                fxKey = KeyCode.RIGHT;
                break;
            case NativeKeyEvent.VC_DOWN:
                fxKey = KeyCode.DOWN;
                break;
            // End Cursor Key Zone


            // Begin Numeric Zone
            case NativeKeyEvent.VC_NUM_LOCK:
                fxKey = KeyCode.NUM_LOCK;
                break;

            case NativeKeyEvent.VC_SEPARATOR:
                fxKey = KeyCode.SEPARATOR;
                break;
            // End Numeric Zone


            // Begin Modifier and Control Keys
            case NativeKeyEvent.VC_SHIFT:
            case VC_SHIFT_R:
                fxKey = KeyCode.SHIFT;
                break;

            case NativeKeyEvent.VC_CONTROL:
                fxKey = KeyCode.CONTROL;
                break;

            case NativeKeyEvent.VC_ALT:
                fxKey = KeyCode.ALT;
                break;

            case NativeKeyEvent.VC_META:
                fxKey = KeyCode.META;
                break;

            case NativeKeyEvent.VC_CONTEXT_MENU:
                fxKey = KeyCode.CONTEXT_MENU;
                break;
            // End Modifier and Control Keys


			/* Begin Media Control Keys
			case NativeKeyEvent.VC_POWER:
			case NativeKeyEvent.VC_SLEEP:
			case NativeKeyEvent.VC_WAKE:
			case NativeKeyEvent.VC_MEDIA_PLAY:
			case NativeKeyEvent.VC_MEDIA_STOP:
			case NativeKeyEvent.VC_MEDIA_PREVIOUS:
			case NativeKeyEvent.VC_MEDIA_NEXT:
			case NativeKeyEvent.VC_MEDIA_SELECT:
			case NativeKeyEvent.VC_MEDIA_EJECT:
			case NativeKeyEvent.VC_VOLUME_MUTE:
			case NativeKeyEvent.VC_VOLUME_UP:
			case NativeKeyEvent.VC_VOLUME_DOWN:
			case NativeKeyEvent.VC_APP_MAIL:
			case NativeKeyEvent.VC_APP_CALCULATOR:
			case NativeKeyEvent.VC_APP_MUSIC:
			case NativeKeyEvent.VC_APP_PICTURES:
			case NativeKeyEvent.VC_BROWSER_SEARCH:
			case NativeKeyEvent.VC_BROWSER_HOME:
			case NativeKeyEvent.VC_BROWSER_BACK:
			case NativeKeyEvent.VC_BROWSER_FORWARD:
			case NativeKeyEvent.VC_BROWSER_STOP:
			case NativeKeyEvent.VC_BROWSER_REFRESH:
			case NativeKeyEvent.VC_BROWSER_FAVORITES:
			// End Media Control Keys */


            // Begin Japanese Language Keys
            case NativeKeyEvent.VC_KATAKANA:
                fxKey = KeyCode.KATAKANA;
                break;

            case NativeKeyEvent.VC_UNDERSCORE:
                fxKey = KeyCode.UNDERSCORE;
                break;

            //case VC_FURIGANA:

            case NativeKeyEvent.VC_KANJI:
                fxKey = KeyCode.KANJI;
                break;

            case NativeKeyEvent.VC_HIRAGANA:
                fxKey = KeyCode.HIRAGANA;
                break;

            //case VC_YEN:
            // End Japanese Language Keys


            // Begin Sun keyboards
            case NativeKeyEvent.VC_SUN_HELP:
                fxKey = KeyCode.HELP;
                break;

            case NativeKeyEvent.VC_SUN_STOP:
                fxKey = KeyCode.STOP;
                break;

            //case VC_SUN_FRONT:

            //case VC_SUN_OPEN:

            case NativeKeyEvent.VC_SUN_PROPS:
                fxKey = KeyCode.PROPS;
                break;

            case NativeKeyEvent.VC_SUN_FIND:
                fxKey = KeyCode.FIND;
                break;

            case NativeKeyEvent.VC_SUN_AGAIN:
                fxKey = KeyCode.AGAIN;
                break;

            //case NativeKeyEvent.VC_SUN_INSERT:

            case NativeKeyEvent.VC_SUN_COPY:
                fxKey = KeyCode.COPY;
                break;

            case NativeKeyEvent.VC_SUN_CUT:
                fxKey = KeyCode.CUT;
                break;
            // End Sun keyboards
        }
        
        return fxKey;
	}
	
	public static class WatcherException extends Exception {
		private static final long serialVersionUID = -7558575917442213184L;
		
		public WatcherException(String message) {
			super(message);
		}
	}
}
