package com.terry;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

import com.terry.State.StateException;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class Action extends LanguageMapping implements Serializable {
	private static final long serialVersionUID = -8618425214808650606L;
	
	private ArrayList<State<?>> states;
	
	public Action(String expr) throws LanguageMappingException {
		super(TYPE_ACTION, expr);
		
		states = new ArrayList<>();
	}
	
	public static void init() throws LanguageMappingException {
		Terry.dummyAction = new Action("ddummya");
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
	
	//add states to global Terry.states
	public void addStates() {
		for (State<?> state : states) {
			if (Terry.states.get(state.getName()) == null) {
				Terry.states.put(state.getName(), state);
			}
		}
	}
	
	/*
	 * Each state accepts an array of objects as arguments.
	 */
	public SimpleObjectProperty<Boolean> execute(HashMap<String,Arg> allArgs) throws StateException {
		Logger.log("executing " + pattern);
		
		Arg[] args; //subset of allArgs for each state
		String[] argNames;
		
		int n = states.size();
		SimpleObjectProperty<Integer> completions = new SimpleObjectProperty<>(0); 
		SimpleObjectProperty<Boolean> executed = new SimpleObjectProperty<>(false);
		
		for (int s=0; s<n; s++) {
			State<?> state = states.get(s);
			
			//prep args
			argNames = state.getArgNames();
			args = new Arg[argNames.length];
			Arg arg;
			
			for (int a=0; a<args.length; a++) {
				arg = allArgs.get(argNames[a]);
				if (arg == null) {
					arg = new Arg();
					arg.name = argNames[a];
				}
				
				Logger.log("mapping arg " + argNames[a] + " to " + arg);
				
				args[a] = arg;
			}
			
			//execute state transition
			SimpleObjectProperty<Boolean> transitioned = state.transition(args);
			transitioned.addListener(new ChangeListener<Boolean>() {
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (newValue) {
						observable.removeListener(this);
						
						synchronized (completions) {
							int c = completions.get() + 1;
							completions.set(c);
							
							if (c == n) {
								executed.set(true);
							}
						}
					}
				}
			});
			
			if (transitioned.get()) {
				//notify again if too fast
				transitioned.set(false);
				transitioned.set(true);
			}
		}
		
		return executed;
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
		try {
			fromString((String) stream.readObject());
		}
		catch (LanguageMappingException e) {
			throw new ClassNotFoundException(e.getMessage());
		}
		
		int ns = stream.readInt();
		
		states = new ArrayList<>();
		for (int s=0; s<ns; s++) {
			states.add((State<?>) stream.readObject());
		}
		
		Logger.log("deserialized action " + id + ": " + pattern);
	}
}
