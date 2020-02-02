package com.terry;

import java.awt.geom.Point2D;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public class State<T> implements Serializable {
	private static final long serialVersionUID = -5257200053676112985L;
	
	private static final char TYPE_BOOL = 'b';	//boolean
	private static final char TYPE_INT = 'i';	//int
	private static final char TYPE_STR = 's';	//string
	private static final char TYPE_PNT = 'p';	//point int[2]
	
	private String name;
	private char type;
	private SimpleObjectProperty<T> value;
	private String[] argNames;
	private DriverExecution<T> transition;
	
	public State(String name, T value, String[] args, DriverExecution<T> transition) {
		this.name = name;
		this.value = new SimpleObjectProperty<T>(value);
		argNames = args;
		
		Class<?> typeClass = value.getClass();
		if (typeClass == Boolean.class) {
			type = TYPE_BOOL;
		}
		else if (typeClass == Integer.class) {
			type = TYPE_INT;
		}
		else if (typeClass == String.class) {
			type = TYPE_STR;
		}
		else if (typeClass == Point2D.class) {
			type = TYPE_PNT;
		}
		
		this.transition = transition;
	}
	
	public String getName() {
		return name;
	}
	
	public T getValue() {
		return value.get();
	}
	
	public String[] getArgNames() {
		return argNames;
	}
	
	/*
	 * Updates value and executes transition. 
	 * Call transition.setArgs() first.
	 */
	public void transition(Arg[] args) throws StateException {
		if (args.length == argNames.length) {
			value.set(transition.execute(value.get(), args));
		}
		else {
			throw new StateException(name + " transition expects " + argNames.length + "args; got " + args.length);
		}
	}
	
	public void addListener(ChangeListener<Object> l) {
		value.addListener(l);
	}
	
	@Override
	public String toString() {
		return name + ":" + type + "=" + value.get();
	}
	
	/*
	 * name type driver_execution
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeObject(name);
		stream.writeChar(type);
		stream.writeObject(transition);
		stream.writeObject(argNames);
	}
	
	@SuppressWarnings("unchecked")
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		name = (String) stream.readObject();
		type = stream.readChar();
		transition = (DriverExecution<T>) stream.readObject();
		argNames = (String[]) stream.readObject();
	}
	
	public static class StateException extends Exception {
		private static final long serialVersionUID = 8027619357141849662L;
		
		private String message;
		
		public StateException(String message) {
			this.message = message;
		}
		
		public StateException() {
			this.message = "state transition failed for unknown reason";
		}
		
		@Override
		public String getMessage() {
			return message;
		}
	}
}