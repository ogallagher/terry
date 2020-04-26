package com.terry;

import java.util.ArrayList;

import com.terry.InstructionPossibilities.InstructionPossibility;

import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class Compiler {
	public static ArrayList<InstructionPossibility> executionQueue;
	
	public static final char STATE_IDLE = 0;		//not compiling, may be queuing new instructions
	public static final char STATE_COMPILING = 1;	//compile all instructions currently on the queue
	public static final char STATE_BUSY = 2;		//more instructions requested to compile during current compilation
	
	public static char state;
	
	public static void init() {
		state = STATE_IDLE;
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
		if (state == STATE_IDLE) {
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
									Logger.log("finished instruction " + (i-1), Logger.LEVEL_FILE);
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
									for (i=0; i<n; i++) {
										//clear compiled instructions from queue
										executionQueue.remove(0);
									}
									
									if (state == STATE_BUSY) {
										//handle postponed compilation
										state = STATE_IDLE;
										compile();
									}
									else {
										//no new compilations requested; return to idle
										state = STATE_IDLE;
									}
								}
							}
						}
					}
				};
				
				SimpleObjectProperty<Boolean> compiled = null;
				synchronized (I) {
					int i = I.get();
					
					compiled = executionQueue.get(i).compile();
					compiled.addListener(sequencer);
					I.set(i + 1);
				}
					
				//notify again if too fast (in the case of lessons, for example
				if (compiled != null && compiled.get()) {
					compiled.set(false);
					compiled.set(true);
				}
			}
		}
		else if (state == STATE_COMPILING) {
			state = STATE_BUSY;
		}
	}
}
