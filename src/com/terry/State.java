package com.terry;

import java.util.Map.Entry;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;

public abstract class State implements Entry<String,Object> {
	private String name;
	private SimpleObjectProperty<Object> value;
	private DriverExecution transition;
	
	public State(String name, DriverExecution transition) {
		this.name = name;
		this.value = new SimpleObjectProperty<Object>(null);
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
}