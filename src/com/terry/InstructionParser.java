package com.terry;

import java.util.Scanner;

import com.terry.InstructionPossibilities.InstructionPossibility;

public class InstructionParser {	
	private static final char STATE_IDLE = 0;
	private static final char STATE_PARSING = 1;
	private static final char STATE_DONE = 2;
	
	public static CharProperty state = new CharProperty(STATE_IDLE);
		
	public static void init() {		
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
		
		tokens = spellPunctuation(tokens);
		
		InstructionPossibilities possibilities = new InstructionPossibilities();
		
		Scanner scanner = new Scanner(tokens);
		String token;
		
		while (scanner.hasNext()) {
			//get next token
			token = scanner.next();
			
			//handle punctuation
			
			//update instruction possibilities
			if (possibilities.resolve(token)) {
				/*
				 * If possibilities have resolved into one mapping, fill in the rest of the tokens 
				 * and execute the action or learn the lesson.
				 */
				InstructionPossibility instruction = possibilities.finish(scanner);
				
				if (instruction == null) { //instruction did not match mapping
					Logger.logError("no mappings found for given instruction");
				}
				else {
					Compiler.enqueue(instruction);
				}
				
				//look for new instruction
				possibilities = new InstructionPossibilities();
			}
		}
		
		/*
		 * If there are multiple remaining possibilities (or none), pick the best.
		 */
		InstructionPossibility instruction = possibilities.finish();
		
		if (instruction == null) { //there were no remaining possibilities
			Logger.logError("no mappings found for given instruction");
		}
		else { //follow through
			Compiler.enqueue(instruction);
			Compiler.compile();
		}
		
		scanner.close();
		state.set(STATE_DONE);
		state.set(STATE_IDLE);
	}
	
	/*
	 * Replace punctuation symbols with names.
	 * 
	 * Ex: apple, banana. -> apple comma banana period
	 */
	private static String spellPunctuation(String string) {
		char[] in = string.toCharArray();
		String out = "";
		
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
					out += " comma";
					break;
					
				case '.':
					if (d == ' ') { //otherwise, 
						out += " period";
					}
					else { //number with a decimal
						out += c;
					}
					
					break;
					
				case ';':
					out += " semicolon";
					break;
					
				case ':':
					out += " colon";
					break;
					
				case '\u201c':
					out += "begin quote ";
					break;
					
				case '\u201d':
					out += " end quote";
					break;
					
				case '"':
					if (b == ' ') { //previous is space; start string
						out += "begin quote ";
					}
					else if (c == ' ') { //next is space; end string
						out += " end quote";
					}
					else { //mid-word
						out += " double quote ";
					}
					
				case '\'': //single quote
				case '\u2019': //apostrophe
					if (b == ' ') { //previous is space; start string
						out += "begin quote ";
					}
					else if (c == ' ') { //next is space; end string
						out += " end quote";
					}
					else { //mid-word
						//contraction apostrophe is ignored; don't -> dont
					}
					break;
					
				default:
					out += c;
					break;
			}
			
			b = c;
		}
		
		return out;
	}
}
