package com.terry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.LinkedList;

public class Action extends LanguageMapping implements Serializable {
	private static final long serialVersionUID = -8618425214808650606L;
	
	private LinkedList<State> states;
	
	public Action(String expr) {
		super(TYPE_ACTION, expr);
		
		states = new LinkedList<State>();
	}
	
	public int getId() {
		return id;
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeChars(super.toString());
		stream.writeInt(states.size());
		
		for (State state : states) {
			stream.writeObject(state);
		}
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		String lm = (String) stream.readObject();
		fromString(lm);
		
		for (int s=0; s<stream.readInt(); s++) {
			states.add((State) stream.readObject());
		}
	}
}
