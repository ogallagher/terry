/*
 * Title:		Terry
 * Author:		Owen Gallagher
 * Start Date:	December 2019
 * End Date:	May 2020
 */

package com.terry;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import com.terry.LanguageMapping.LanguageMappingException;
import com.terry.Lesson.Definition;
import com.terry.Memory.MemoryException;
import com.terry.Prompter.PrompterException;
import com.terry.Scribe.ScribeException;
import com.terry.Speaker.SpeakerException;
import com.terry.State.StateException;
import com.terry.Watcher.WatcherException;
import com.terry.Widget.WidgetException;
import com.terry.State.Execution;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;

public class Terry {
	public static Prompter prompter;
	
	public static Widget dummyWidget;
	public static Action dummyAction;
	
	public static HashMap<String,State<?>> states = new HashMap<>(); //global state table
	
	public static final String RES_PATH = "res/";
	
	public static final char OS_MAC = 1;
	public static final char OS_WIN = 2;
	public static final char OS_OTHER = 3;
	public static char os;
		
	public static final int KEY_ALIAS_MAX = 3;
	public static final String KEY_DELETE = "del";
	public static final String KEY_BACKSPACE = "bck";
	public static final String KEY_SHIFT = "shf";
	public static final String KEY_CONTROL = "ctl";
	public static final String KEY_ENTER_RETURN = "ret";
	public static final String KEY_ALT = "alt";
	public static final String KEY_FN1 = "fn1";
	public static final String KEY_CMD = "cmd";
	public static final String KEY_CAPS_LOCK = "cap";
	public static final String KEY_ESCAPE = "esc";
	public static final String KEY_UP = "up_";
	public static final String KEY_RIGHT = "rgt";
	public static final String KEY_DOWN = "dwn";
	public static final String KEY_LEFT = "lft";
	public static final String KEY_HASHTAG = "hsh";
	public static final String KEY_TILDE = "tld";
	public static final String KEY_EXCLAMATION = "exl";
	public static final String KEY_AT = "at_";
	public static final String KEY_DOLLAR = "dol";
	public static final String KEY_PERCENT = "pct";
	public static final String KEY_CARROT = "crt";
	public static final String KEY_AMPERSAND = "amp";
	public static final String KEY_STAR = "str";
	public static final String KEY_LPAREN = "lpr";
	public static final String KEY_RPAREN = "rpr";
	public static final String KEY_UNDERSCORE = "udr";
	public static final String KEY_PLUS = "pls";
	public static final String KEY_LBRACE = "lbr";
	public static final String KEY_RBRACE = "rbr";
	public static final String KEY_PIPE = "pip";
	public static final String KEY_COLON = "col";
	public static final String KEY_DOUBLE_QUOTE = "dqt";
	public static final String KEY_LESS = "lss";
	public static final String KEY_GREATER = "gtr";
	public static final String KEY_QUERY = "qry";
	public static final String KEY_TAB = "tab";
	//uppercase letters: X = x__
	
	public static KeyCode[] keyComboScribe;
	public static KeyCode[] keyComboScribeDone;
	public static KeyCode[] keyComboDemonstrationDone;
	
	public static final int EXITCODE_OS = 1;
	public static final int EXITCODE_WATCHER = 2;
	public static final int EXITCODE_MEMORY = 3;
	public static final int EXITCODE_MAPPINGS = 4;
	
