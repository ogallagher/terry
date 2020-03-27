/*
 * Author:	Owen Gallagher
 * Start:	December 2019
 * 
 * Notes:
 * 	- dictionary 
 * 		- an entry is token paired with LangMapping ids whose patterns contain that token
 * 		- only keywords and widgets can have entries; value args would not have entries
 * 		- an entry is in the form: token ref_1 ref_2 ... ref_n
 * 	- LanguageMapping/LangMap
 * 		- superclass for lessons, actions and widgets, with a pattern and mappings for keywords and args
 * 		- LangMap.id is a unique int id for each
 * - Action
 * 		- inherited member LangMap.value is an state name and value pairing as Entry<String,Object>
 * 		- args define what widget to perform the action on and how to do the action
 * - State
 * 		- member name is a String
 * 		- member value is an Object (observable)
 * 		- member execution is a DriverExecution: { abstract T execute(T oldState, Arg[] args) }
 * 		- member transition() calls execution.execute()
 * - Widget
 * 		- member type is the widget interaction type (label, button, text area, etc)
 * 		- member label is the string contained within the widget's bounds
 * 		- member bounds is a rectangle to define the size and shape of the widget
 * 		- member appearance is a collection of features (keypoint-descriptor pairs) for visual identification
 * - Lesson
 * 		- member type
 * 		- member definition
 * 
 * TODO:
 * 	- finish Widget.Appearance
 *  - accept widget appearances with prompter
 *  - create action learner
 *  - create watcher connected to keyboard and mouse
 *  	- create os input hooks to catch keystrokes and mouse updates: https://stackoverflow.com/a/43885566/10200417
 *  	- trigger scribe with key combination
 */

package com.terry;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.opencv_java;

import com.terry.Driver.DriverException;
import com.terry.Lesson.Definition;
import com.terry.Memory.MemoryException;
import com.terry.Scribe.ScribeException;
import com.terry.State.StateException;
import com.terry.Widget.WidgetException;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.paint.Color;

public class Terry {
	public static Prompter prompter;
	
	public static Widget dummyWidget;
	
	public static HashMap<String,State<?>> states = new HashMap<>();
	
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
	public static final String KEY_DASH = "dsh";
	public static final String KEY_PLUS = "pls";
	public static final String KEY_LBRACE = "lbr";
	public static final String KEY_RBRACE = "rbr";
	public static final String KEY_PIPE = "pip";
	public static final String KEY_COLON = "col";
	public static final String KEY_DOUBLE_QUOTE = "dqt";
	public static final String KEY_LESS = "lss";
	public static final String KEY_GREATER = "gtr";
	public static final String KEY_QUERY = "qry";
	
	public static final int EXITCODE_MEMORY = 1;
	
	public static void main(String[] args) {
		Logger.init();
		
		try {
			Widget.init();
			dummyWidget = new Widget("ddumy"); //purposefully misspelled so widgets called "dummy" can still be created
		} 
		catch (WidgetException e) {
			Logger.logError(e.getMessage());
		}
		
		Arg.init();
		
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.startsWith("win")) {
			os = OS_WIN;
			Logger.log("detected win os");
		}
		else if (osName.startsWith("mac")) {
			os = OS_MAC;
			Logger.log("detected mac os");
		}
		else {
			os = OS_OTHER;
			Logger.logError("detected unsupported os: " + osName);
			System.exit(1);
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
			createPrimitiveActions();
			createLessons();
		}
		Logger.log(Memory.printDictionary());
		
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
	
