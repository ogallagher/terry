package com.terry;

import java.util.LinkedList;
import java.util.Scanner;

public class InstructionClassifier {
	private static InstructionPossibilities possibilities;
	
	private static final char STATE_IDLE = 0;
	private static final char STATE_PARSING = 1;
	private static final char STATE_DONE = 2;
	
	public static CharProperty state = new CharProperty(STATE_IDLE);
	
	public static void init() {
		Logger.log("instruction classifier init success");
	}
	
	public static void parse(String tokens) {
		Scanner scanner = new Scanner(tokens);
		String token;
		
		while (scanner.hasNext()) {
			//get next token
			token = scanner.next();
			
			//ignore if trivial
			
			//handle punctuation
			
			//dictionary lookup
			
			//
		}
	}
}
