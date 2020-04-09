package com.terry;

import java.awt.Color;
import java.util.ArrayList;

import com.terry.Memory.Lookup;

public class Arg {
	public String name;				//name of arg in language mapping expression
	private Object value = null;	//usable arg value
	private String text = "";		//tokens that created the value
	
	//arg types cannot be ? + * ) as these are reserved for pattern structure
	public static final char notarg = '0';		//not arg
	public static final char strarg = '$';		//string
	public static final char numarg = '#';		//number
	public static final char wigarg = 'w';		//widget
	public static final char colarg = 'c';		//color
	public static final char spdarg = '>';		//speed
	public static final char dirarg = 'd';		//direction
	public static final char wtparg = 't';		//widget type
	public static final char[] argtypes = new char[] {strarg,numarg,wigarg,colarg,spdarg,dirarg,wtparg};
	
	//color arg values
	public static final String COLARG_RED = "red";
	public static final String COLARG_GREEN = "green";
	public static final String COLARG_BLUE = "blue";
	public static final String COLARG_INDIGO = "indigo";
	public static final String COLARG_YELLOW = "yellow";
	public static final String COLARG_TURQUOISE = "turquoise";
	public static final String COLARG_CYAN = "cyan";
	public static final String COLARG_PURPLE = "purple";
	public static final String COLARG_VIOLET = "violet";
	public static final String COLARG_PINK = "pink";
	public static final String COLARG_ORANGE = "orange";
	public static final String COLARG_WHITE = "white";
	public static final String COLARG_BLACK = "black";
	public static final String COLARG_BROWN = "brown";
	public static final String COLARG_GRAY = "gray";
	public static ArrayList<String> colargs;
	
	//speed arg values
	public static final String SPDARG_SLOW = "slow";
	public static final String SPDARG_SLOWLY = "slowly";
	public static final String SPDARG_QUICK = "quick";
	public static final String SPDARG_QUICKLY = "quickly";
	public static final String SPDARG_FAST = "fast";
	public static final String SPDARG_MEDIUM = "medium";
	public static ArrayList<String> spdargs;
	
	//direction arg values
	public static final String DIRARG_UP = "up";
	public static final String DIRARG_RIGHT = "right";
	public static final String DIRARG_DOWN = "down";
	public static final String DIRARG_LEFT = "left";
	public static final String DIRARG_MIDDLE = "middle";
	public static ArrayList<String> dirargs;
	
	//number word arg values
	public static ArrayList<String> numargs;
	
	//widget type arg values
	public static final String WTPARG_BUTTON = "button";
	public static final String WTPARG_LABEL = "label";
	public static final String WTPARG_GRAPHIC = "graphic";
	public static final String WTPARG_RADIO = "radio";
	public static final String WTPARG_CHECKBOX = "checkbox";
	public static final String WTPARG_TEXTBOX = "textbox";
	public static ArrayList<String> wtpargs;
	
	public static void init() {
		colargs = new ArrayList<>();
		colargs.add(COLARG_RED);
		colargs.add(COLARG_GREEN);
		colargs.add(COLARG_BLUE);
		colargs.add(COLARG_INDIGO);
		colargs.add(COLARG_YELLOW);
		colargs.add(COLARG_TURQUOISE);
		colargs.add(COLARG_CYAN);
		colargs.add(COLARG_PURPLE);
		colargs.add(COLARG_VIOLET);
		colargs.add(COLARG_PINK);
		colargs.add(COLARG_ORANGE);
		colargs.add(COLARG_WHITE);
		colargs.add(COLARG_BLACK);
		colargs.add(COLARG_BROWN);
		colargs.add(COLARG_GRAY);
		
		spdargs = new ArrayList<>();
		spdargs.add(SPDARG_SLOW);
		spdargs.add(SPDARG_SLOWLY);
		spdargs.add(SPDARG_QUICK);
		spdargs.add(SPDARG_QUICKLY);
		spdargs.add(SPDARG_FAST);
		spdargs.add(SPDARG_MEDIUM);
		
		dirargs = new ArrayList<>();
		dirargs.add(DIRARG_UP);
		dirargs.add(DIRARG_RIGHT);
		dirargs.add(DIRARG_DOWN);
		dirargs.add(DIRARG_LEFT);
		
		numargs = new ArrayList<>();
		numargs.add("zero");
		numargs.add("one");
		numargs.add("two");
		numargs.add("three");
		numargs.add("four");
		numargs.add("five");
		numargs.add("six");
		numargs.add("seven");
		numargs.add("eight");
		numargs.add("nine");
		numargs.add("ten");
		numargs.add("eleven");
		numargs.add("twelve");
		numargs.add("thirteen");
		numargs.add("fourteen");
		numargs.add("fifteen");
		numargs.add("sixteen");
		numargs.add("seventeen");
		numargs.add("eighteen");
		numargs.add("nineteen");
		numargs.add("twenty");
		numargs.add("thirty");
		numargs.add("forty");
		numargs.add("fifty");
		numargs.add("sixty");
		numargs.add("seventy");
		numargs.add("eighty");
		numargs.add("ninety");
		numargs.add("hundred");
		numargs.add("thousand");
		numargs.add("million");
		
		wtpargs = new ArrayList<>();
		wtpargs.add(WTPARG_BUTTON);
		wtpargs.add(WTPARG_LABEL);
		wtpargs.add(WTPARG_GRAPHIC);
		wtpargs.add(WTPARG_RADIO);
		wtpargs.add(WTPARG_CHECKBOX);
		wtpargs.add(WTPARG_TEXTBOX);
	}
	