	public static void triggerScribe() {
		try {
			switch (Scribe.state.get()) {
				case Scribe.STATE_IDLE:
					Scribe.start();
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
	
	private static void createPrimitiveActions() {
		Logger.log("no mappings found; creating primitive actions corpus");
		
		//--- move mouse to screen location ---//
		Action mouseToXY = new Action("?move) ?|mouse,cursor,pointer,)) to ?|location,position,coordinates,)) ?x) @#x |x,comma,y,) @#y ?y)");
		
		State<Point2D> mouseat = new State<Point2D>("mouseat", new Point2D.Float(), new String[] {"x","y"}, new DriverExecution<Point2D>() {
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
				Driver.point(x.intValue(), y.intValue());
				
				//update state
				return new Point2D.Float(x, y);
			}
		});
		mouseToXY.addState(mouseat);
		
		Memory.addMapping(mouseToXY);
		
		//--- click mouse button ---//
		Action mouseClickBtn = new Action("?@dbtn) click");
		
		State<Integer> clickbtn = new State<Integer>("clickbtn", 0, new String[] {"btn"}, new DriverExecution<Integer>() {
			private static final long serialVersionUID = -3163938142402546869L;

			public Integer execute(Integer stateOld, Arg[] args) {
		        Integer button = MouseEvent.BUTTON1;
		        
		        //map args
		        for (Arg arg : args) {
		        	Object value = arg.getValue();
		        	
		        	if (value == null) {
						Logger.log("null arg");
					}
		        	else if (arg.name.equals("btn")) {
		        		String direction = (String) value;
		        		
		        		if (direction.equals("right")) {
		        			button = MouseEvent.BUTTON2;
		        		}
		        		//else, assume button 1
		        	}
		        }
		        
		        //direct driver
		        if (button == MouseEvent.BUTTON1) {
		            Driver.clickLeft();
		        }
		        else if (button == MouseEvent.BUTTON2) {
		            Driver.clickRight();
		        }
		        
		        //update state
		        return button;
		    }
		});
		mouseClickBtn.addState(clickbtn);
		
		Memory.addMapping(mouseClickBtn);
		
		//--- type string ---//
		Action typeStr = new Action("type ?out) ?following string) @$str ?end quote)");
		
		State<String> typed = new State<String>("typed", "", new String[] {"str"}, new DriverExecution<String>() {
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
				Driver.type(string);
				
				//update state
				return string;
			}
		});
		typeStr.addState(typed);
		
		Memory.addMapping(typeStr);
		
		//--- shutdown ---//
		Action shutdown = new Action("|[shut_down],[turn_off],quit,)");
		
		State<Boolean> quitted = new State<Boolean>("quitted", false, new String[] {}, new DriverExecution<Boolean>() {
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
		Action showstate = new Action("|show,[what_is],log,) ?current) state");
		
		State<Boolean> stateshown = new State<Boolean>("stateshown", true, new String[] {}, new DriverExecution<Boolean>() {
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
		showstate.addState(stateshown);
		
		Memory.addMapping(showstate);
		
		//--- capture screen/take screenshot ---//
		Action captureScreen = new Action("?|create,take,)) |screenshot,[screen_shot],[screen_capture],[capture_screen],)");
		
		//set captureupdated to false, which statecaptured will then set to true when the screen capture is obtained
		State<Boolean> statecaptureupdated = new State<Boolean>("statecaptureupdated", Boolean.FALSE, new String[] {}, new DriverExecution<Boolean>() {
			private static final long serialVersionUID = -5850373248159506280L;

			@Override
			public Boolean execute(Boolean stateOld, Arg[] args) {
				return false;
			}
		});
		captureScreen.addState(statecaptureupdated);
		
		State<BufferedImage> statecaptured = new State<BufferedImage>("statecaptured", new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB), new String[] {}, new DriverExecution<BufferedImage>() {
			private static final long serialVersionUID = 4607129019674379163L;

			public BufferedImage execute(BufferedImage stateOld, Arg[] args) {
				//no args
				//direct driver and prompter
				Prompter.hide();
				
				Dimension screen = Driver.getScreen();
				BufferedImage capture = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_RGB);
				
				//make sure hide happens first
				//TODO Platform.runLater may now be redundant
				Platform.runLater(new Runnable() {
					public void run() {
						//handle capture result
						Driver.captured.addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
								if (newValue) {
									SwingFXUtils.fromFXImage(Driver.capture, capture);
									
									//update statecaptureupdated when the capture object contains the data
									SimpleObjectProperty<Boolean> captureUpdated = statecaptureupdated.getProperty();
									captureUpdated.set(true);
									
									if (capture != null) {
										Utilities.saveImage(capture, Terry.RES_PATH + "vision/", "screen_capture.png");
									}
									
									Prompter.show();
								}
							}
						});
						Driver.captureScreen();
					}
				});
				
				return capture;
			}
		});
		captureScreen.addState(statecaptured);
		
		Memory.addMapping(captureScreen);
		
		//--- find widget location in screen ---//
		Action locateWidget = new Action("|find,locate,show,) ?where) ?is) @wwidget ?is)");
		
		State<Boolean> widgetlocationupdated = new State<Boolean>("widgetlocationupdated", Boolean.FALSE, new String[] {}, new DriverExecution<Boolean>() {
			private static final long serialVersionUID = 1721914958113344877L;
			
			public Boolean execute(Boolean stateOld, Arg[] args) {
				return false;
			}
		});
		locateWidget.addState(widgetlocationupdated);
		
		State<Point2D> widgetlocation = new State<Point2D>("widgetlocation", new Point2D.Double(), new String[] {"widget"}, new DriverExecution<Point2D>() {
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
						//execute screen capture action
						captureScreen.execute(captureScreenArgs);
						
						//listen for when screen capture is complete
						SimpleObjectProperty<Boolean> captureUpdated = statecaptureupdated.getProperty();
						captureUpdated.addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
								if (newValue.booleanValue()) {
									//get capture and search
									BufferedImage screenshot;
									try {
										screenshot = statecaptured.getValue();
										
										//handle result of widget search
										finalWidget.state = new CharProperty(Widget.STATE_IDLE);
										finalWidget.state.addListener(new ChangeListener<Character>() {
											public void changed(ObservableValue<? extends Character> observable, Character oldValue, Character newValue) {
												if (newValue == Widget.STATE_FOUND) {
													Rectangle zone = finalWidget.getZone();
													
													Logger.log("widget found at " + zone.getX() + " " + zone.getY() + " " + zone.getWidth() + " " + zone.getHeight());
													
													//direct prompter to highlight found widget
													Prompter.clearOverlay();
													Prompter.showOverlay();
													Prompter.colorOverlay(new Color(1,1,1,0.2), Color.YELLOW);
													Prompter.drawOverlay(zone.getPathIterator(null), true, true);
													
													//update state(s)
													location.setLocation(zone.getCenterX(), zone.getCenterY());
													widgetlocationupdated.getProperty().set(true);
												}
												else if (newValue == Widget.STATE_NOT_FOUND) {
													Logger.log("widget not found");
												}
											}
										});
										
										//direct widget to find itself
										finalWidget.findInScreen(screenshot);
									}
									catch (WidgetException e) {
										Logger.logError("widget search failed: " + e.getMessage());
									}
								}
							}
						});
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
		
