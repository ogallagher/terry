package com.terry;

import java.io.Serializable;

public abstract class DriverExecution implements Serializable {
	private static final long serialVersionUID = -4703545545934408077L;
	
	private Object args;
	public abstract void execute(Object stateOld, Object stateNew);
	
	public void setArgs(Object args) {
		this.args = args;
	}
}
