package com.terry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import com.terry.State.StateException;

public class Action extends LanguageMapping implements Serializable {
	private static final long serialVersionUID = -8618425214808650606L;
	
	private ArrayList<State<?>> states;
	
	public Action(String expr) {
		super(TYPE_ACTION, expr);
		
		states = new ArrayList<>();
	}
	
	public void addState(State<?> state) {
		//add entry to global state table
		if (Terry.states.get(state.getName()) == null) {
			Terry.states.put(state.getName(), state);
		}
		
		states.add(state);
	}
	
	/*
	 * Each state accepts an array of objects as arguments.
	 */
	public void execute(HashMap<String,Arg> allArgs) throws StateException {
		Logger.log("executing " + pattern);
		
		State<?> state;
		Arg[] args; //subset of allArgs for each state
		String[] argNames;
		
		for (int s=0; s<states.size(); s++) {
			state = states.get(s);
			
			//prep args
			argNames = state.getArgNames();
			args = new Arg[argNames.length];
			
			for (int a=0; a<args.length; a++) {
				Logger.log("mapping arg " + argNames[a]);
				args[a] = allArgs.get(argNames[a]);
			}
			
			//execute state transition
			state.transition(args);
		}
	}
	
	public int getId() {
		return id;
	}
	
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeChars(super.toString());
		stream.writeInt(states.size());
		
		for (State<?> state : states) {
			stream.writeObject(state);
		}
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		String lm = (String) stream.readObject();
		fromString(lm);
		
		for (int s=0; s<stream.readInt(); s++) {
			states.add((State<?>) stream.readObject());
		}
	}
}