	public static void main(String[] args) {
		String osName = System.getProperty("os.name").toLowerCase();
		String osMessage = "";
		if (osName.startsWith("win")) {
			os = OS_WIN;
			osMessage = "detected win os";
			
			keyComboScribe = new KeyCode[] {KeyCode.CONTROL, KeyCode.ALT, KeyCode.T};
			keyComboDemonstrationDone = new KeyCode[] {KeyCode.CONTROL, KeyCode.ESCAPE};
		}
		else if (osName.startsWith("mac")) {
			os = OS_MAC;
			osMessage = "detected mac os";
			
			keyComboScribe = new KeyCode[] {KeyCode.META, KeyCode.ALT, KeyCode.SHIFT};
			keyComboDemonstrationDone = new KeyCode[] {KeyCode.META, KeyCode.SHIFT}; //TODO change this to escape
		}
		else {
			os = OS_OTHER;
			System.err.println("detected unsupported os: " + osName);
			System.exit(EXITCODE_OS);
		}
		
		keyComboScribeDone = new KeyCode[] {KeyCode.UNDEFINED}; //undefined = any key
		
		Logger.init();
		Logger.log(osMessage, Logger.LEVEL_CONSOLE);
		
		Utilities.init();
		
		try {
			Speaker.init();
		} 
		catch (SpeakerException e) {
			Logger.logError(e.getMessage());
		}
		
		try {
			Widget.init();
		} 
		catch (WidgetException | LanguageMappingException e) {
			Logger.logError("widget init failed. " + e.getMessage());
		}
		
		try {
			Action.init();
		} 
		catch (LanguageMappingException e) {
			Logger.logError("action init failed. " + e.getMessage());
		}
		
		Arg.init();
		
		try {
			Watcher.init();
		}
		catch (WatcherException e) {
			Logger.logError(e.getMessage());
			System.exit(EXITCODE_WATCHER);
		}
		
		try {
			Scribe.init();
		}
		catch (ScribeException e) {
			Logger.logError(e.getMessage());
		}
		
		InstructionParser.init();
		
		Compiler.init();
		
		try {
			Memory.init(); //calls LanguageMapping.init() from maps.txt file
		}
		catch (MemoryException e) {
			e.printStackTrace();
			System.exit(EXITCODE_MEMORY);
		}
		
		if (LanguageMapping.empty()) {
			try {
				createPrimitiveActions();
				createLessons();
			}
			catch (LanguageMappingException e) {
				e.printStackTrace();
				System.exit(EXITCODE_MAPPINGS);
			}
		}
		
		Logger.log(Memory.printDictionary(), Logger.LEVEL_FILE);
		
		prompter = new Prompter();
		prompter.init(args); //calls Driver.init() since jfx robot needs jfx toolkit initialization
	}
	
	public static ArrayList<String> printState() {
		ArrayList<String> list = new ArrayList<String>();
		
		for (Entry<String, State<?>> entry : states.entrySet()) {
			list.add(entry.getValue().toString());
		}
		
		return list;
	}
	
	//if activate, start if idle. Otherwise, switch scribe states
	public static void triggerScribe(boolean activate) {
		try {
			switch (Scribe.state.get()) {
				case Scribe.STATE_IDLE:
					if (activate) {
						Scribe.start();
					}
					break;
					
				case Scribe.STATE_RECORDING:
					Scribe.stop();
					break;
					
				case Scribe.STATE_TRANSCRIBING:
				case Scribe.STATE_DONE:
					Logger.log("waiting for scribe to finish transcription...");
					break;
					
				default:
					Logger.logError("scribe in unknown state");
					break;
			}
		} 
		catch (ScribeException e) {
			Logger.logError(e.getMessage());
		}
	}
	
