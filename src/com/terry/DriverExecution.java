package com.terry;

import java.io.Serializable;

import com.terry.LanguageMapping.Arg;

public abstract class DriverExecution<T> implements Serializable {
	private static final long serialVersionUID = -4703545545934408077L;
	
	public abstract T execute(T stateOld, Arg[] args);
}
