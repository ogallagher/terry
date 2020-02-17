package com.terry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import com.terry.LanguageMapping.PatternNode;
import com.terry.State.StateException;

import javafx.application.Platform;

public class InstructionPossibilities {
	private ArrayList<InstructionPossibility> possibilities;
	boolean trivialLeader;
	
	public InstructionPossibilities() {
		possibilities = null;
		trivialLeader = false;
	}
	
	public boolean resolve(String token) {
		if (possibilities == null) {
			//first word, dictionary lookup
			possibilities = new ArrayList<InstructionPossibility>();
			ArrayList<Memory.Lookup> entries = Memory.dictionaryLookup(token, true, true);
			
			trivialLeader = Memory.isTrivial(token);
			
			if (entries == null) { //no matching results, which shouldn't really be possible
				Logger.logError("token " + token + " is not a leader token of any mapping");
			}
			else {
				for (Memory.Lookup entry : entries) {
					for (LanguageMapping lm : entry.mappings) {		
						for (PatternNode leader : lm.getLeaders(entry.token)) {
							InstructionPossibility p = new InstructionPossibility(lm, leader, token);
							if (leader.getType() != Arg.notarg) {
								//itself is a leaf if it can be a multiword arg
								p.leaves.add(p);
							}
							
							possibilities.add(p);
						}
					}					
				}
			}
		}
		else {
			//subsequent words, check against followers
			int n = possibilities.size();
			for (int i=0; i<n; i++) {
				InstructionPossibility p = possibilities.get(i);
				
				if (!p.resolve(token) && p.complete() == null) { //possibility no longer possible
					Logger.log("eliminated possibility " + p.mapping);
					possibilities.remove(i);
					i--;
					n--;
				}
			}
			
			if (trivialLeader && !Memory.isTrivial(token)) { //keyword and arg leader mappings can now all attempt leader resolution
				trivialLeader = false;
				ArrayList<Memory.Lookup> entries = Memory.dictionaryLookup(token, true, true);
				
				for (Memory.Lookup entry : entries) {
					for (LanguageMapping lm : entry.mappings) {
						for (PatternNode leader : lm.getLeaders(entry.token)) {
							InstructionPossibility p = new InstructionPossibility(lm, leader, token);
							if (leader.getType() != Arg.notarg) {
								//itself is a leaf if it can be a multiword arg
								p.leaves.add(p);
							}
							
							possibilities.add(p);
						}
					}
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
		
		if (possibility.complete() != null) {
			return possibility;
		}
		else {
			Logger.log("finishing mapping");
			String token;
			
			while (scanner.hasNext()) {
				token = scanner.next();
				
				if (possibility.resolve(token)) { //possibility remains possible
					 if (possibility.complete() != null) { //all tokens are mapped
						 return possibility;
					 }
				}
				else {
					Logger.logError("invalid instruction token " + token);
					return null;
				}
			}
			
			Logger.log("instruction needed more tokens");
			return possibility;
		}
	}
	
	/*
	 * When there are still multiple possible mappings given the instruction, rank based on number of tokens
	 * in the mapping's pattern and pick the best.
	 */
	public InstructionPossibility finish() {
		if (possibilities.isEmpty()) {
			return null;
		}
		else {
			InstructionPossibility best = possibilities.get(0);
			int bestCount = best.tokenCount();
			if (best.complete() == null) {
				System.out.println("candidate mapping not complete: " + best.mapping);
				
				bestCount = Integer.MAX_VALUE;
				best = null;
			}
			
			InstructionPossibility next;
			int nextCount = bestCount;
			
			for (int i=1; i<possibilities.size(); i++) {
				next = possibilities.get(i);
				
				if (next.complete() != null) {
					nextCount = next.tokenCount();
					
					if (nextCount < bestCount) {
						System.out.println("candidate mapping not complete: " + next.mapping);
						best = next;
						bestCount = nextCount;
					}
				}
			}
			
			return best;
		}
	}
	
	public LanguageMapping getMapping() {
		return possibilities.get(0).mapping;
	}
	
	public static class InstructionPossibility {
		private PatternNode node; //node.token cannot be null, because PatternNode.getFollowers() skips null nodes
		private Arg arg;
		private ArrayList<InstructionPossibility> children;
		private ArrayList<InstructionPossibility> parents;
		
		private LanguageMapping mapping;					//root has reference to mapping
		private ArrayList<InstructionPossibility> leaves;	//root has links to leaves
		private ArrayList<Integer> completion;
		
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
				arg.setValue(Arg.getArgValue(leader.getType(),token), token);
			}
			
			mapping = lm;
			leaves = new ArrayList<InstructionPossibility>();
			completion = new ArrayList<>();
			
			ArrayList<PatternNode> nodes = new ArrayList<>(); 						//identify cycles
			ArrayList<InstructionPossibility> possibilities = new ArrayList<>();	//close cycles
			
			children = new ArrayList<InstructionPossibility>();
			for (PatternNode follower : node.getFollowers()) {
				int i = nodes.indexOf(follower);
				InstructionPossibility possibility;
				
				if (i == -1) {
					//new node in graph
					Logger.log("added possibility " + mapping.id + "." + follower.token);
					possibility = new InstructionPossibility(follower,this);
					
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
					possibility.parents.add(this);
					leaves.add(possibility);
				}
			}
			parents = new ArrayList<InstructionPossibility>();
		}
		
		//branch constructor
		public InstructionPossibility(PatternNode pnode, InstructionPossibility parent) {
			node = pnode;
			arg = null;
			children = new ArrayList<InstructionPossibility>();
			parents = new ArrayList<InstructionPossibility>();
			parents.add(parent);
		}
		
		/*
		 * clone constructor for branches
		 * Note that arg is initialized to null.
		 */
		public InstructionPossibility(InstructionPossibility clone, ArrayList<InstructionPossibility> leaves) {
			if (leaves.contains(clone)) {
				leaves.add(this);
			}
			
			node = clone.node;
			
			if (clone.arg == null) {
				arg = null;
			}
			else {
				arg = new Arg(clone.arg, clone.getType());
			}
			
			parents = new ArrayList<InstructionPossibility>();
			parents.addAll(clone.parents);
			
			children = new ArrayList<InstructionPossibility>();
			InstructionPossibility child;
			for (InstructionPossibility childClone : clone.children) {
				child = new InstructionPossibility(childClone,leaves);
				
				child.parents.remove(clone);
				child.parents.add(this);
				
				children.add(child);
			}
		}
		
		public void extend(ArrayList<PatternNode> nodes, ArrayList<InstructionPossibility> possibilities) {
			for (PatternNode follower : node.getFollowers()) {
				int i = nodes.indexOf(follower);
				
				if (i == -1) {
					//extend tree
					InstructionPossibility possibility = new InstructionPossibility(follower,this);
					
					nodes.add(follower);
					possibilities.add(possibility);
					
					children.add(possibility);
					
					possibility.extend(nodes, possibilities);
				}
				else {
					//create cycle
					InstructionPossibility child = possibilities.get(i);
					children.add(child);
					child.parents.add(this);
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
		 * TODO handle multiple possible token assignments for the same mapping, arg values getting overwritten 
		 */
		public boolean resolve(String next) {
			Logger.log("resolving " + next);
			boolean resolved = false;
			boolean trivial = Memory.isTrivial(next);
			
			InstructionPossibility leaf = null;
			int i=0;
			int n=leaves.size();
			char argType;
			
			while (i < n) {
				leaf = leaves.get(0);
				argType = leaf.node.getType();
				
				if (argType == Arg.notarg) {
					Logger.log("checking " + next + " against keyword " + leaf.node.token);
					//is keyword
					if (!trivial) {
						int dist = Utilities.editDistance(leaf.node.token, next, leaf.node.token.length()*2/3);
						
						if (dist != -1) {
							resolved = true;
							
							for (InstructionPossibility newLeaf : leaf.children) {
								Logger.log("added leaf " + newLeaf.node.token);
								leaves.add(newLeaf);
							}
						}
					}
					else {
						//leaf was skipped; add it back before removing
						leaves.add(leaf);
					}
				}
				else {
					//is arg
					if (leaf.arg == null) { //first word of arg
						Arg arg = new Arg();
						arg.name = leaf.node.token;
						arg.setValue(Arg.getArgValue(argType, next), next);
						
						if (arg.getValue() != null) {
							Logger.log("set arg " + arg.name + " to " + arg.getValue());
							leaf.arg = arg;
							resolved = true;
							
							for (InstructionPossibility newLeaf : leaf.children) {
								Logger.log("added leaf " + newLeaf.node.token);
								leaves.add(newLeaf);
							}
						}
						
						leaves.add(leaf);
					}
					else { //multiword arg
						//there now exist multiple possible values of this arg, including or not including next token
						InstructionPossibility cloneLeaf = new InstructionPossibility(leaf, leaves);
						
						//arg could add next token; remains as a leaf; add it back
						if (cloneLeaf.arg.appendToken(argType, next)) {
							Logger.log("appended " + next + " to arg " + cloneLeaf.node.token + ": " + cloneLeaf.arg.getText());
							resolved = true;
							
							for (InstructionPossibility parent : cloneLeaf.parents) {
								parent.children.add(cloneLeaf);
							}
							
							//cloneLeaf adds itself to leaves in constructor
						}
					}
				}
				
				leaves.remove(0);
				i++;
			}
			
			return resolved;
		}
		
		/*
		 * One possible version of this mapping is a terminal node, and that terminal node has no followers
		 * that are resolved (in leaves).
		 * If a completion exists, the return value is a list of which edge to follow at each node in
		 * the graph.
		 */
		public ArrayList<Integer> complete() {
			if (!completion.isEmpty() || children.isEmpty()) {
				return completion;
			}
			else {
				ArrayList<Integer> subcompletion;
				for (int i=0; i<children.size(); i++) {
					subcompletion = children.get(i).complete(i,leaves);
					
					if (subcompletion.size() > completion.size()) {
						completion = subcompletion;
					}
				}
				
				if (completion.isEmpty()) {
					return null;
				}
				else {
					String log = "completed " + mapping + ":\n";
					for (Integer i : completion) {
						log += " " + i;
					}
					Logger.log(log);
					
					return completion;
				}
			}
		}
		
		private ArrayList<Integer> complete(int i, ArrayList<InstructionPossibility> leaves) {
			ArrayList<Integer> completion = new ArrayList<Integer>();
			
			if (node.isTerminal()) { //can end expression
				if (node.getType() == Arg.notarg) { //keyword
					if (!leaves.contains(this)) { //nonleaf terminal keyword = complete
						completion.add(i);
						//Logger.log(node.token + " is a resolved keyword");
					}
				}
				else {
					if (arg != null) { //resolved terminal arg = complete.
						completion.add(i);
						//Logger.log(node.token + "=" + arg.getValue() + " is a resolved arg");
					}
				}
			}
			
			ArrayList<Integer> subcompletion;
			for (int j=0; j<children.size(); j++) {
				subcompletion = children.get(j).complete(j, leaves);
				subcompletion.add(0, i);
				
				if (subcompletion.size() != 1 && subcompletion.size() > completion.size()) {
					completion = subcompletion;
				}
			}
			
			return completion;
		}
		
		public int tokenCount() {
			return node.tokenCount();
		}
		
		/*
		 * Only the root possibility should call this method, which follows through
		 * on the mapped lesson or action.
		 */
		public void compile() {
			System.out.println(diagram());
			
			char mappingType = mapping.getType();
			
			if (mappingType != LanguageMapping.TYPE_UNKNOWN) {
				HashMap<String,Arg> argMap = new HashMap<>();
				
				InstructionPossibility p = this;
				if (p.arg != null) {
					argMap.put(p.arg.name, p.arg);
				}
				
				StringBuilder instructionString = new StringBuilder();	
				
				for (int i=0; i<completion.size() && !p.children.isEmpty(); i++) {
					if (p.getType() == Arg.notarg) {
						instructionString.append(p.node.token + " ");
					}
					else {
						instructionString.append(p.node.token + "=" + p.arg.getValue() + " ");
					}
					
					p = p.children.get(completion.get(i));
					
					if (p.arg != null) {
						argMap.put(p.arg.name, p.arg);
					}
				}
				
				if (p.getType() == Arg.notarg) {
					instructionString.append(p.node.token);
				}
				else {
					instructionString.append(p.node.token + "=" + p.arg.getValue());
				}
				Logger.log("compiled instruction: " + instructionString.toString());
				
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
		
		public String diagram() {
			StringBuilder string = new StringBuilder(); 
			string.append(node.token + "\n");
			
			ArrayList<PatternNode> nodes = new ArrayList<>();
			for (InstructionPossibility child : children) {				
				if (!leaves.contains(child)) {
					child.diagram(leaves, nodes, "   ", string);
				}
				else {
					string.append("   " + child.node.token);
					
					if (child.node.isTerminal()) {
						string.append(">>--x\n");
					}
					else {
						string.append(">>\n");
					}
				}
			}
			
			return string.toString();
		}
		
		private void diagram(ArrayList<InstructionPossibility> leaves, ArrayList<PatternNode> nodes, String prefix, StringBuilder string) {			
			if (!nodes.contains(node)) {
				string.append(prefix);
				if (arg != null) {
					string.append("[" + arg.getValue() + "]");
				}
				else {
					string.append(node.token);
				}
				if (node.isTerminal()) {
					string.append("--x\n");
				}
				else {
					string.append("\n");
				}
				
				for (InstructionPossibility child : children) {
					if (!leaves.contains(child) || child.arg != null) {
						child.diagram(leaves, nodes, prefix + "   ", string);
					}
					else {
						string.append(prefix + "   " + child.node.token);
						
						if (child.node.isTerminal()) {
							string.append(">>--x\n");
						}
						else {
							string.append(">>\n");
						}
					}
				}
			}
		}
	}
}
