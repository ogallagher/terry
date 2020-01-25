package com.terry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import com.terry.LanguageMapping.PatternNode;

public class InstructionPossibilities {
	private ArrayList<InstructionPossibility> possibilities;
	
	public InstructionPossibilities() {
		possibilities = null;
	}
	
	public void resolve(String token) {
		if (possibilities == null) {
			//first word, dictionary lookup
			ArrayList<Memory.Lookup> entries = Memory.dictionaryLookup(token);
			
			if (entries == null) { //no matching results
				Logger.logError(token + " not recognized; skipping token");
			}
			else {
				for (Memory.Lookup entry : entries) {
					for (LanguageMapping lm : entry.mappings) {				
						possibilities.add(new InstructionPossibility(entry.token, lm));
					}					
				}
			}
		}
		else {
			//subsequent words, check against followers
			for (InstructionPossibility possibility : possibilities) {
				if (possibility.resolve(token)) {
					//possibility is still valid
				}
				else {
					//possibility is no longer valid; remove it; etc
				}
			}
		}
	}
	
	private static class InstructionPossibility {
		private String token;
		private char type;
		private ArrayList<InstructionPossibility> children;
		private LanguageMapping mapping;
		
		//root constructor
		public InstructionPossibility(String tok, LanguageMapping lm) {
			token = tok;
			type = lm.getLeader().getType();
			mapping = lm;
			
			children = new ArrayList<InstructionPossibility>();
			for (PatternNode follower : mapping.getFollowers(token)) {
				children.add(new InstructionPossibility(follower,mapping));
			}
		}
		
		//branch constructor
		public InstructionPossibility(PatternNode pnode, LanguageMapping lm) {
			token = pnode.token;
			type = pnode.getType();
			children = null;
			mapping = lm;
		}
		
		/*
		 * Return true if this possibility can still accept the next token.
		 * Else, return false.
		 * 
		 * TODO handle edit distance in LanguageMapping.getFollowers()? Would be an extra challenge.
		 */
		public boolean resolve(String next) {
			children = new ArrayList<InstructionPossibility>();
			
			boolean resolved = false;
			for (PatternNode pnode : mapping.getFollowers(next)) {
				if (pnode.token.equals(next)) {
					resolved = true;
					children.add(new InstructionPossibility(pnode,mapping));
				}
			}
			
			return resolved;
		}
	}
}
