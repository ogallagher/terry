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
				
				/*
				 * Dictionary lookup. 
				 * 
				 * If not found in dictionary, it's a typo, an argument, or an unknown word. To distinguish between
				 * each, the lookup should return a probability between 0 and 1 based on edit distance between the token
				 * and the dict entry. 
				 * 
				 * If the best probability is greater than MIN_MATCH, it's a typo. 
				 * 
				 * Else, if the token could be an argument based on the current possibilities, it's an argument. 
				 * 
				 * Otherwise it's an unknown word. Ideally, the prompter would then ask for a synonym definition, but the 
				 * minimal prototype will just have to fail and log the unknown word.
				 * 
				 */
				
				//update instruction possibilities
				
			}
			else {
				Logger.log(token + " ignored");
			}
		}
		
		scanner.close();
	}
}
