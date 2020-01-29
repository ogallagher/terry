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
 * - State implements Entry<String,Object>
 * 		- member name is a String
 * 		- member value is an Object (observable)
 * 		- member execution is a DriverExecution: { abstract T execute(T oldState, Arg[] args) }
 * 		- member transition() calls execution.execute()
 */

package com.terry;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.HashMap;

import com.terry.Driver.DriverException;
import com.terry.Driver.DriverThread;
import com.terry.Memory.MemoryException;
import com.terry.Scribe.ScribeException;

public class Terry {
	public static Prompter prompter;
	
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
	
	public static void main(String[] args) {
		Logger.init();
		
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
		
		try {
			Driver.init();
		}
		catch (DriverException e) {
			Logger.logError(e.getMessage());
		}
		
		InstructionParser.init();
		
		try {
			Memory.init(); //calls LanguageMapping.init() from maps.txt file
		}
		catch (MemoryException e) {
			Logger.logError(e.getMessage());
		}
		
		if (LanguageMapping.empty()) {
			createPrimitiveActions();
			Logger.log(Memory.printDictionary());
		}
		
		// testing start
		
		// testing stop
		
		prompter = new Prompter();
		prompter.init(args);
	}
	
	private static void createPrimitiveActions() {
		Logger.log("no mappings found; creating primitive actions corpus");
		
		//--- move mouse to screen location ---//
		Action mouseToXY = new Action("?move) |mouse,cursor,pointer,) |to,two,too,) ?|location,position,coordinates,)) ?ex) @#x |ex,comma,why,) @#y ?why)");
		
		State<Point2D> mouseat = new State<Point2D>("mouseat", new Point2D.Float(), new String[] {"x","y"}, new DriverExecution<Point2D>() {
			private static final long serialVersionUID = -5509580894164954809L;
			
			public Point2D execute(Point2D stateOld, Arg[] args) {
				Float x = new Float(0);
				Float y = new Float(0);
				
				//map args
				for (Arg arg : args) {
					if (arg == null) {
						Logger.log("null arg");
					}
					else if (arg.name.equals("x")) {
						x = (Float) arg.value;
					}
					else if (arg.name.equals("y")) {
						y = (Float) arg.value;
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
		
		//--- click mouse button ---///
		Action mouseClickBtn = new Action("?|left,right,) click");
		
		State<Integer> clickbtn = new State<Integer>("clickbtn", 0, new String[] {"btn"}, new DriverExecution<Integer>() {
			private static final long serialVersionUID = -3163938142402546869L;

			public Integer execute(Integer stateOld, Arg[] args) {
		        Integer button = MouseEvent.BUTTON1;
		        
		        //map args
		        for (Arg arg : args) {
		        	if (arg.name.equals("btn")) {
		        		button = (Integer) arg.value;
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
						string = (String) arg.value;
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
		
		//--- demos ---//
		Action driverDemo1 = new Action("?do) ?driver) |demo,demonstration,) |one,won,run,)");
		
		State<Integer> demoed = new State<Integer>("demoed", 0, new String[] {}, new DriverExecution<Integer>() {
			private static final long serialVersionUID = 7287040985627859604L;

			public Integer execute(Integer stateOld, Arg[] args) {
				//no args
				//direct driver
				new DriverThread() {
					public void run() {
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
					}
				}.start();
				
				//update state
				return 1;
			}
		});
		driverDemo1.addState(demoed);
		
		Memory.addMapping(driverDemo1);
	}
}
