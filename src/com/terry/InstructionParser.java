package com.terry;

import java.util.Scanner;

import com.terry.InstructionPossibilities.InstructionPossibility;

public class InstructionParser {	
	private static final char STATE_IDLE = 0;
	private static final char STATE_PARSING = 1;
	private static final char STATE_DONE = 2;
	
	public static CharProperty state = new CharProperty(STATE_IDLE);
	
	public static void init() {		
		InstructionPossibilities.init();
		Logger.log("instruction parser init success");
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
		state.set(STATE_PARSING);
		
		tokens = detectAliases(spellPunctuation(tokens));
		
		InstructionPossibilities possibilities = new InstructionPossibilities();
		
		Scanner scanner = new Scanner(tokens);
		String token;
		InstructionPossibility instruction = null;
		boolean unresolved = true;
		boolean unknownAction = false;
		
		while (scanner.hasNext()) {
			unresolved = true;
			
			//get next token
			token = scanner.next();
			
			//update instruction possibilities
			if (possibilities.resolve(token)) {
				/*
				 * If possibilities have resolved into one mapping, fill in the rest of the tokens 
				 * and execute the action or learn the lesson.
				 */
				instruction = possibilities.finish(scanner);
				
				if (instruction == null) { //instruction did not match mapping
					Logger.logError("mapping candidate for given instruction failed to resolve");
					unresolved = false;
					unknownAction = true;
				}
				else {
					unresolved = false;
					Compiler.enqueue(instruction);
				}
			}
		}
		
		/*
		 * If there are multiple remaining possibilities (or none), pick the best.
		 */
		if (unresolved) {
			instruction = possibilities.finish(true);
			
			if (instruction == null) { //there were no remaining possibilities
				unknownAction = true;
				Logger.logError("no mappings matched given instruction");
			}
			else {				
				Compiler.enqueue(instruction);
			}
		}
		
		scanner.close();
		if (unknownAction) {
			//notify unknown action
			Logger.log("perhaps \"" + tokens + "\" contains actions I've not learned yet?", Logger.LEVEL_SPEECH);
		}
		else {
			//follow through
			Compiler.compile();
		}
		
		
		state.set(STATE_DONE);
		state.set(STATE_IDLE);
	}
	
	/*
	 * Replace punctuation symbols with names.
	 * 
	 * Ex: apple, banana. -> apple comma banana period
	 * 
	 * TODO probably remove this method
	 */
	private static String spellPunctuation(String string) {
		char[] in = string.toCharArray();
		StringBuilder out = new StringBuilder();
		
		char b = 0; //previous
		char c = 0; //current
		char d = 0; //next
		int n = in.length;
		
		for (int i=0; i<n; i++) {
			c = in[i];
			
			if (i+1 < n) {
				d = in[i+1];
			}
			
			switch (c) {
				case ',':
					if (d != ' ') {
						out.append(" comma ");
					}
					else {
						out.append(" comma");
					}
					break;
					
				case '.':
					if (d == ' ') { //skip periods at the end of sentences
						//skip
					}
					else { //keep number with a decimal intact
						out.append(c);
					}
					
					break;
					
				case ';':
					out.append(" semicolon");
					break;
					
				case ':':
					out.append(" colon");
					break;
					
				case '\u201c':
					out.append("begin quote ");
					break;
					
				case '\u201d':
					out.append(" end quote");
					break;
					
				case '"':
					if (b == ' ') { //previous is space; start string
						out.append("begin quote ");
					}
					else if (c == ' ') { //next is space; end string
						out.append(" end quote");
					}
					else { //mid-word
						out.append(" double quote ");
					}
					break;
					
				case '\'': //single quote
				case '\u2019': //apostrophe
					if (b == ' ') { //previous is space; start string
						out.append("begin quote ");
					}
					else if (c == ' ') { //next is space; end string
						out.append(" end quote");
					}
					else { //mid-word
						//contraction apostrophe is ignored; don't -> dont
					}
					break;
					
				case '%':
					out.append(" percent");
					break;
					
				default:
					out.append(c);
					break;
			}
			
			b = c;
		}
		
		return out.toString();
	}
	
	//TODO there's a lot left to do here
	private static String detectAliases(String string) {
		return string
				.replaceAll("\\W?enter\\W?", "#ret)")
				.replaceAll("\\W?tab\\W?", "#tab)")
				.replaceAll("\\W?backspace\\W?", "#bck)")
				.replaceAll("\\W?delete\\W?", "#del)")
				.replaceAll("\\W?command\\W?", "#cmd)");
	}
}
