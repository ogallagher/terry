package com.terry;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import org.jnativehook.mouse.NativeMouseEvent;
import org.jnativehook.mouse.NativeMouseListener;
import org.jnativehook.mouse.NativeMouseMotionListener;

import com.terry.LanguageMapping.LanguageMappingException;
import com.terry.State.Execution;
import com.terry.State.StateException;
import com.terry.Utilities.KeyComboException;
import com.terry.Widget.WidgetException;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;

public class Watcher {	
	private static NativeKeyListener nativeKeyListener;
	private static NativeMouseListener nativeMouseListener;
	private static NativeMouseMotionListener nativeMouseMotionListener;
	
	//keys not accounted for by org.jnativehook.keyboard.NativeKeyEvent
	private static final int VC_SHIFT_R = 3638;
	private static final int VC_META_R = 3676;
	
	private static ArrayList<KeyCode> keysPressed; 
	
	private static boolean enabled = false;
	private static boolean recording = false;
	
	public static final char STATE_DISABLED = 'd';
	public static final char STATE_ENABLED = 'e';
	public static final char STATE_RECORDING = 'r';
	public static final char STATE_PAUSED = 'p';
	public static final char STATE_DONE = 'z';
	public static final char STATE_FAILED = 'f';
	
	public static CharProperty state = new CharProperty(STATE_DISABLED);
	
	public static WatcherRecording demonstration = null;
	public static Action demonstratedAction = null;
	
	public static void init() throws WatcherException {
		//disable jnativehook logging
		java.util.logging.Logger logger = java.util.logging.Logger.getLogger(GlobalScreen.class.getPackage().getName());
		logger.setLevel(Level.WARNING);
		logger.setUseParentHandlers(false);
		
		//handle keyboard input
		keysPressed = new ArrayList<>();
		nativeKeyListener = new NativeKeyListener() {
			public void nativeKeyPressed(NativeKeyEvent e) {
				Logger.log(e.paramString());
				KeyCode fxKey = nativeKeyToFxKey(e.getKeyCode());
				
				if (!keysPressed.contains(fxKey)) {
					keysPressed.add(fxKey);
					
					if (recording) {
						//update demonstration
						demonstration.pressKey(e.getWhen(), fxKey);
					}
					
					//check scribe key combo
					if (keyCombo(Terry.keyComboScribe)) {
						Terry.triggerScribe(true);
					}
					
					//check scribe-done key combo
					if (keyCombo(Terry.keyComboScribeDone)) {
						Terry.triggerScribe(false);
					}
					
					//check abort key combo
					if (keyCombo(Prompter.keyComboAbort)) {
						Prompter.abort();
					}
					
					//check demonstration-done key combo
					if (keyCombo(Terry.keyComboDemonstrationDone)) {
						pause();
					}
				}
			}
			
			public void nativeKeyReleased(NativeKeyEvent e) {
				KeyCode fxKey = nativeKeyToFxKey(e.getKeyCode());
				
				keysPressed.remove(fxKey);
				
				if (recording) {
					//update demonstration
					demonstration.releaseKey(e.getWhen(), fxKey);
				}
			}
			
			public void nativeKeyTyped(NativeKeyEvent e) {
				//do nothing
			}
		};
		
		//handle mouse input
		nativeMouseListener = new NativeMouseListener() {
			private void handleMouseRelease(NativeMouseEvent e) {
				if (recording) {
					//update demonstration
					MouseButton button = MouseButton.NONE;
					
					switch (e.getButton()) {
						case NativeMouseEvent.BUTTON1:
							button = MouseButton.PRIMARY;
							break;
							
						case NativeMouseEvent.BUTTON2:
							button = MouseButton.SECONDARY;
							break;
							
						case NativeMouseEvent.BUTTON3:
							button = MouseButton.MIDDLE;
							break;
							
						default:
							button = MouseButton.NONE;
					}
					
					demonstration.releaseMouse(e.getWhen(), e.getX(), e.getY(), button);
				}
			}
			
			public void nativeMouseClicked(NativeMouseEvent e) {
				handleMouseRelease(e);
			}
			
			public void nativeMousePressed(NativeMouseEvent e) {
				if (recording) {
					//update demonstration
					MouseButton button = MouseButton.NONE;
					
					switch (e.getButton()) {
						case NativeMouseEvent.BUTTON1:
							button = MouseButton.PRIMARY;
							break;
							
						case NativeMouseEvent.BUTTON2:
							button = MouseButton.SECONDARY;
							break;
							
						case NativeMouseEvent.BUTTON3:
							button = MouseButton.MIDDLE;
							break;
							
						default:
							button = MouseButton.NONE;
					}
					
					demonstration.pressMouse(e.getWhen(), e.getX(), e.getY(), button);
				}
			}

			public void nativeMouseReleased(NativeMouseEvent e) {
				handleMouseRelease(e);
			}
		};
		
		nativeMouseMotionListener = new NativeMouseMotionListener() {
			public void nativeMouseMoved(NativeMouseEvent e) {
				if (recording) {
					//update demonstration					
					demonstration.moveMouse(System.currentTimeMillis(), e.getX(), e.getY());
				}
			}
			
			public void nativeMouseDragged(NativeMouseEvent e) {
				if (recording) {
					//update demonstration
					demonstration.dragMouse(System.currentTimeMillis(), e.getX(), e.getY());
				}
			}
		};
		
		enable();
		
		Logger.log("watcher init success");
	}
	
