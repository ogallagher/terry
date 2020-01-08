package com.terry;

public abstract class DriverExecution {
	private Object args;
	public abstract void execute(Object stateOld, Object stateNew);
	
	public void setArgs(Object args) {
		this.args = args;
	}
}
