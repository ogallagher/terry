package com.terry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
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
			ArrayList<Memory.Lookup> entries = Memory.dictionaryLookup(token);
			
			if (entries == null) { //no matching results
				Logger.logError(token + " not recognized; skipping token");
			}
			else {
				for (Memory.Lookup entry : entries) {
					for (LanguageMapping lm : entry.mappings) {		
						PatternNode leader = lm.getLeader(entry.token);
						
						if (leader == null) {
							Logger.log(entry.token + " is not a leader of " + lm.id);
						}
						else {
							possibilities.add(new InstructionPossibility(lm, leader));
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
			while (scanner.hasNext()) {
				if (possibility.resolve(scanner.next())) { //possibility remains possible
					 if (possibility.complete()) { //all tokens are mapped
						 return possibility;
					 }
				}
				else {
					return null;
				}
			}
			
			return null;
		}
	}
	
	public LanguageMapping getMapping() {
		return possibilities.get(0).mapping;
	}
	
	public static class InstructionPossibility {
		private PatternNode node;
		private Arg arg;
		private ArrayList<InstructionPossibility> children;
		
		private LanguageMapping mapping;					//root has reference to mapping
		private ArrayList<InstructionPossibility> leaves;	//root has links to leaves
		
		//root constructor
		public InstructionPossibility(LanguageMapping lm, PatternNode leader) {
			Logger.log("adding possibilities for " + lm);
			node = leader;
			arg = null;
			
			mapping = lm;
			leaves = new ArrayList<InstructionPossibility>();
			
			ArrayList<PatternNode> nodes = new ArrayList<>(); 						//identify cycles
			ArrayList<InstructionPossibility> possibilities = new ArrayList<>();	//close cycles
			
			children = new ArrayList<InstructionPossibility>();
			for (PatternNode follower : node.getFollowers()) {
				if (!nodes.contains(follower)) {
					Logger.log("added possibility " + mapping.id + "." + follower.token);
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
		 * 
		 * TODO handle edit distance in LanguageMapping.getFollowers()? Would be an extra challenge.
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
				
				Logger.log("checking " + next + " vs " + leaf.node.token);
				
				if (argType == PatternNode.notarg) {
					//is keyword
					if (leaf.node.token.equals(next)) {
						resolved = true;
						
						for (InstructionPossibility newLeaf : leaf.children) {
							leaves.add(newLeaf);
						}
					}
					
					leaves.remove(0);
					i++;
				}
				else {
					//is arg
					arg = new Arg();
					arg.name = leaf.node.token;
					boolean argValid = true;
					
					switch (argType) {
						case PatternNode.colarg:
							arg.value = Arg.getColor(next);
							if (arg.value == null) {
								Logger.logError("invalid color " + next);
								argValid = false;
							}
							break;
							
						case PatternNode.dirarg:
							arg.value = Arg.getDirection(next);
							if (arg.value == null) {
								Logger.logError("invalid direction " + next);
								argValid = false;
							}
							break;
							
						case PatternNode.numarg:
							arg.value = Arg.getNumeric(next);
							if (arg.value == null) {
								Logger.logError("invalid number " + next);
								argValid = false;
							}
							break;
							
						case PatternNode.spdarg:
							arg.value = Arg.getSpeed(next);
							if (arg.value == null) {
								Logger.logError("invalid speed " + next);
								argValid = false;
							}
							break;
							
						case PatternNode.strarg: //TODO handle multitoken strings
							arg.value = next;
							argValid = false;
							break;
							
						case PatternNode.wigarg:
							Logger.log("widgets not supported yet");
							argValid = false;
							break;
							
						default:
							Logger.logError("unknown arg type " + argType);
							argValid = false;
							break;
					}
					
					if (argValid) {
						resolved = true;
						
						for (InstructionPossibility newLeaf : leaf.children) {
							leaves.add(newLeaf);
						}
					}
					
					leaves.remove(0);
					i++;
				}
			}
			
			return resolved;
		}
		
		//the only possible version of this mapping has no more tokens expected
		public boolean complete() {
			return (leaves.size() == 1 && leaves.get(0).children.isEmpty());
		}
		
		/*
		 * Only the root possibility should call this method, which follows through
		 * on the mapped lesson or action.
		 */
		public void compile() {
			switch (mapping.getType()) {
				case LanguageMapping.TYPE_ACTION:
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
					
					try {
						((Action) mapping).execute(argMap);
					} 
					catch (StateException e) {
						Logger.logError(e.getMessage());
					}
					
					break;
					
				case LanguageMapping.TYPE_LESSON:
					Logger.log("lessons are not supported yet");
					break;
					
				case LanguageMapping.TYPE_UNKNOWN:
				default:
					Logger.logError("unknown mapping type");
					break;
			}
		}
	}
}
