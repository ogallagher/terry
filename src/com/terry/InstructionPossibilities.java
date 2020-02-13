package com.terry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.terry.LanguageMapping.PatternNode;
import com.terry.State.StateException;

public class InstructionPossibilities {
	private ArrayList<InstructionPossibility> possibilities;
	
	public InstructionPossibilities() {
		possibilities = null;
	}
	
	public boolean resolve(String token) {
		if (possibilities == null) {
			//first word, dictionary lookup
			possibilities = new ArrayList<InstructionPossibility>();
			ArrayList<Memory.Lookup> entries = Memory.dictionaryLookup(token, true, true);
			
			if (entries == null) { //no matching results
				Logger.logError("token " + token + " is not a leader token of any mapping");
			}
			else {
				for (Memory.Lookup entry : entries) {
					for (LanguageMapping lm : entry.mappings) {		
						PatternNode leader = lm.getLeader(entry.token);
						
						if (leader == null) {
							Logger.log(entry.token + " is not a leader of " + lm.id);
						}
						else {
							possibilities.add(new InstructionPossibility(lm, leader, token));
						}
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
	
	/*
	 * Assumes possibilities.size() == 1.
	 * Returns possibility if it fills all expected tokens.
	 * Returns null if unexpected token is encountered or tokens run out.
	 */
	public InstructionPossibility finish(Scanner scanner) {
		InstructionPossibility possibility = possibilities.get(0);
		
		if (possibility.complete()) {
			return possibility;
		}
		else {
			Logger.log("finishing mapping");
			String token;
			
			while (scanner.hasNext()) {
				token = scanner.next();
				
				if (!Memory.isTrivial(token)) {
					if (possibility.resolve(token)) { //possibility remains possible
						 if (possibility.complete()) { //all tokens are mapped
							 return possibility;
						 }
					}
					else {
						Logger.logError("invalid instruction token " + token);
						return null;
					}
				}
				else {
					Logger.log(token + " ignored");
				}
			}
			
			Logger.log("instruction could have taken more tokens");
			return possibility;
		}
	}
	
	public LanguageMapping getMapping() {
		return possibilities.get(0).mapping;
	}
	
	public static class InstructionPossibility {
		private PatternNode node; //node.token cannot be null, because PatternNode.getFollowers() skips null nodes
		private Arg arg;
		private ArrayList<InstructionPossibility> children;
		
		private LanguageMapping mapping;					//root has reference to mapping
		private ArrayList<InstructionPossibility> leaves;	//root has links to leaves
		
		//root constructor
		public InstructionPossibility(LanguageMapping lm, PatternNode leader, String token) {
			Logger.log("adding possibilities for " + lm);
			node = leader;
			
			if (node.getType() == Arg.notarg) {
				arg = null;
			}
			else {
				arg = new Arg();
				arg.name = leader.token;
				arg.value = Arg.getArgValue(leader.getType(), token);
			}
			
			mapping = lm;
			leaves = new ArrayList<InstructionPossibility>();
			
			ArrayList<PatternNode> nodes = new ArrayList<>(); 						//identify cycles
			ArrayList<InstructionPossibility> possibilities = new ArrayList<>();	//close cycles
			
			children = new ArrayList<InstructionPossibility>();
			for (PatternNode follower : node.getFollowers()) {
				int i = nodes.indexOf(follower);
				InstructionPossibility possibility;
				
				if (i == -1) {
					//new node in graph
					Logger.log("added possibility " + mapping.id + "." + follower.token);
					possibility = new InstructionPossibility(follower);
					
					nodes.add(follower);
					possibilities.add(possibility);
					
					children.add(possibility);
					leaves.add(possibility);
					
					possibility.extend(nodes, possibilities);
				}
				else {
					//repeated node in graph; create cycle
					Logger.log("added possibility " + mapping.id + "." + follower.token);
					possibility = possibilities.get(i);
					
					children.add(possibility);
					leaves.add(possibility);
				}
			}
		}
		
		//branch constructor
		public InstructionPossibility(PatternNode pnode) {
			node = pnode;
			arg = null;
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
		 */
		public boolean resolve(String next) {
			Logger.log("resolving " + next);
			boolean resolved = false;
			
			InstructionPossibility leaf = null;
			int i=0;
			int n=leaves.size();
			char argType;
			
			while (i < n) {
				leaf = leaves.get(0);
				argType = leaf.node.getType();
				
				//Logger.log("checking " + next + " vs " + leaf.node.token);
				
				if (argType == Arg.notarg) {
					//is keyword
					int dist = Utilities.editDistance(leaf.node.token, next, leaf.node.token.length()*2/3);
					
					if (dist != -1) {
						resolved = true;
						
						for (InstructionPossibility newLeaf : leaf.children) {
							Logger.log("added leaf " + newLeaf.node.token);
							leaves.add(newLeaf);
						}
					}
					
					leaves.remove(0);
					i++;
				}
				else {
					//is arg
					Arg arg = new Arg();
					arg.name = leaf.node.token;
					arg.value = Arg.getArgValue(argType, next);
					
					if (arg.value != null) {
						leaf.arg = arg;
						resolved = true;
						
						for (InstructionPossibility newLeaf : leaf.children) {
							Logger.log("added leaf " + newLeaf.node.token);
							leaves.add(newLeaf);
						}
					}
					
					//check multiword args
					leaves.remove(0);
					if (leaf.arg.appendToken(argType, next)) {
						//arg could add next token; remains as a leaf; add it back
						leaves.add(leaf);
						resolved = true;
					}
					i++;
				}
			}
			
			return resolved;
		}
		
		/*
		 * One possible version of this mapping is a terminal node.
		 */
		public boolean complete() {
			return leaves.isEmpty() || node.isTerminal();
		}
		
		/*
		 * Only the root possibility should call this method, which follows through
		 * on the mapped lesson or action.
		 */
		public void compile() {
			char mappingType = mapping.getType();
			
			if (mappingType != LanguageMapping.TYPE_UNKNOWN) {
				HashMap<String,Arg> argMap = new HashMap<>();
				
				InstructionPossibility p = this;
				if (p.arg != null) {
					argMap.put(p.arg.name, p.arg);
				}
				
				while (!p.children.isEmpty()) {
					p = p.children.get(0);
					if (p.arg != null) {
						argMap.put(p.arg.name, p.arg);
					}
				}
				
				if (mappingType == LanguageMapping.TYPE_ACTION) {
					try {
						((Action) mapping).execute(argMap);
					}
					catch (StateException e) {
						Logger.logError(e.getMessage());
					}
				}
				else if (mappingType == LanguageMapping.TYPE_LESSON) {
					((Lesson) mapping).learn(argMap);
				}
			}
			else {
				Logger.logError("unknown mapping type");
			}
		}
	}
}