	public static void enable() throws WatcherException {
		Logger.log("enabling watcher");
		
		if (!enabled && !recording) {
			try {
				//register native hook for terry
				GlobalScreen.registerNativeHook();
				
				//add native key listener
				GlobalScreen.addNativeKeyListener(nativeKeyListener);
				
				//add native mouse listeners
				GlobalScreen.addNativeMouseListener(nativeMouseListener);
				GlobalScreen.addNativeMouseMotionListener(nativeMouseMotionListener);
				
				enabled = true;
				state.set(STATE_ENABLED);
			} 
			catch (NativeHookException e) {
				throw new WatcherException("terry failed to register native hooks for mouse and keyboard events");
			} 
		}
	}
	
	public static void disable() throws WatcherException {
		Logger.log("disabling watcher");
		
		if (recording) {
			pause();
		}
		
		if (enabled) {
			try {
				//remove native input listeners
				GlobalScreen.removeNativeKeyListener(nativeKeyListener);
				
				//unregister native hook
				GlobalScreen.unregisterNativeHook();
				
				enabled = false;
				state.set(STATE_DISABLED);
			} 
			catch (NativeHookException e) {
				throw new WatcherException("unable to unregister native hooks for watcher");
			}
		}
	}
	
	public static void record() throws WatcherException {
		if (!recording) {
			if (!enabled) {
				enable();
			}
			
			demonstration = new WatcherRecording();
			demonstratedAction = null;
			recording = true;
			state.set(STATE_RECORDING);
		}
	}
	
	public static void pause() {
		if (recording) {
			recording = false;
			state.set(STATE_PAUSED);
		}
	}
	
	public static void compile(String actionName) {
		if (demonstration != null) {
			new Thread() {
				public void run() {
					try {
						SimpleObjectProperty<Action> actionProperty = new SimpleObjectProperty<>(Terry.dummyAction);
						actionProperty.addListener(new ChangeListener<Action>() {
							public void changed(ObservableValue<? extends Action> observable, Action oldValue, Action newValue) {
								boolean removeme = false;
								
								if (newValue == null) {
									removeme = true;
									state.set(STATE_FAILED);
								}
								else if (newValue != Terry.dummyAction) {
									removeme = true;
									demonstratedAction = newValue;
									state.set(STATE_DONE);
								}
								
								if (removeme) {
									actionProperty.removeListener(this);
								}
							}
						});
						
						demonstration.compile(actionName, actionProperty);
					}
					catch (LanguageMappingException e) {
						demonstratedAction = null;
						Logger.logError(e.getMessage(), Logger.LEVEL_CONSOLE);
						state.set(STATE_FAILED);
					}
				}
			}.start();
		}
	}
	
