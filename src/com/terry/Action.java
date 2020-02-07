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
		
		if (!states.contains(state)) {
			states.add(state);
		}
	}
	
	public void addStates() {
		for (State<?> state : states) {
			Terry.states.put(state.getName(), state);
		}
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
			Arg arg;
			
			for (int a=0; a<args.length; a++) {
				arg = allArgs.get(argNames[a]);
				Logger.log("mapping arg " + argNames[a] + " to " + arg);
				args[a] = arg;
			}
			
			//execute state transition
			state.transition(args);
		}
	}
	
	/*
	 * type   id   pattern   state_num   <states>
	 */
	private void writeObject(ObjectOutputStream stream) throws IOException {
		System.out.println("serializing action " + id);
		
		stream.writeObject(super.toString());
		stream.writeInt(states.size());
		
		for (State<?> state : states) {
			stream.writeObject(state);
		}
	}
	
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		String lm = (String) stream.readObject();
		fromString(lm);
		
		int ns = stream.readInt();
		
		states = new ArrayList<>();
		for (int s=0; s<ns; s++) {
			states.add((State<?>) stream.readObject());
		}
		
		Logger.log("deserialized action " + id);
	}
}
