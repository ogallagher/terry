package com.terry;

import java.util.ArrayList;

import com.terry.InstructionPossibilities.InstructionPossibility;

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
		for (InstructionPossibility instruction : executionQueue) {
			instruction.compile();
		}
		executionQueue.clear();
	}
}