	private static boolean keyCombo(KeyCode[] combo) {
		for (KeyCode k : combo) {
			if (!keysPressed.contains(k) && k != KeyCode.UNDEFINED) {
				return false;
			}
		}
		
		return true;
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
            case VC_META_R:
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
	
	/*
	 * A sequential log of keyboard and mouse manipulations from a start time to an end time,
	 * along with screen data for determining selected widgets.
	 */
	public static class WatcherRecording {
		LinkedList<Peripheral> peripherals;
		
		public static final char STATE_RECORDING = Watcher.STATE_RECORDING;
		public static final char STATE_COMPILED = 'c';
		
		public char state;
		
		private State<String> typedState; //convenience references to demonstratable states
		private State<Point2D> movedState;
		private State<Point2D> draggedState;
		private State<Widget> movedWidgetState;
		private State<Widget> draggedWidgetState;
		private State<MouseButton> clickedState;
		
		@SuppressWarnings("unchecked")
		public WatcherRecording() {
			peripherals = new LinkedList<>();
			state = STATE_RECORDING;
			
			typedState = (State<String>) Terry.states.get("typed");
			movedState = (State<Point2D>) Terry.states.get("mouseat");
			draggedState = (State<Point2D>) Terry.states.get("mousedragged");
			movedWidgetState = (State<Widget>) Terry.states.get("mouseatwidget");
			draggedWidgetState = (State<Widget>) Terry.states.get("mousedraggedwidget");
			clickedState = (State<MouseButton>) Terry.states.get("clickbtn");
		}
		
		public void pressKey(long timestamp, KeyCode key) {
			addKey(KeyEvent.KEY_PRESSED, timestamp, key);
		}
		
		public void releaseKey(long timestamp, KeyCode key) {
			addKey(KeyEvent.KEY_RELEASED, timestamp, key);
		}
		
		private void addKey(EventType<KeyEvent> type, long timestamp, KeyCode key) {
			try {
				Keyboard last = (Keyboard) peripherals.getLast();
				last = last.append(type, timestamp, key);
				
				if (last != null) {
					peripherals.add(last);
				}
			}
			catch (ClassCastException | NoSuchElementException e) {
				peripherals.add(new Keyboard(type, timestamp, key));
			}
		}
		
		public void moveMouse(long timestamp, int x, int y) {
			addMouse(MouseEvent.MOUSE_MOVED, timestamp, x, y, MouseButton.NONE);
		}
		
		public void dragMouse(long timestamp, int x, int y) {
			addMouse(MouseEvent.MOUSE_DRAGGED, timestamp, x, y, MouseButton.NONE);
		}
		
		public void pressMouse(long timestamp, int x, int y, MouseButton button) {
			addMouse(MouseEvent.MOUSE_PRESSED, timestamp, x, y, button);
		}
		
		public void releaseMouse(long timestamp, int x, int y, MouseButton button) {
			addMouse(MouseEvent.MOUSE_RELEASED, timestamp, x, y, button);
		}
		
		private void addMouse(EventType<MouseEvent> type, long timestamp, int x, int y, MouseButton button) {
			try {
				Mouse last = (Mouse) peripherals.getLast();
				last = last.append(type, timestamp, x, y, button);
				
				if (last != null) {
					peripherals.add(last);
				}
			}
			catch (ClassCastException | NoSuchElementException e) {
				Mouse first = new Mouse(type, timestamp, x, y, button);
				peripherals.add(first);
			}
		}
		
		public void compile(String actionName, SimpleObjectProperty<Action> actionProperty) throws LanguageMappingException {
			Action compositeAction = new Action(actionName);
			
			int n = peripherals.size();
			State<?>[] states = new State<?>[n];
			Arg[][] argArrays = new Arg[n][];
			
			/*
			 * s increments each time a new state is needed.
			 * Not all peripherals convert to their own states, and some are ignored
			 */
			int s = 0;
			
			//converted increments each time a new state added.
			SimpleObjectProperty<Integer> progress = new SimpleObjectProperty<Integer>(0);
			Logger.log("compiling demonstrated action " + actionName, Logger.LEVEL_CONSOLE);
			
			LinkedList<KeyCode> keysPressed = new LinkedList<KeyCode>();
			StringBuilder stringTyped = new StringBuilder();
			KeyCode key;
			
			for (Peripheral p : peripherals) {
				if (p instanceof Keyboard) {
					if (p.type == KeyEvent.KEY_PRESSED) {
						key = ((Keyboard)p).key;
						
						if (Utilities.keyIsModifiable(key)) {
							try {
								stringTyped.append(Utilities.charTypedFromKeyCodes(key, keysPressed));
							} 
							catch (KeyComboException e) {
								//if the character typed requires a key combination (ex: shf+a), a key combo exception os thrown with the aliases included. Ex: A --> #shf+a)
								stringTyped.append(e.getMessage());
							}
						}
						else {
							keysPressed.add(key);
						}
					}
					else { //released
						keysPressed.remove(((Keyboard)p).key);
					}
				}
				else { //Mouse
					//convert completed keyboard to state
					if (stringTyped.length() != 0) {
						states[s] = typedState;
						argArrays[s] = new Arg[] {new Arg("str",stringTyped.toString(),null)};
						s++;
						stringTyped = new StringBuilder();
						
						synchronized (progress) {
							progress.set(progress.get() + 1);
						}
					}
					
					Mouse m = (Mouse) p;
					if (m.type == MouseEvent.MOUSE_CLICKED) {
						//convert mouse click
						String direction;
						switch (m.button) {
							case SECONDARY:
								direction = Arg.DIRARG_RIGHT;
								break;
								
							case MIDDLE:
								direction = Arg.DIRARG_MIDDLE;
								break;
								
							case PRIMARY:
							default:
								direction = Arg.DIRARG_LEFT;
								break;
						}
						
						states[s] = clickedState;
						argArrays[s] = new Arg[] { new Arg("btn", direction, null) };
						s++;
						
						synchronized (progress) {
							progress.set(progress.get() + 1);
						}
					}
					else {
						int ms = s; //s will change as other mice are searched in parallel; create local copy
						s++;
						
						//handle widget search result
						m.defined.addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
								if (newValue) {
									Widget w = m.widget;
									
									if (w == null) {
										//convert completed to-location mouse
										if (m.type == MouseEvent.MOUSE_MOVED) {
											states[ms] = movedState;
										}
										else { //drag
											states[ms] = draggedState;
										}
										
										argArrays[ms] = new Arg[] {
														new Arg("x", Float.valueOf(m.dest.x), null),
														new Arg("y", Float.valueOf(m.dest.y), null)
														};
										
										synchronized (progress) {
											progress.set(progress.get() + 1);
										}
									}
									else {
										//convert completed to-widget mouse
										if (m.type == MouseEvent.MOUSE_MOVED) {
											states[ms] = movedWidgetState;
										}
										else { //drag
											states[ms] = draggedWidgetState;
										}
										
										argArrays[ms] = new Arg[] {new Arg("widget", w, null)};
										
										synchronized (progress) {
											progress.set(progress.get() + 1);
										}
									}
									
									m.defined.removeListener(this);
								}
							}
						});
						
						/*
						 * determine whether the target is a location or a widget
						 * for clicks we don't check target; target is derived from location (move/drag)
						 */
						m.findWidget();
					}
				}
			}
			