	public Arg() {}
	
	public Arg(String name, Object value, String text) {
		this.name = name;
		this.value = value;
		this.text = text;
	}
	
	public Arg(Arg clone, char argType) {
		name = clone.name;
		text = clone.text;
		value = clone.value; //clone.value is immutable; only reassigned for changes
	}
	
	public void setValue(Object value, String text) {
		this.value = value;
		this.text = text;
	}
	
	public Object getValue() {
		return value;
	}
	
	public String getText() {
		return text;
	}
	
	public int tokenCount() {
		return text.split("\\s+").length;
	}
	
	/*
	 * return true if multiword arg accepted
	 * return false if multiword arg is invalid
	 */
	public boolean appendToken(char argType, String token) {
		String multiword = text + " " + token;
		Object newValue = getArgValue(argType, multiword);
		
		if (newValue == null) {
			return false;
		}
		else {
			text = multiword;
			value = newValue;
			return true;
		}
	}
	
	@Override
	public String toString() {
		if (value == null) {
			return "null";
		}
		else {
			return value.toString();
		}
	}
	
	public static Object getArgValue(char argType, String next) {
		Object argValue = null;
		
		switch (argType) {
			case colarg:
				argValue = Arg.getColor(next);
				if (argValue != null) {
					Logger.log("valid color " + next);
				}
				break;
				
			case dirarg:
				argValue = Arg.getDirection(next);
				if (argValue != null) {
					Logger.log("valid direction " + next);
				}
				break;
				
			case numarg:
				argValue = Arg.getNumeric(next);
				if (argValue != null) {
					Logger.log("valid number " + next);
				}
				break;
				
			case spdarg:
				argValue = Arg.getSpeed(next);
				if (argValue != null) {
					Logger.log("valid speed " + next);
				}
				break;
				
			case strarg:
				argValue = next;
				break;
				
			case wtparg:
				argValue = Arg.getWidgetType(next);
				if (argValue != null) {
					Logger.log("valid widget type " + next);
				}
				break;
				
			case wigarg:
				argValue = Arg.getWidget(next);
				if (argValue != Terry.dummyWidget) {
					Logger.log("found known widget " + next);
				}
				break;
				
			default:
				Logger.logError("unknown arg type " + argType);
				break;
		}
		
		return argValue;
	}
	
	public static Color getColor(String color) {
		if (color.equals(COLARG_RED) || color.equals("head")) {
			return Color.red;
		}
		else if (color.equals(COLARG_GREEN)) {
			return Color.green;
		}
		else if (color.equals(COLARG_BLUE) || color.equals("lose")) {
			return Color.blue;
		}
		else if (color.equals(COLARG_INDIGO)) {
			return Color.blue;
		}
		else if (color.equals(COLARG_YELLOW) || color.equals("hello")) {
			return Color.yellow;
		}
		else if (color.equals(COLARG_TURQUOISE)) {
			return new Color(0, 200, 255);
		}
		else if (color.equals(COLARG_CYAN)) {
			return Color.cyan;
		}
		else if (color.equals(COLARG_PURPLE)) {
			return new Color(150, 0, 240);
		}
		else if (color.equals(COLARG_VIOLET)) {
			return Color.MAGENTA;
		}
		else if (color.equals(COLARG_PINK)) {
			return Color.pink;
		}
		else if (color.equals(COLARG_ORANGE)) {
			return Color.orange;
		}
		else if (color.equals(COLARG_WHITE) || color.equals("wife")) {
			return Color.white;
		}
		else if (color.equals(COLARG_BLACK) || color.equals("back")) {
			return Color.black;
		}
		else if (color.equals(COLARG_BROWN)) {
			return new Color(180, 50, 90);
		}
		else if (color.equals(COLARG_GRAY) || color.equals("ray")) {
			return Color.gray;
		}
		else {
			return null;
		}
	}
	
	public static String getDirection(String direction) {
		if (direction.equals(DIRARG_UP)) {
			return DIRARG_UP;
		}
		else if (direction.equals(DIRARG_RIGHT) || direction.equals("write")) {
			return DIRARG_RIGHT;
		}
		else if (direction.equals(DIRARG_DOWN)) {
			return DIRARG_DOWN;
		}
		else if (direction.equals(DIRARG_LEFT)) {
			return DIRARG_LEFT;
		}
		else {
			return null;
		}
	}
	
