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
	
	public boolean resolve(String token) {
		if (possibilities == null) {
			//first word, dictionary lookup
			possibilities = new ArrayList<InstructionPossibility>();
			ArrayList<Memory.Lookup> entries = Memory.dictionaryLookup(token);
			
			if (entries == null) { //no matching results
				Logger.logError(token + " not recognized; skipping token");
			}
			else {
				for (Memory.Lookup entry : entries) {
					for (LanguageMapping lm : entry.mappings) {				
						possibilities.add(new InstructionPossibility(lm));
					}					
				}
			}
		}
		else {
			//subsequent words, check against followers
			int n = possibilities.size();
			for (int i=0; i<n; i++) {
				if (!possibilities.get(i).resolve(token)) { //possibility no longer possible
					possibilities.remove(i);
					i--;
					n--;
				}
			}
		}
		
		return (possibilities != null && possibilities.size() == 1);
	}
	
	public LanguageMapping getMapping() {
		return possibilities.get(0).mapping;
	}
	
	private static class InstructionPossibility {
		private PatternNode node;
		private ArrayList<InstructionPossibility> children;
		
		private LanguageMapping mapping;					//root has reference to mapping
		private ArrayList<InstructionPossibility> leaves;	//root has links to leaves
		
		//root constructor
		public InstructionPossibility(LanguageMapping lm) {
			node = lm.getLeader();
			
			mapping = lm;
			leaves = new ArrayList<InstructionPossibility>();
			
			ArrayList<PatternNode> nodes = new ArrayList<>(); 						//identify cycles
			ArrayList<InstructionPossibility> possibilities = new ArrayList<>();	//close cycles
			
			children = new ArrayList<InstructionPossibility>();
			for (PatternNode follower : node.getFollowers()) {
				if (!nodes.contains(follower)) {
					Logger.log("added possibility " + mapping);
					InstructionPossibility possibility = new InstructionPossibility(follower);
					
					nodes.add(follower);
					possibilities.add(possibility);
					
					children.add(possibility);
					leaves.add(possibility);
					
					possibility.extend(nodes, possibilities);
				}
			}
		}
		
		//branch constructor
		public InstructionPossibility(PatternNode pnode) {
			node = pnode;
			children = new ArrayList<InstructionPossibility>();
		}
		
		public void extend(ArrayList<PatternNode> nodes, ArrayList<InstructionPossibility> possibilities) {
			for (PatternNode follower : node.getFollowers()) {
				int i = nodes.indexOf(follower);
				
				if (i == -1) {
					//extend tree
					InstructionPossibility possibility = new InstructionPossibility(follower);
					
					nodes.add(follower);
					possibilities.add(possibility);
					
					children.add(possibility);
					
					possibility.extend(nodes, possibilities);
				}
				else {
					//create cycle
					children.add(possibilities.get(i));
				}
			}
		}
		
		public char getType() {
			return node.getType();
		}
		
		/*
		 * Return true if this possibility can still accept the next token.
		 * Else, return false.
		 * 
		 * TODO handle edit distance in LanguageMapping.getFollowers()? Would be an extra challenge.
		 */
		public boolean resolve(String next) {
			boolean resolved = false;
			
			InstructionPossibility leaf = null;
			int i=0;
			int n=leaves.size();
			while (i < n) {
				leaf = leaves.get(0);
				
				if (leaf.node.token.equals(next)) {
					resolved = true;
					
					for (InstructionPossibility newLeaf : leaf.children) {
						leaves.add(newLeaf);
					}
				}
				
				leaves.remove(0);
				i++;
			}
			
			return resolved;
		}
	}
}