		State<Widget> mouseatwidget = new State<Widget>("mouseatwidget", dummyWidget, new String[] {"widget"}, new DriverExecution<Widget>() {
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
						//widgetlocationupdated.getProperty().set(false);
						widgetlocationupdated.getProperty().addListener(new ChangeListener<Boolean>() {
							public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {								
								if (newValue) {
									//move mouse to widget location
									Point2D location = widgetlocation.getProperty().get();
									int x = (int) location.getX();
									int y = (int) location.getY();
									
									//direct driver
									Driver.point(x, y);
									
									//update mouse location
									mouseat.getProperty().set(new Point2D.Float(x,y));
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
		
		//--- demos ---//
		Action driverDemo1 = new Action("?do) driver |demo,demonstration,) |one,1,)");
		
		State<Integer> driverDemoed = new State<Integer>("driverdemoed", 0, new String[] {}, new DriverExecution<Integer>() {
			private static final long serialVersionUID = 7287040985627859604L;

			public Integer execute(Integer stateOld, Arg[] args) {
				//no args
				//direct driver
				Logger.log("typing in spotlight...");
				Driver.point(930, 30); //go to eclipse
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.clickLeft(); //click window
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for refocus
				
				Driver.point(1375, 12); //go to spotlight
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.clickLeft(); //click icon
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("this is a hello torry#lft)#lft)#lft)#bck)e#lft)#lft)from ");
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("#cmd+rgt)#exl)"); //shift to end and add !
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("#cmd+bck)"); //clear search
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.type("#lpr)#amp) I can use punctuation too#rpr)#tld)"); //show off punctuation
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Driver.type("#cmd+bck)#esc)"); //clear search and exit
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {}
				
				Logger.log("quitting via mouse...");
				Driver.point(755, 899); //go to dock
				
				try {Thread.sleep(500);} catch (InterruptedException e) {}
				
				Driver.point(908, 860); //go to java
				
				Driver.clickRight(); //right-click menu
				
				try {Thread.sleep(1000);} catch (InterruptedException e) {} //wait for os to show options
				
				Driver.point(934, 771); //close option
				
				//update state
				return 1;
			}
		});
		driverDemo1.addState(driverDemoed);
		
		Memory.addMapping(driverDemo1);
		
		Action overlayDemo1 = new Action("?do) overlay |demo,demonstration,) |one,1,)");
		
		State<Integer> overlayDemoed = new State<Integer>("overlaydemoed", 0, new String[] {}, new DriverExecution<Integer>() {
			private static final long serialVersionUID = -7000513250778527982L;
			
			@SuppressWarnings("unchecked")
			public Integer execute(Integer stateOld, Arg[] args) {
				//no args
				//direct prompter
				Prompter.showOverlay();
				Prompter.colorOverlay(Color.MEDIUMPURPLE, Color.PURPLE);
				
				Dimension screen = Driver.getScreen();
				Ellipse2D ball = new Ellipse2D.Double(20,20,20,20);
				AffineTransform t = new AffineTransform();
				int vx = 1; int vy = 1;
				
				SimpleObjectProperty<Integer> overlaydemoed = (SimpleObjectProperty<Integer>) states.get("overlaydemoed").getProperty();
				overlaydemoed.set(1);
				
				boolean go = true;
				Logger.log("drawing overlay circle");
				while (go && overlaydemoed.get().equals(1)) {
					t.translate(vx, vy);
					
					if (t.getTranslateX() > screen.width || t.getTranslateX() < 0) {
						vx = -vx;
						t.translate(vx, 0);
					}
					if (t.getTranslateY() > screen.height || t.getTranslateY() < 0) {
						vy = -vy;
						t.translate(0, vy);
					}
					
					Prompter.clearOverlay();
					Prompter.drawOverlay(ball.getPathIterator(t), true, true);
					
					try {
						Thread.sleep(10);
					} 
					catch (InterruptedException e) {
						go = false;
					}
				}
				Logger.log("drawing done");
				Prompter.hideOverlay();
				
				//update state
				return 1;
			}
		});
		overlayDemo1.addState(overlayDemoed);
		
		Memory.addMapping(overlayDemo1);
	}
	
	public static void createLessons() {
		Logger.log("creating lessons");
		
		//--- create widget with label ---//
		Lesson newWidget = new Lesson("@$name ?is @ttype) |has,with,says,) @$label", Lesson.TYPE_WIDGET);
		
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
						Logger.log("arg: " + arg.name + " = " + value);
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
				
				//update memory
				Widget widget = new Widget(name);
				widget.setType(type);
				widget.setLabel(label);
				
				Memory.addMapping(widget);
			}
		};
		newWidget.setDefinition(newwidget);
		
		Memory.addMapping(newWidget);
	}
}
