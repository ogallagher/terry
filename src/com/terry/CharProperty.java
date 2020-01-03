package com.terry;

import javafx.beans.property.ObjectPropertyBase;

public class CharProperty extends ObjectPropertyBase<Character> {
	private Object bean = null;
	
	public CharProperty(char value) {
		set(value);
	}

	public void setBean(Object parent) {
		bean = parent;
	}
		
	@Override
	public Object getBean() {
		return bean;
	}

	//has no name
	@Override
	public String getName() {
		return null;
	}
}