	/*
	 * Adapted from https://stackoverflow.com/a/26951693/10200417. 
	 * Handles multiword number names (ex: one thousand three hundred fifty four)
	 */
	public static Float getNumeric(String numeric) {
		float digit = 0;
		float number = 0;
		
		try {
			number = Float.parseFloat(numeric);
			
			return number;
		}
		catch (NumberFormatException e) {
			if (numeric != null && numeric.length() > 0) {
				numeric = numeric.replaceAll("-", " ").replaceAll(" and", " ");
				String[] words = numeric.trim().split("\\s+");
				
				for (String str : words) {
					if (!numargs.contains(str)) {
						return null;
					}
					
					if(str.equals("zero")) {
						digit += 0;
					}
					else if(str.equals("one") || str.equals("won")) {
						digit += 1;
					}
					else if(str.equals("two") || str.equals("to") || str.equals("too")) {
						digit += 2;
					}
					else if(str.equals("three")) {
						digit += 3;
					}
					else if(str.equals("four") || str.equals("for")) {
						digit += 4;
					}
					else if(str.equals("five")) {
						digit += 5;
					}
					else if(str.equals("six")) {
						digit += 6;
					}
					else if(str.equals("seven")) {
						digit += 7;
					}
					else if(str.equals("eight")) {
						digit += 8;
					}
					else if(str.equals("nine")) {
						digit += 9;
					}
					else if(str.equals("ten")) {
						digit += 10;
					}
					else if(str.equals("eleven")) {
						digit += 11;
					}
					else if(str.equals("twelve")) {
						digit += 12;
					}
					else if(str.equals("thirteen")) {
						digit += 13;
					}
					else if(str.equals("fourteen")) {
						digit += 14;
					}
					else if(str.equals("fifteen")) {
						digit += 15;
					}
					else if(str.equals("sixteen")) {
						digit += 16;
					}
					else if(str.equals("seventeen")) {
						digit += 17;
					}
					else if(str.equals("eighteen")) {
						digit += 18;
					}
					else if(str.equals("nineteen")) {
						digit += 19;
					}
					else if(str.equals("twenty")) {
						digit += 20;
					}
					else if(str.equals("thirty")) {
						digit += 30;
					}
					else if(str.equals("forty")) {
						digit += 40;
					}
					else if(str.equals("fifty")) {
						digit += 50;
					}
					else if(str.equals("sixty")) {
						digit += 60;
					}
					else if(str.equals("seventy")) {
						digit += 70;
					}
					else if(str.equals("eighty")) {
						digit += 80;
					}
					else if(str.equals("ninety")) {
						digit += 90;
					}
					else if(str.equals("hundred")) {
						digit *= 100;
					}
					else if(str.equals("thousand")) {
						digit *= 1000;
						number += digit;
						digit=0;
					}
					else if(str.equals("million")) {
						digit *= 1000000;
						number += digit;
						digit=0;
					}
				}
				
				number += digit;
				digit=0;
				Logger.log(numeric + " = " + number);
			}
			
			return number;
		}
	}
	
	public static String getSpeed(String speed) {
		if (speed.equals(SPDARG_SLOW) || speed.equals(SPDARG_SLOWLY)) {
			return SPDARG_SLOW;
		}
		else if (speed.equals(SPDARG_QUICK) || speed.equals(SPDARG_QUICKLY) || speed.equals(SPDARG_FAST)) {
			return SPDARG_FAST;
		}
		else if (speed.equals(SPDARG_MEDIUM)) {
			return SPDARG_MEDIUM;
		}
		else {
			return null;
		}
	}
	
	public static Widget getWidget(String name) {
		ArrayList<Lookup> entries = Memory.dictionaryLookup(name, false, false);
		
		if (entries == null) {
			return Terry.dummyWidget;
		}
		else {
			for (Lookup entry : entries) {
				for (LanguageMapping lm : entry.mappings) {
					try {
						Widget widget = (Widget) lm;
						return widget;
					}
					catch (ClassCastException e) {
						//not widget, fail quietly
					}
				}
			}
		}
		
		return null;
	}
	
	public static Character getWidgetType(String type) {
		if (type.equals(WTPARG_LABEL)) {
			return Widget.TYPE_LABEL;
		}
		else if (type.equals(WTPARG_BUTTON)) {
			return Widget.TYPE_BUTTON;
		}
		else if (type.equals(WTPARG_GRAPHIC)) {
			return Widget.TYPE_GRAPHIC;
		}
		else if (type.equals(WTPARG_RADIO)) {
			return Widget.TYPE_RADIO;
		}
		else if (type.equals(WTPARG_CHECKBOX)) {
			return Widget.TYPE_CHECK;
		}
		else if (type.equals(WTPARG_TEXTBOX)) {
			return Widget.TYPE_TEXTBOX;
		}
		else {
			return null;
		}
	}
}