			//convert last completed keyboard to state
			if (stringTyped.length() != 0) {
				states[s] = typedState;
				argArrays[s] = new Arg[] {new Arg("str",stringTyped.toString(),null)};
				s++;
				stringTyped = new StringBuilder();
				
				synchronized (progress) {
					progress.set(progress.get() + 1);
				}
			}
			
			int finals = s;
			
			//handle when conversion completes
			progress.addListener(new ChangeListener<Integer>() {
				public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
					Logger.log("demonstration compile progress: " + newValue + " / " + finals, Logger.LEVEL_CONSOLE);
					
					if (newValue == finals) {
						State<?>[] finalStates = new State<?>[finals];
						Arg[][] finalArgArrays = new Arg[finals][];
						
						for (int i=0; i<finals; i++) {
							if (states[i] != null) {
								finalStates[i] = states[i];
								finalArgArrays[i] = argArrays[i];
							}
						}
						
						State<Boolean> compositeState = new State<Boolean>(
								actionName.replaceAll("\\s+", "").toLowerCase() + "state", 
								false, new String[] {}, 
								new DemonstratedExecution(finalStates, finalArgArrays));
						compositeAction.addState(compositeState);
						actionProperty.set(compositeAction);
						
						progress.removeListener(this);
					}
				}
			});
			
			//notify listener in case converted reached s before the listener was registered
			synchronized (progress) {
				if (progress.get() == s) {
					progress.set(s-1);
					progress.set(s);
				}
			}
		}
		
		//returns duration of full recording in milliseconds
		public long duration() {
			return peripherals.getLast().start - peripherals.getFirst().start;
		}
		
		@Override
		public String toString() {
			StringBuilder string = new StringBuilder("watcher recording:\n");
			
			for (Peripheral p : peripherals) {
				string.append("\t" + p.toString() + "\n");
			}
			
			return string.toString();
		}
	}
	
	protected abstract static class Peripheral {
		EventType<? extends Event> type;
		long start;		
	}
	
	public static class Keyboard extends Peripheral {
		private KeyCode key;
		
		public Keyboard(EventType<KeyEvent> type, long timestamp, KeyCode key) {
			this.type = type;
			start = timestamp;
			this.key = key;
		}
		
		public Keyboard append(EventType<KeyEvent> type, long timestamp, KeyCode key) {
			if (type != this.type || key != this.key) {
				return new Keyboard((EventType<KeyEvent>) type, timestamp, key);
			}
			else {
				return null;
			}
		}
		
		@Override
		public String toString() {
			String typeStr;
			
			if (type == KeyEvent.KEY_PRESSED) {
				typeStr = "press";
			}
			else {
				typeStr = "release";
			}
			
			return "@" + start + " key " + typeStr + ": " + key.toString();
		}
	}
	
	/*
	 * Moves are a straight line from orig to dest
	 * Drags are a straight line from orig to dest
	 * Clicks are done in place, where orig==dest
	 * Presses are not stored; they either start a click or a drag
	 * Releases are not stored; they either finish a click or a drag
	 */
	public static class Mouse extends Peripheral {
		private Point orig;
		private Point dest;
		private MouseButton button;
		private Widget widget;
		
		public SimpleObjectProperty<Boolean> defined;
		
		public Mouse(EventType<MouseEvent> type, long timestamp, int x, int y, MouseButton button) {
			this.type = type;
			start = timestamp;
			orig = new Point(x, y);
			dest = new Point(x, y);
			this.button = button;
			widget = null;
			defined = new SimpleObjectProperty<>(false);
		}
		
		public Mouse append(EventType<MouseEvent> type, long timestamp, int x, int y, MouseButton button) {
			boolean newMouse = false;
			
			if (type == MouseEvent.MOUSE_MOVED) {
				if (this.type == type) {
					//extend movement
					dest.x = x;
					dest.y = y;
				}
				else {
					newMouse = true;
				}
			}
			else if (type == MouseEvent.MOUSE_PRESSED) {
				if (this.type != type) {
					newMouse = true;
				}
				//else ignore duplicate press
			}
			else if (type == MouseEvent.MOUSE_RELEASED || type == MouseEvent.MOUSE_CLICKED) {
				if (this.type == MouseEvent.MOUSE_PRESSED) {
					//convert press to click
					this.type = MouseEvent.MOUSE_CLICKED;
				}
				//else ignore duplicate release and drag finish
			}
			else if (type == MouseEvent.MOUSE_DRAGGED) {
				if (this.type == type) {
					//extend drag
					dest.x = x;
					dest.y = y;
				}
				else if (this.type == MouseEvent.MOUSE_PRESSED) {
					//convert press to drag
					this.type = MouseEvent.MOUSE_DRAGGED;
				}
				else {
					newMouse = true;
				}
			}
			
			if (newMouse) {
				return new Mouse(type, timestamp, x, y, button);
			}
			else {
				return null;
			}
		}
		
		private void findWidget() {
			Logger.log("finding widget in " + this.toString(), Logger.LEVEL_FILE);
			widget = Terry.dummyWidget; //set to dummy while widget search is unresolved
			
			//take screen capture
			int captureWidth = Widget.WIDGET_SIZE_MAX;
			Rectangle captureZone = new Rectangle(dest.x - captureWidth/2, dest.y - captureWidth/2, captureWidth, captureWidth);
			
			Driver.captured.addListener(new ChangeListener<Boolean>() {
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (newValue) {
						//search screen capture for known widgets
						BufferedImage capture = SwingFXUtils.fromFXImage(Driver.capture, null);
						LinkedList<Widget> widgets = Memory.getWidgets();
						
						SimpleObjectProperty<Widget> bestWidget = new SimpleObjectProperty<>(null);
						SimpleObjectProperty<Double> bestDiff = new SimpleObjectProperty<Double>(Widget.WIDGET_SEARCH_DIFF_MAX);
						SimpleObjectProperty<Integer> searched = new SimpleObjectProperty<>(0);
						
						//set widget to best
						searched.addListener(new ChangeListener<Integer>() {
							public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
								if (searched.get() == widgets.size()) {
									widget = bestWidget.get();
									Logger.log("finished widget search; found " + widget, Logger.LEVEL_FILE);
									defined.set(true);
								}
							}	
						});
						
						Logger.log("looking for widgets near dest", Logger.LEVEL_FILE);
						for (Widget w : widgets) {
							try {
								w.state.addListener(new ChangeListener<Character>() {
									public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
										char c = newValue.charValue();
										boolean removeme = false;
										double diff = w.getDiff();
										
										if (c == Widget.STATE_FOUND && diff < bestDiff.get()) {
											//check that cursor was within widget zone
											Rectangle zone = w.getZone();
											if (zone.contains(dest.x, dest.y)) {
												bestWidget.set(w);
												bestDiff.set(diff);
											}
											
											removeme = true;
										}
										else if (c == Widget.STATE_NOT_FOUND) {
											removeme = true;
										}
										
										if (removeme) {
											synchronized (searched) {
												searched.set(searched.get() + 1);
											}
											
											w.state.removeListener(this);
										}
									}
								});
								
								w.findInScreen(capture, Widget.WIDGET_SEARCH_DIFF_MAX);
							} 
							catch (WidgetException e) {
								Logger.logError("could not search for widget" + w.getName(), Logger.LEVEL_FILE);
							}
						}
						
						//in case searches complete too fast (if there are no widgets, for example), notify again
						synchronized (searched) {
							if (searched.get() == widgets.size()) {
								searched.set(searched.get() - 1);
								searched.set(widgets.size());
							}
						}
						
						Driver.captured.removeListener(this);
					}
				}
			});
			
			Driver.captureScreen(captureZone,null);
		}
		
		@Override
		public String toString() {
			String typeStr;
			
			if (type == MouseEvent.MOUSE_MOVED) {
				typeStr = "move";
			}
			else if (type == MouseEvent.MOUSE_DRAGGED) {
				typeStr = "drag";
			}
			else {
				typeStr = "click";
			}
			
			return "@" + start + " mouse " + typeStr + "(" + orig.x + "," + orig.y + ") -> (" + dest.x + "," + dest.y + ") " + button.toString();
		}
	}
	
	private static class DemonstratedExecution extends Execution<Boolean> {
		private static final long serialVersionUID = 4490776432548709253L;
		
		private State<?>[] subStates;
		private Arg[][] subArgs;
		
		@SuppressWarnings("unused")
		public DemonstratedExecution() {
			subStates = null;
			subArgs = null;
		}
		
		public DemonstratedExecution(State<?>[] stateSequence, Arg[][] argSequence) {
			subStates = stateSequence;
			subArgs = argSequence;
		}
		
		public Boolean execute(Boolean stateOld, Arg[] args) {
			if (subStates != null && subArgs != null) {
				SimpleObjectProperty<Integer> completions = new SimpleObjectProperty<>(0); 
				this.notifier.set(false);
				
				try {
					int n = subStates.length;
					
					for (int i=0; i<n; i++) {
						SimpleObjectProperty<Boolean> transitioned = subStates[i].transition(subArgs[i]);
						
						transitioned.addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
								if (newValue) {
									observable.removeListener(this);
									
									synchronized (completions) {
										int c = completions.get() + 1;
										completions.set(c);
										
										if (c == n) {
											notifier.set(true);
										}
									}
								}
							}
						});
						
						//notify again if completed too fast
						if (transitioned.get()) {
							transitioned.set(false);
							transitioned.set(true);
						}
					}
					
					return true;
				} 
				catch (StateException e) {
					Logger.logError(e.getMessage(), Logger.LEVEL_CONSOLE);
					return false;
				}
			}
			else {
				return false;
			}
		}
		
		//this implementation of DriverExecution now has properties that need to be serialized explicitly
		private void writeObject(ObjectOutputStream stream) throws IOException {
			//save substates
			stream.writeObject(subStates);
			
			//save subargs
			stream.writeObject(subArgs);
		}
		
		private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
			//read substates
			subStates = (State<?>[]) stream.readObject();
			
			//read subargs
			subArgs = (Arg[][]) stream.readObject();
		}
	}
}
