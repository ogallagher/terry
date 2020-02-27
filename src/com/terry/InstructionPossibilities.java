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
			return null;
		}
	}
	
	/*
	 * When there are still multiple possible mappings given the instruction, rank based on number of arguments
	 * in the mapping's pattern and pick the best.
	 */
	public InstructionPossibility finish() {
		if (possibilities == null || possibilities.isEmpty()) {
			return null;
		}
		else {
			InstructionPossibility best = possibilities.get(0);
			int[] bestCount = {0,0};
			
			if (best.complete() == null) {
				bestCount[0] = 0;
				bestCount[1] = 0;
				best = null;
			}
			else {
				bestCount = best.argCount();
				System.out.println("candidate mapping (" + bestCount[0] + "," + bestCount[1] + "): " + best.mapping);
			}
			
			InstructionPossibility next;
			int[] nextCount = new int[2];
			
			for (int i=1; i<possibilities.size(); i++) {
				nextCount[0] = 0;
				nextCount[1] = 0;
				next = possibilities.get(i);
				
				if (next.complete() != null) {
					nextCount = next.argCount();
					
					if ( nextCount[0] > bestCount[0] || 
						(nextCount[0] == bestCount[0] && nextCount[1] > bestCount[1]) || 
						(bestCount[0] == 0 && bestCount[1] == 0)) {
						best = next;
						bestCount[0] = nextCount[0];
						bestCount[1] = nextCount[1];
						System.out.println("candidate mapping (" + bestCount[0] + "," + bestCount[1] + "): " + best.mapping);
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
		/*
		 * For branches node.token cannot be null, because PatternNode.getFollowers() skips null nodes; root
		 * possibilities have null root nodes.
		 */
		private PatternNode node;
		private Arg arg;
		private boolean resolved;
		private ArrayList<InstructionPossibility> children;
		private ArrayList<InstructionPossibility> parents;
		
		//root has a null node
		private LanguageMapping mapping;					//root has reference to mapping
		private ArrayList<InstructionPossibility> leaves;	//root has links to leaves
		private ArrayList<Integer> completion;
		private int[] argCount;
		
		//root constructor
		public InstructionPossibility(LanguageMapping lm, PatternNode leader, String token) {
			Logger.log("adding possibilities for " + lm);
			node = null;
			resolved = true;
			
			mapping = lm;
			leaves = new ArrayList<InstructionPossibility>();
			completion = new ArrayList<>();
			argCount = new int[] {0,0};
			
			ArrayList<PatternNode> nodes = new ArrayList<>(); 						//identify cycles
			ArrayList<InstructionPossibility> possibilities = new ArrayList<>();	//close cycles
			
			children = new ArrayList<InstructionPossibility>();
			
			InstructionPossibility first = new InstructionPossibility(leader, this);
			if (leader.getType() == Arg.notarg) {
				first.arg = null;
			}
			else {
				first.arg = new Arg();
				first.arg.name = leader.token;
				first.arg.setValue(Arg.getArgValue(leader.getType(),token), token);
				
				leaves.add(first);
			}
			first.resolved = true;
			children.add(first);
			
			for (PatternNode follower : leader.getFollowers()) {
				//new node in graph
				InstructionPossibility possibility = new InstructionPossibility(follower,first);
				Logger.log("added new possibility " + mapping.id + "." + follower.token);
				
				nodes.add(follower);
				possibilities.add(possibility);
				
				first.children.add(possibility);
				leaves.add(possibility);
				possibility.extend(nodes, possibilities);
			}
			
			parents = new ArrayList<InstructionPossibility>();
		}
		
		//branch constructor
		public InstructionPossibility(PatternNode pnode, InstructionPossibility parent) {
			node = pnode;
			arg = null;
			resolved = false;
			children = new ArrayList<InstructionPossibility>();
			parents = new ArrayList<InstructionPossibility>();
			parents.add(parent);
		}
		
		/*
		 * clone constructor for branches
		 * Note that arg is initialized to null.
		 */
		public InstructionPossibility(InstructionPossibility clone, ArrayList<InstructionPossibility> cloneLeaves, ArrayList<InstructionPossibility> newLeaves) {
			node = clone.node;
			
			if (clone.arg == null) {
				arg = null;
			}
			else {
				arg = new Arg(clone.arg, clone.getType());
			}
			
			parents = new ArrayList<InstructionPossibility>();
			parents.addAll(clone.parents);
			
			if (cloneLeaves.contains(clone) && !newLeaves.contains(this)) {
				newLeaves.add(this);
			}
			
			children = new ArrayList<InstructionPossibility>();
			InstructionPossibility child;
			for (InstructionPossibility childClone : clone.children) {
				child = new InstructionPossibility(childClone,cloneLeaves,newLeaves);
				
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
		 */
		public boolean resolve(String next) {
			boolean resolved = false;
			boolean trivial = Memory.isTrivial(next);
			
			InstructionPossibility leaf = null;
			int i=0;
			int n=leaves.size();
			char argType;
			
			Logger.log("resolving " + next + " against " + n + " leaves");
			
			/*
			 * When adding new leaves or adding back old ones that are multitoken args, add first to this list, and then
			 * transfer newLeaves to leaves
			 */
			ArrayList<InstructionPossibility> newLeaves = new ArrayList<>();
			
			while (i < n) {
				leaf = leaves.get(i);
				argType = leaf.node.getType();
				
				if (argType == Arg.notarg) {
					Logger.log("checking " + next + " against keyword " + leaf.node.token);
					//is keyword
					if (!trivial) {
						int dist = Utilities.editDistance(leaf.node.token, next, leaf.node.token.length()*2/3);
						
						if (dist != -1) {
							resolved = true;
							leaf.resolved = true;
							
							for (InstructionPossibility newLeaf : leaf.children) {
								if (!newLeaves.contains(newLeaf)) {
									Logger.log("added leaf " + newLeaf.node.token);
									newLeaves.add(newLeaf);
								}
							}
						}
					}
					else {
						resolved = true;
						//leaf was skipped; add it back before removing
						if (!newLeaves.contains(leaf)) {
							newLeaves.add(leaf);
						}
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
							leaf.resolved = true;
							
							for (InstructionPossibility newLeaf : leaf.children) {
								if (!newLeaves.contains(newLeaf)) {
									Logger.log("added leaf " + newLeaf.node.token);
									newLeaves.add(newLeaf);
								}
							}
						}
						
						if (!newLeaves.contains(leaf)) {
							newLeaves.add(leaf);
						}
					}
					else { //multiword arg
						Arg cloneArg = new Arg(leaf.arg, leaf.getType());
						
						//arg could add next token; remains as a leaf
						if (cloneArg.appendToken(argType, next)) {
							//there now exist multiple possible values of this arg, including or not including next token
							InstructionPossibility cloneLeaf = new InstructionPossibility(leaf, leaves, newLeaves); //TODO handle leaves.contains() check
							cloneLeaf.arg = cloneArg;
							
							Logger.log("appended " + next + " to arg " + cloneLeaf.node.token + ": " + cloneLeaf.arg.getText());
							resolved = true;
							cloneLeaf.resolved = true;
							
							for (InstructionPossibility parent : cloneLeaf.parents) {
								parent.children.add(cloneLeaf);
							}
							
							//cloneLeaf adds itself to leaves in constructor
						}
					}
				}
				i++;
			}
			
			leaves.clear();
			leaves.addAll(newLeaves);
			
			System.out.println(diagram());
			
			return resolved;
		}
		
		/*
		 * One possible version of this mapping is a terminal node, and that terminal node has no followers
		 * that are resolved (in leaves).
		 * If a completion exists, the return value is a list of which edge to follow at each node in
		 * the graph.
		 */
		public ArrayList<Integer> complete() {
			if (children.isEmpty()) {
				return null;
			}
			else {
				ArrayList<Integer> subcompletion;
				int[] bestCount = {0,0};
				int[] nextCount = new int[2];
				for (int i=0; i<children.size(); i++) {
					nextCount[0] = 0;
					nextCount[1] = 0;
					subcompletion = children.get(i).complete(i,leaves,nextCount);
					
					if ( nextCount[0] > bestCount[0] || 
						(nextCount[0] == bestCount[0] && nextCount[1] > bestCount[1]) || 
						(bestCount[0] == 0 && bestCount[1] == 0)) {
						Logger.log("new completion (" + nextCount[0] + "," + nextCount[1] + "): " + subcompletion.size());
						completion = subcompletion;
						bestCount[0] = nextCount[0];
						bestCount[1] = nextCount[1];
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
					
					argCount = bestCount;
					return completion;
				}
			}
		}
		
		private ArrayList<Integer> complete(int i, ArrayList<InstructionPossibility> leaves, int[] superCount) {
			ArrayList<Integer> completion = new ArrayList<Integer>();
			
			int[] thisCount = {0,0};
			if (resolved && node != null && node.isTerminal()) { //can end expression
				if (node.getType() == Arg.notarg) { //keyword
					if (!leaves.contains(this)) { //nonleaf terminal keyword = complete
						completion.add(i);
						Logger.log(node.token + " is a resolved keyword");
					}
				}
				else {
					if (arg != null) { //resolved terminal arg = complete.
						completion.add(i);
						Logger.log(node.token + "=" + arg.getValue() + " is a resolved arg");
					}
				}
			}
			
			if (arg != null) {
				thisCount[0] = 1;
				thisCount[1] = arg.tokenCount();
			}
			
			ArrayList<Integer> subcompletion;
			int[] bestCount = {thisCount[0],thisCount[1]};
			int[] nextCount = new int[2];
			for (int j=0; j<children.size(); j++) {
				nextCount[0] = thisCount[0];
				nextCount[1] = thisCount[1];
				subcompletion = children.get(j).complete(j, leaves, nextCount);
				
				if (!subcompletion.isEmpty()) {
					if ( nextCount[0] > bestCount[0] || 
						(nextCount[0] == bestCount[0] && nextCount[1] > bestCount[1]) ||
						(bestCount[0] == thisCount[0] && bestCount[1] == thisCount[1])) {
						subcompletion.add(0, i);
						completion = subcompletion;
						bestCount[0] = nextCount[0];
						bestCount[1] = nextCount[1];
					}
				}
			}
			
			superCount[0] += bestCount[0];
			superCount[1] += bestCount[1];
			return completion;
		}
		
		/*
		 * argCount[] = { arg count, arg token count }
		 * 
		 * complete() always called before this
		 */
		public int[] argCount() {
			return argCount;
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
				
				InstructionPossibility p = children.get(completion.get(0));
				if (p.arg != null) {
					argMap.put(p.arg.name, p.arg);
				}
				
				StringBuilder instructionString = new StringBuilder();	
				
				for (int i=1; i<completion.size() && !p.children.isEmpty(); i++) {
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
			string.append(mapping.id + "\n");
			
			ArrayList<PatternNode> nodes = new ArrayList<>();
			for (InstructionPossibility child : children) {				
				if (!leaves.contains(child)) {
					child.diagram(leaves, nodes, "   ", string);
				}
				else {
					string.append("   ");
					if (child.resolved) {
						string.append("+");
					}
					if (child.arg != null) {
						string.append("[" + child.arg.getValue() + "]");
					}
					else {
						string.append(child.node.token);
					}
					
					if (child.node.isTerminal()) {
						string.append(">>--\n");
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
				if (resolved) {
					string.append("+");
				}
				if (arg != null) {
					string.append("[" + arg.getValue() + "]");
				}
				else {
					string.append(node.token);
				}
				if (node.isTerminal()) {
					string.append("--\n");
				}
				else {
					string.append("\n");
				}
				
				for (InstructionPossibility child : children) {
					if (!leaves.contains(child) || child.arg != null) {
						child.diagram(leaves, nodes, prefix + "   ", string);
					}
					else {
						string.append(prefix + "   ");
						if (child.resolved) {
							string.append("+");
						}
						string.append(child.node.token);
						if (child.node.isTerminal()) {
							string.append(">>--\n");
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
