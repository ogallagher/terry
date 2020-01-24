package com.terry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Map.Entry;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public abstract class State implements Entry<String,Object>, Serializable {
	private static final long serialVersionUID = -5257200053676112985L;
	
	private static final char TYPE_BOOL = 'b';	//boolean
	private static final char TYPE_INT = 'i';	//int
	private static final char TYPE_STR = 's';	//string
	private static final char TYPE_PNT = 'p';	//point int[2]
	
	private String name;
	private char type;
	private SimpleObjectProperty<Object> value;
	private DriverExecution transition;
	
	public State(String name, char type, DriverExecution transition) {
		this.name = name;
		this.type = type;
		
		value = new SimpleObjectProperty<Object>(null);
		switch (type) {
			case TYPE_BOOL:
				value.set(false);
				break;
				
			case TYPE_INT:
				value.set(0);
				break;
				
			case TYPE_STR:
				value.set("");
				break;
				
			case TYPE_PNT:
				value.set(new int[] {0,0});
				break;
		} 
		
		this.transition = transition;
	}

	@Override
	public String getKey() {
		return name;
	}

	@Override
	public Object getValue() {
		return value.get();
	}
	
	@Override
	public Object setValue(Object newValue) {
		transition.execute(value, newValue);
		value.set(newValue);
		
		return newValue;
	}
	
	public void addListener(ChangeListener<Object> l) {
		value.addListener(l);
	}
	
	@Override
	public String toString() {
		return name + ":" + type + "=" + value.get();
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeChars(name);
		stream.writeChar(type);
		stream.writeObject(value.get());
		stream.writeObject(transition);
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		name = (String) stream.readObject();
		type = stream.readChar();
		value.set(stream.readObject());
		transition = (DriverExecution) stream.readObject();
	}
}