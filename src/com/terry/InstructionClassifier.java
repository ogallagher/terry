package com.terry;

import java.util.LinkedList;
import java.util.Scanner;

public class InstructionClassifier {	
	private static final char STATE_IDLE = 0;
	private static final char STATE_PARSING = 1;
	private static final char STATE_DONE = 2;
	
	public static CharProperty state = new CharProperty(STATE_IDLE);
	
	public static void init() {
		Logger.log("instruction classifier init success");
	}
	
	public static void parse(String tokens) {
		/*
		 * This represents a tree of possible expected tokens, given the ones parsed and matched with
		 * mappings so far. Each mapping can expect multiple possible tokens, and each successive token
		 * can satisfy multiple mappings.
		 * 
		 * Initially, a possibilities instance expects all possible mappings' first tokens. As tokens are
		 * appended to the instruction graph, branches are collapsed and possibilities narrow until only one
		 * possible mapping remains.  
		 */
		InstructionPossibilities possibilities = new InstructionPossibilities();
		
		Scanner scanner = new Scanner(tokens);
		String token;
		
		while (scanner.hasNext()) {
			//get next token
			token = scanner.next();
			
			//ignore if trivial
			if (!Memory.isTrivial(token)) {
				Logger.log(token + " kept");
				//handle punctuation
				
				
				//update instruction possibilities
				possibilities.resolve(token);
			}
			else {
				Logger.log(token + " ignored");
			}
		}
		
		scanner.close();
	}
}
