package com.terry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
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
		
		Arg[] args = new Arg[definition.argNames.length];
		for (int i=0; i<args.length; i++) {
			args[i] = allArgs.get(definition.argNames[i]);
		}
		definition.learn(args);
	}
	
	/*
	 * type   id   pattern   lesson_type   definition
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		System.out.println("serializing lesson " + id);
		
		stream.writeObject(super.toString());
		stream.writeChar(type);
		stream.writeObject(definition);
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		fromString((String) stream.readObject());
		type = stream.readChar();
		definition = (Definition) stream.readObject();
		
		Logger.log("deserialized lesson " + id);
	}
	
	public static abstract class Definition implements Serializable {
		private static final long serialVersionUID = 129440776453380438L;
		
		private String[] argNames;
		
		public Definition(String[] args) {
			argNames = args;
		}
		
		public abstract void learn(Arg[] args);
	}
}
