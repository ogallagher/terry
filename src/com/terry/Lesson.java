package com.terry;

import java.io.Serializable;
import java.util.HashMap;

public class Lesson extends LanguageMapping implements Serializable {
	private static final long serialVersionUID = 6905698412940472554L;
	
	public static final char TYPE_UNKNOWN = '?';
	public static final char TYPE_WIDGET = 'w';
	public static final char TYPE_ACTION = 'a';
	
	private char type = TYPE_UNKNOWN;
	private Definition definition = null;
	
	public Lesson(String expr, char typ) {
		super(TYPE_LESSON, expr);
		type = typ;
	}
	
	public void setDefinition(Definition def) {
		definition = def;
	}
	
	public void learn(HashMap<String,Arg> allArgs) {
		Logger.log("learning " + type + ": " + pattern);
		definition.learn(allArgs);
	}
	
	private static abstract class Definition implements Serializable {
		private String[] argNames;
		
		public Definition(String[] args) {
			argNames = args;
		}
		
		public abstract void learn(HashMap<String,Arg> args);
	}
}
