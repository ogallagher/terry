package com.terry;

import java.util.ArrayList;

import com.terry.InstructionPossibilities.InstructionPossibility;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class Compiler {
	public static ArrayList<InstructionPossibility> executionQueue;
	
	public static void init() {
		executionQueue = new ArrayList<>();
	}
	
	public static void enqueue(InstructionPossibility instruction) {
		if (instruction != null) {
			executionQueue.add(instruction);
		}
		else {
			Logger.logError("compiler cannot enqueue a null instruction");
		}
	}
	
	public static void compile() {
		SimpleObjectProperty<Integer> I = new SimpleObjectProperty<>(0);
		int n = executionQueue.size();
		
		if (n != 0) {			
			ChangeListener<Boolean> sequencer = new ChangeListener<Boolean>() {
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (newValue) {
						observable.removeListener(this);
						
						synchronized (I) {
							int i = I.get();
							
							if (i < n) {
								SimpleObjectProperty<Boolean> compiled = executionQueue.get(i).compile();
								compiled.addListener(this);
								I.set(i + 1);
								
								//notify again if too fast
								if (compiled.get()) {
									compiled.set(false);
									compiled.set(true);
								}
							}
							else {
								executionQueue.clear();
							}
						}
					}
				}
			};
			
			synchronized (I) {
				int i = I.get();
				
				SimpleObjectProperty<Boolean> compiled = executionQueue.get(i).compile();
				compiled.addListener(sequencer);
				I.set(i + 1);
				
				//notify again if too fast (in the case of lessons, for example
				if (compiled.get()) {
					compiled.set(false);
					compiled.set(true);
				}
			}
		}
	}
}