	private static void createPrimitiveActions() throws LanguageMappingException {
		Logger.log("no mappings found; creating primitive actions");
		
		//--- move mouse to screen location ---//
		Action mouseToXY = new Action("?|move,go,)) ?|mouse,cursor,pointer,)) to ?|location,position,coordinates,)) ?x) @#x |x,comma,y,) @#y ?y)");
		
		State<Point2D> mouseat = new State<Point2D>("mouseat", new Point2D.Float(), new String[] {"x","y"}, new Execution<Point2D>() {
			private static final long serialVersionUID = -5509580894164954809L;
			
			public Point2D execute(Point2D stateOld, Arg[] args) {
				Float x = Float.valueOf(0);
				Float y = Float.valueOf(0);
				
				//map args
				for (Arg arg : args) {
					Object value = arg.getValue();
					
					if (value == null) {
						Logger.log("null arg");
					}
					else if (arg.name.equals("x")) {
						x = (Float) value;
					}
					else if (arg.name.equals("y")) {
						y = (Float) value;
					}
				}
				
				//direct driver
				Driver.point(x.intValue(), y.intValue(), notifier);
				
				//update state
				return new Point2D.Float(x, y);
			}
		});
		mouseToXY.addState(mouseat);
		
		Memory.addMapping(mouseToXY);
		
		//--- click mouse button ---//
		Action mouseClickBtn = new Action("?@dbtn) click");
		
		State<MouseButton> clickbtn = new State<MouseButton>("clickbtn", MouseButton.NONE, new String[] {"btn"}, new Execution<MouseButton>() {
			private static final long serialVersionUID = -3163938142402546869L;

			public MouseButton execute(MouseButton stateOld, Arg[] args) {
		        MouseButton button = MouseButton.PRIMARY;
		        
		        //map args
		        for (Arg arg : args) {
		        	Object value = arg.getValue();
		        	
		        	if (value == null) {
						Logger.log("null arg");
					}
		        	else if (arg.name.equals("btn")) {
		        		String direction = (String) value;
		        		
		        		if (direction.equals(Arg.DIRARG_RIGHT)) {
		        			button = MouseButton.SECONDARY;
		        		}
		        		else if (direction.equals(Arg.DIRARG_MIDDLE)) {
		        			button = MouseButton.MIDDLE;
		        		}
		        		else if (direction.equals(Arg.DIRARG_DOUBLE)) {
		        			button = MouseButton.NONE; //double click
		        		}
		        		//else, assume button 1
		        	}
		        }
		        
		        //direct driver
		        if (button == MouseButton.PRIMARY) {
		            Driver.clickLeft(notifier);
		        }
		        else if (button == MouseButton.SECONDARY) {
		            Driver.clickRight(notifier);
		        }
		        else if (button == MouseButton.MIDDLE) {
		        	Driver.clickMiddle(notifier);
		        }
		        else if (button == MouseButton.NONE) {
		        	Driver.clickDouble(notifier);
		        }
		        
		        //update state
		        return button;
		    }
		});
		mouseClickBtn.addState(clickbtn);
		
		Memory.addMapping(mouseClickBtn);
		
		//--- drag mouse to screen location ---//
		Action mouseDragXY = new Action("?drag) ?|mouse,cursor,pointer,)) to ?|location,position,coordinates,)) ?x) @#x |x,comma,y,) @#y ?y)");
		
		State<Point2D> mousedragged = new State<>("mousedragged", new Point2D.Float(), new String[] {"x","y"}, new Execution<Point2D>() {
			private static final long serialVersionUID = 8098973136515206171L;

			public Point2D execute(Point2D stateOld, Arg[] args) {
				Float x = Float.valueOf(0);
				Float y = Float.valueOf(0);
				
				//map args
				for (Arg arg : args) {
					Object value = arg.getValue();
					
					if (value == null) {
						Logger.log("null arg");
					}
					else if (arg.name.equals("x")) {
						x = (Float) value;
					}
					else if (arg.name.equals("y")) {
						y = (Float) value;
					}
				}
				
				//direct driver
				Driver.drag(x.intValue(), y.intValue(), notifier);
				
				//update state(s)
				Point2D.Float dest = new Point2D.Float(x, y);
				mouseat.getProperty().set(dest);
				return dest;
			}
		});
		mouseDragXY.addState(mousedragged);
		
		Memory.addMapping(mouseDragXY);
		
		//--- type string ---//
		Action typeStr = new Action("type ?out) ?following string) @$str ?end quote)");
		
		State<String> typed = new State<String>("typed", "", new String[] {"str"}, new Execution<String>() {
			private static final long serialVersionUID = 6266250470624001432L;

			public String execute(String stateOld, Arg[] args) {
				String string = "";
				
				//map args
				for (Arg arg : args) {
					if (arg.name.equals("str")) {
						string = (String) arg.getValue();
					}
				}
				
				//direct driver
				Driver.type(string, notifier);
				
				//update state
				return string;
			}
		});
		typeStr.addState(typed);
		
		Memory.addMapping(typeStr);
		
		//--- shutdown ---//
		Action shutdown = new Action("|[shut_down],[turn_off],quit,)");
		
		State<Boolean> quitted = new State<Boolean>("quitted", false, new String[] {}, new Execution<Boolean>() {
			private static final long serialVersionUID = -7508747835691544792L;

			public Boolean execute(Boolean stateOld, Arg[] args) {
				//no args
				//direct prompter
				
				try {
					prompter.stop();
				} 
				catch (Exception e) {
					Logger.logError("shutdown failed");
					Logger.logError(e.getMessage());
				}
				
				return true;
			}
		});
		shutdown.addState(quitted);
		
		Memory.addMapping(shutdown);
		
		//--- show state ---//
		Action showState = new Action("|show,[what_is],log,) ?current) state");
		
		State<Boolean> stateshown = new State<Boolean>("stateshown", true, new String[] {}, new Execution<Boolean>() {
			private static final long serialVersionUID = -4961265132685762301L;

			public Boolean execute(Boolean stateOld, Arg[] args) {
				//no args
				//direct controller
				Logger.log("states: ");
				for (String state : Terry.printState()) {
					Logger.log(state);
				}
				
				return true;
			}
		});
		showState.addState(stateshown);
		
		Memory.addMapping(showState);
		
		//--- capture screen/take screenshot ---//
		Action captureScreen = new Action("?|create,take,)) |screenshot,[screen_shot],[screen_capture],[capture_screen],)");
		
		//set captureupdated to false, which statecaptured will then set to true when the screen capture is obtained
		State<Boolean> statecaptureupdated = new State<Boolean>("statecaptureupdated", Boolean.FALSE, new String[] {}, new Execution<Boolean>() {
			private static final long serialVersionUID = -5850373248159506280L;
			
			public Boolean execute(Boolean stateOld, Arg[] args) {
				return false;
			}
		});
		captureScreen.addState(statecaptureupdated);
		
		State<BufferedImage> statecaptured = new State<BufferedImage>("statecaptured", new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), new String[] {}, new Execution<BufferedImage>() {
			private static final long serialVersionUID = 4607129019674379163L;

			public BufferedImage execute(BufferedImage stateOld, Arg[] args) {
				//no args
				//direct driver and prompter
				Prompter.hide(null);
				try {Thread.sleep(500);} catch (InterruptedException e) {} //wait for hide to complete
				
				Dimension screen = Driver.getScreen();
				BufferedImage capture = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_RGB);
				
				//capture screen
				Driver.captured.addListener(new ChangeListener<Boolean>() {
					public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
						if (newValue) {
							//update statecaptureupdated when the capture object contains the data
							SimpleObjectProperty<Boolean> captureUpdated = statecaptureupdated.getProperty();
							
							SwingFXUtils.fromFXImage(Driver.capture, capture);
							captureUpdated.set(true);
							
							if (capture != null) {
								Utilities.saveImage(capture, Terry.RES_PATH + "vision/", "screen_capture.png");
							}
							
							Prompter.show(notifier);
							Driver.captured.removeListener(this);
						}
					}
				});
				Driver.captureScreen(notifier);
				
				return capture;
			}
		});
		captureScreen.addState(statecaptured);
		
		Memory.addMapping(captureScreen);
		
		//--- find widget location in screen ---//
		Action locateWidget = new Action("|find,locate,show,) ?where) ?is) @wwidget ?is)");
		
		State<Boolean> widgetlocationupdated = new State<Boolean>("widgetlocationupdated", Boolean.FALSE, new String[] {}, new Execution<Boolean>() {
			private static final long serialVersionUID = 1721914958113344877L;
			
			public Boolean execute(Boolean stateOld, Arg[] args) {
				return false;
			}
		});
		locateWidget.addState(widgetlocationupdated);
		
		State<Point2D> widgetlocation = new State<Point2D>("widgetlocation", new Point2D.Double(), new String[] {"widget"}, new Execution<Point2D>() {
			private static final long serialVersionUID = 3281519688836173335L;

			public Point2D execute(Point2D stateOld, Arg[] args) {
				//map args
				Widget widget = null;
				
				for (Arg arg : args) {	
					if (arg != null && arg.name.equals("widget")) {
						widget = (Widget) arg.getValue();
					}
				}
				
				Point2D location = new Point2D.Double(-1,-1);
				
				//direct various modules
				if (widget != null) {										
					//direct driver to capture screen
					Logger.log("preparing to locate widget " + widget.getName());
					final Widget finalWidget = widget;
					
					HashMap<String,Arg> captureScreenArgs = new HashMap<>();
					try {
						//listen for when screen capture is complete
						SimpleObjectProperty<Boolean> captureUpdated = statecaptureupdated.getProperty();
						captureUpdated.set(false);
						captureUpdated.addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
								if (newValue.booleanValue()) {
									//get capture and search
									BufferedImage screenshot;
									try {
										screenshot = statecaptured.getValue();
										
										//handle result of widget search
										finalWidget.state.set(Widget.STATE_IDLE);
										finalWidget.state.addListener(new ChangeListener<Character>() {
											public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
												boolean removeme = false;
												
												if (newValue == Widget.STATE_FOUND) {
													Rectangle zone = finalWidget.getZone();
													
													Logger.log("widget found at " + zone.getX() + " " + zone.getY() + " " + zone.getWidth() + " " + zone.getHeight());
													
													/*
													//direct prompter to highlight found widget
													Prompter.clearOverlay(null);
													Prompter.showOverlay(null);
													Prompter.colorOverlay(new Color(0.8,0.1,0.6,0.2), Color.MEDIUMVIOLETRED);
													Prompter.drawOverlay(zone.getPathIterator(null), true, true, null);
													
													//hide overlay after waiting
													Timer fader = new Timer();
													fader.schedule(new TimerTask() {
														public void run() {
															Logger.log("fading overlay", Logger.LEVEL_FILE);
															Prompter.fadeOverlay(zone.getPathIterator(null), true, true, null);
														}
													}, Prompter.WIDGET_HIGHLIGHT_TIME);
													*/
													
													//update state(s)
													location.setLocation(zone.getCenterX(), zone.getCenterY());
													widgetlocationupdated.getProperty().set(true);
													removeme = true;
												}
												else if (newValue == Widget.STATE_NOT_FOUND) {
													Logger.log("widget not found");
													removeme = true;
												}
												
												if (removeme) {
													notifier.set(true);
													finalWidget.state.removeListener(this);
												}
											}
										});
										
										//direct widget to find itself
										finalWidget.findInScreen(screenshot);
									}
									catch (WidgetException e) {
										Logger.logError("widget search failed: " + e.getMessage());
									}
									
									captureUpdated.removeListener(this);
								}
							}
						});
						
						//execute screen capture action
						captureScreen.execute(captureScreenArgs);
					} 
					catch (StateException e) {
						Logger.logError(e.getMessage());
					}
				}
				else {
					Logger.logError("widget to find was not given");
				}
				
				return location;
			}
		});
		locateWidget.addState(widgetlocation);
		
		Memory.addMapping(locateWidget);
		
		//--- move mouse to widget ---//
		Action mouseToWidget = new Action("|[?move)_|mouse,cursor,pointer,)],go,) to @wwidget");
		
		State<Widget> mouseatwidget = new State<Widget>("mouseatwidget", dummyWidget, new String[] {"widget"}, new Execution<Widget>() {
			private static final long serialVersionUID = 4934794946741729275L;

			public Widget execute(Widget stateOld, Arg[] args) {
				//map args
				Widget widget = null;
				Arg widgetArg = null;
				
				for (Arg arg : args) {	
					if (arg != null && arg.name.equals("widget")) {
						widget = (Widget) arg.getValue();
						widgetArg = arg;
					}
				}
				
				if (widget != null) {
					//get widget location
					HashMap<String,Arg> locateWidgetArgs = new HashMap<>();
					locateWidgetArgs.put("widget", widgetArg);
					
					try {
						SimpleObjectProperty<Boolean> widgetLocationUpdated = widgetlocationupdated.getProperty();
						widgetLocationUpdated.set(false);
						widgetLocationUpdated.addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {								
								if (newValue) {
									System.out.println("widgetlocationupdated is true");
									//move mouse to widget location
									Point2D location = widgetlocation.getProperty().get();
									int x = (int) location.getX();
									int y = (int) location.getY();
									
									//direct driver
									Driver.point(x, y, notifier);
									
									//update mouse location
									mouseat.getProperty().set(new Point2D.Float(x,y));
									
									widgetLocationUpdated.removeListener(this);
								}
							}
						});
						
						locateWidget.execute(locateWidgetArgs);
						
						//update moused widget
						return widget;
					}
					catch (StateException e) {
						//widget location failure
						return null;
					}					
				}
				else {
					return null;
				}
			}
		});
		mouseToWidget.addState(mouseatwidget);
		
		Memory.addMapping(mouseToWidget);
		
		//--- drag mouse to widget ---//
		Action mouseDragWidget = new Action("drag ?|mouse,cursor,pointer,)) to @wwidget");
		
		State<Widget> mousedraggedwidget = new State<Widget>("mousedraggedwidget", dummyWidget, new String[] {"widget"}, new Execution<Widget>() {
			private static final long serialVersionUID = 6240024334541248565L;

			public Widget execute(Widget stateOld, Arg[] args) {
				//map args
				Widget widget = null;
				Arg widgetArg = null;
				
				for (Arg arg : args) {	
					if (arg != null && arg.name.equals("widget")) {
						widget = (Widget) arg.getValue();
						widgetArg = arg;
					}
				}
				
				if (widget != null) {
					//get widget location
					HashMap<String,Arg> locateWidgetArgs = new HashMap<>();
					locateWidgetArgs.put("widget", widgetArg);
					
					try {
						SimpleObjectProperty<Boolean> widgetLocationUpdated = widgetlocationupdated.getProperty();
						widgetLocationUpdated.set(false);
						widgetLocationUpdated.addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {								
								if (newValue) {
									//move mouse to widget location
									Point2D location = widgetlocation.getProperty().get();
									int x = (int) location.getX();
									int y = (int) location.getY();
									
									//direct driver
									Driver.drag(x, y, notifier);
									
									//update mouse location
									Point2D.Float dest = new Point2D.Float(x,y);
									mouseat.getProperty().set(dest);
									mousedragged.getProperty().set(dest);
									
									widgetLocationUpdated.removeListener(this);
								}
							}
						});
						
						locateWidget.execute(locateWidgetArgs);
						
						//update moused widget
						mouseatwidget.getProperty().set(widget);
						return widget;
					}
					catch (StateException e) {
						//widget location failure
						return null;
					}					
				}
				else {
					return null;
				}
			}
		});
		mouseDragWidget.addState(mousedraggedwidget);
		
		Memory.addMapping(mouseDragWidget);
		
		//--- adjust speaker volume ---//
		Action setSpeakerVolume = new Action("?set) ?|speaker,speech,spoken)) volume to @#level ?@$percent)");
		
		State<Float> speakervolume = new State<Float>("speakervolume", 0.5f, new String[] {"level","percent"}, new Execution<Float>() {
			private static final long serialVersionUID = 2959383880513305366L;
			
			public Float execute(Float stateOld, Arg[] args) {
				//map args
				Float volume = null;
				String percent = null;
				
				for (Arg arg : args) {
					if (arg != null) {
						if (arg.name.equals("level")) {
							volume = (Float) arg.getValue();
						}
						else if (arg.name.equals("percent")) {
							percent = (String) arg.getValue();
						}
					}
				}
				
				//control speaker
				if (volume != null && volume > 0) {
					if (percent != null && percent.equals("percent")) { //percent was said explicitly, 0-100 range
						volume = volume / 100;
					}
					else if (volume >= 1 && volume <= 10) { //assume 0-10 range
						volume = volume / 10;
					}
					//else assume 0-1 range
					
					try {
						Speaker.setVolume(volume);
						Logger.log("now i speak this loud", Logger.LEVEL_SPEECH);
					}
					catch (SpeakerException e) {
						Logger.logError(e.getMessage(), Logger.LEVEL_SPEECH);
						volume = null;
					}
					
					notifier.set(true);
					return volume;
				}
				else {
					//no volume specified
					Logger.logError("volume level " + volume + " not specified or invalid", Logger.LEVEL_CONSOLE);
					notifier.set(true);
					return null;
				}
			}
		});
		setSpeakerVolume.addState(speakervolume);
		
		Memory.addMapping(setSpeakerVolume);
		
		//--- adjust speaker speed ---//
		Action setSpeakerSpeed = new Action("?set) ?|speaker,speech,spoken,)) speed to @#speed ?@$percent)");
		
		State<Float> speakerspeed = new State<Float>("speakerspeed", 0.5f, new String[] {"speed","percent"}, new Execution<Float>() {
			private static final long serialVersionUID = -8572398699178191929L;
			
			public Float execute(Float stateOld, Arg[] args) {
				//map args
				Float speed = null;
				String percent = null;
				
				for (Arg arg : args) {
					if (arg != null) {
						if (arg.name.equals("speed")) {
							speed = (Float) arg.getValue();
						}
						else if (arg.name.equals("percent")) {
							percent = (String) arg.getValue();
						}
					}
				}
				
				//control speaker
				if (speed != null && speed > 0) {
					if (percent != null && percent.equals("percent")) { //percent was said explicitly, 0-100 range
						speed = speed / 100;
					}
					else if (speed >= 1 && speed <= 10) { //assume 0-10 range
						speed = speed / 10;
					}
					//else assume 0-1 range
					
					try {
						Speaker.setSpeed(speed);
						Logger.log("now i speak this fast", Logger.LEVEL_SPEECH);
					}
					catch (SpeakerException e) {
						Logger.logError(e.getMessage(), Logger.LEVEL_SPEECH);
						speed = null;
					}
					
					notifier.set(true);
					return speed;
				}
				else {
					//no volume specified
					Logger.logError("speech speed " + speed + " not specified or invalid", Logger.LEVEL_CONSOLE);
					notifier.set(true);
					return null;
				}
			}
		});
		setSpeakerSpeed.addState(speakerspeed);
		
		Memory.addMapping(setSpeakerSpeed);
		
		//--- adjust speaker voice ---//
		Action setSpeakerVoice = new Action("?set) ?|speaker,speech,spoken,)) voice to @$voice");
		
		State<String> speakervoice = new State<String>("speakervoice", "", new String[] {"voice"}, new Execution<String>() {
			private static final long serialVersionUID = 4847183368300490585L;

			public String execute(String stateOld, Arg[] args) {
				//map args
				String voice = null;
				
				for (Arg arg : args) {
					if (arg.name.equals("voice")) {
						voice = (String) arg.getValue();
					}
				}
				
				//control speaker
				if (voice != null) {
					try {
						Speaker.setVoice(voice);
						Logger.log("speaker voice changed to " + voice, Logger.LEVEL_SPEECH);
					} 
					catch (SpeakerException e) {
						Logger.logError(e.getMessage(), Logger.LEVEL_SPEECH);
						voice = null;
					}
					
					notifier.set(true);
					return voice;
				}
				else {
					Logger.logError("no voice was specified", Logger.LEVEL_SPEECH);
					notifier.set(true);
					return null;
				}
			}
			
		});
		setSpeakerVoice.addState(speakervoice);
		
		Memory.addMapping(setSpeakerVoice);
		
		//--- demos ---//
		Action driverDemo1 = new Action("driver |demo,demonstration,) |one,1,)");
		
		State<Integer> driverdemoed = new State<Integer>("driverdemoed", 0, new String[] {}, new Execution<Integer>() {
			private static final long serialVersionUID = 7287040985627859604L;

			public Integer execute(Integer stateOld, Arg[] args) {
				//no args
				//direct driver
				Logger.log("typing in spotlight...");
				Driver.point(930, 30, null); //go to eclipse
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.clickLeft(null); //click window
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for refocus
				
				Driver.point(1375, 12, null); //go to spotlight
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.clickLeft(null); //click icon
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("this is a hello torry#lft)#lft)#lft)#bck)e#lft)#lft)from ", null);
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("#cmd+rgt)#exl)", null); //shift to end and add !
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("#cmd+bck)", null); //clear search
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.type("#lpr)#amp) I can use punctuation too#rpr)#tld)", null); //show off punctuation
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("#cmd+bck)#esc)", null); //clear search and exit
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Logger.log("quitting via mouse...");
				Driver.point(755, 899, null); //go to dock
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.point(908, 860, null); //go to java
				
				Driver.clickRight(null); //right-click menu
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for os to show options
				
				Driver.point(934, 771, notifier); //close option. last action; attach notifier
				
				//update state
				return 1;
			}
		});
		driverDemo1.addState(driverdemoed);
		
		Memory.addMapping(driverDemo1);
		
		Action overlayDemo1 = new Action("overlay |demo,demonstration,) |one,1,)");
		
		State<Integer> overlaydemoed = new State<Integer>("overlaydemoed", 0, new String[] {}, new Execution<Integer>() {
			private static final long serialVersionUID = -7000513250778527982L;
			
			@SuppressWarnings("unchecked")
			public Integer execute(Integer stateOld, Arg[] args) {
				//no args
				//direct prompter
				Prompter.showOverlay(null);
				Prompter.colorOverlay(Color.MEDIUMPURPLE, Color.PURPLE);
				
				Dimension screen = Driver.getScreen();
				Ellipse2D ball = new Ellipse2D.Double(20,20,20,20);
				AffineTransform t = new AffineTransform();
				int vx = 1; int vy = 1;
				
				SimpleObjectProperty<Integer> overlaydemoed = (SimpleObjectProperty<Integer>) states.get("overlaydemoed").getProperty();
				overlaydemoed.set(1);
				
				SimpleObjectProperty<Boolean> go = new SimpleObjectProperty<>(true);
				
				Scribe.state.addListener(new ChangeListener<Character>() {
					public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
						if (newValue == Scribe.STATE_RECORDING || newValue == Scribe.STATE_TRANSCRIBING) {
							go.set(false);
							Scribe.state.removeListener(this);
						}
					}
				});
				
				Logger.log("showing overlay demo one", Logger.LEVEL_SPEECH);
				while (go.get()) {
					t.translate(vx, vy);
					
					if (t.getTranslateX() > screen.width || t.getTranslateX() < 0) {
						vx = -vx;
						t.translate(vx, 0);
					}
					if (t.getTranslateY() > screen.height || t.getTranslateY() < 0) {
						vy = -vy;
						t.translate(0, vy);
					}
					
					Prompter.clearOverlay(null);
					Prompter.drawOverlay(ball.getPathIterator(t), true, true, null);
					
					try {
						Thread.sleep(10);
					} 
					catch (InterruptedException e) {
						go.set(false);
					}
				}
				Logger.log("overlay demo stopped", Logger.LEVEL_CONSOLE);
				Prompter.clearOverlay(null);
				Prompter.hideOverlay(null);
				notifier.set(false);
				
				//update state
				return 1;
			}
		});
		overlayDemo1.addState(overlaydemoed);
		
		Memory.addMapping(overlayDemo1);
	}
	
	public static void createLessons() throws LanguageMappingException {
		Logger.log("creating lessons");
		
		//--- learn widget ---//
		Lesson newWidget = new Lesson("@$name is @ttype ?|has,with,says,) @$label)", Lesson.TYPE_WIDGET);
		
		Definition newwidget = new Definition(new String[] {"name","type","label"}) {
			private static final long serialVersionUID = 7901389876329514500L;

			public void learn(Arg[] args) {
				String name = null;
				char type = Widget.TYPE_BUTTON;
				String label = null;
				
				//map args
				for (Arg arg : args) {
					Object value = arg.getValue();
					
					if (value != null) {
						if (arg.name.equals("name")) {
							name = (String) value;
						}
						else if (arg.name.equals("type")) {
							type = (char) value;
						}
						else if (arg.name.equals("label")) {
							label = (String) value;
						}
					}
				}
				
				//create widget
				try {
					Widget widget = new Widget(name);
					widget.setType(type);
					widget.setLabel(label);
					
					//ask for appearance, askYesNo requires fx thread
					String finalName = name;
					SimpleObjectProperty<Boolean> thisNotifier = this.notifier;
					Platform.runLater(new Runnable() {
						public void run() {
							try {
								boolean appears = Prompter.askYesNo("Define appearance for " + finalName, null, "Does " + finalName + " have any other visuals/graphics that can help me find it (an icon, for example)?");
								if (appears) {
									
									Prompter.state.addListener(new ChangeListener<Character>() {
										public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
											char state = newValue.charValue();
											
											if (state == Prompter.STATE_ZONE_COMPLETE || state == Prompter.STATE_ZONE_ABORTED) {
												//update memory
												Memory.addMapping(widget);
												Prompter.state.removeListener(this);
												thisNotifier.set(true);
											}
										}
									});
									
									Prompter.requestAppearance(widget, null);
								}
								else {
									//update memory
									Memory.addMapping(widget);
									thisNotifier.set(true);
								}
							}
							catch (PrompterException e) {
								Logger.logError(e.getMessage());
							}
						}
					});
				}
				catch (LanguageMappingException e) {
					Logger.logError(e.getMessage(), Logger.LEVEL_CONSOLE);
					Logger.logError("i could not create a widget named " + name, Logger.LEVEL_SPEECH);
					notifier.set(true);
				}
			}
		};
		newWidget.setDefinition(newwidget);
		
		Memory.addMapping(newWidget);
		
		//--- learn action by demonstration ---///
		Lesson demonstrateAction = new Lesson("?is) ?to) |show,demonstrate,demonstration,) how to @$action", Lesson.TYPE_ACTION);
		
		Definition demonstration = new Definition(new String[] {"action"}) {
			private static final long serialVersionUID = 7662523659663777292L;

			public void learn(Arg[] args) {
				String name = null;
				
				//map args
				for (Arg arg : args) {
					Object value = arg.getValue();
					
					if (value != null && arg.name.equals("action")) {
						name= (String) value;
					}
				}
				
				//enable demonstration recording and notify
				if (name != null) {
					try {
						Watcher.enable();
						
						Logger.log("i'm ready to record your demonstration", Logger.LEVEL_SPEECH);
						ButtonType response = Prompter.prompt(
							"Demonstrate \"" + name + "\"", 
							"Hit the READY button to begin demonstrating how to \"" + name + "\". To end the demonstration, type CMD/CTRL + ESC.", 
							new ButtonType(" Ready ", ButtonData.BIG_GAP),
							ButtonType.CANCEL);
						
						if (response == null || response == ButtonType.CANCEL) {
							Logger.log("demonstration aborted");
						}
						else {
							String finalName = name;
							
							try {
								SimpleObjectProperty<Boolean> thisNotifier = this.notifier;
								
								//handle demonstration end
								Watcher.state.addListener(new ChangeListener<Character>() {
									public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
										char c = newValue.charValue();
										boolean removeme = false;
										
										if (c == Watcher.STATE_RECORDING) {
											Logger.log("when you're finished hit the command/control and escape keys", Logger.LEVEL_SPEECH);
										}
										else if (c == Watcher.STATE_PAUSED) {
											//map demonstration to state transitions
											Watcher.compile(finalName);
											
											Logger.log("i'm reviewing your demonstration now", Logger.LEVEL_SPEECH);
											Logger.log(Watcher.demonstration.toString(), Logger.LEVEL_CONSOLE);
										}
										else if (c == Watcher.STATE_DONE) {
											//add new action to memory
											Memory.addMapping(Watcher.demonstratedAction);
											Logger.log("new demonstrated action " + finalName + " successfully learned", Logger.LEVEL_SPEECH);
											removeme = true;
										}
										else if (c == Watcher.STATE_FAILED) {
											Logger.logError("failed to learn from your demonstration", Logger.LEVEL_SPEECH);
											removeme = true;
										}
										
										if (removeme) {
											thisNotifier.set(false);
											Watcher.state.removeListener(this);
										}
									}
								});
								
								//start recording
								Watcher.record();
							} 
							catch (WatcherException e) {
								Logger.logError(e.getMessage(), Logger.LEVEL_CONSOLE);
								Logger.logError("i failed to record your demonstration", Logger.LEVEL_SPEECH);
							}
						}
					} 
					catch (WatcherException e) {
						Logger.logError(e.getMessage(), Logger.LEVEL_CONSOLE);
						Logger.logError("i won't be able to record your demonstration", Logger.LEVEL_SPEECH);
					} 
					catch (PrompterException e) {
						Logger.logError(e.getMessage(), Logger.LEVEL_CONSOLE);
					}
				}
				else {
					Logger.logError("cannot learn a new action without knowing what it's called", Logger.LEVEL_SPEECH);
				}
			}
		};
		demonstrateAction.setDefinition(demonstration);
		
		Memory.addMapping(demonstrateAction);
	}
}
