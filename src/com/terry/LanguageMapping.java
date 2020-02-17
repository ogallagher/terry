package com.terry;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Stack;

/*
 * superclass for actions, widgets, and lessons
 */
public class LanguageMapping {
	public static int count = 0;
	
	public static final char TYPE_ACTION = 'a';
	public static final char TYPE_LESSON = 'l';
	public static final char TYPE_WIDGET = 'w';
	public static final char TYPE_UNKNOWN = '?';
	
	private static final char spc = ' '; //token delimiter
	private static final char rpr = ')'; //end group
	private static final char sop = '*'; //0 or more
	private static final char top = '+'; //1 or more
	private static final char qop = '?'; //0 or 1
	private static final char pop = '|'; //selection
	private static final char aop = '@'; //arg id
	private static final char cma = ','; //delimiter for options in selection
	private static final char lbr = '['; //begin option with underscores
	private static final char rbr = ']'; //end option with underscores
	private static final char usr = '_'; //token delimiter for selection
	
	protected int id;
	protected char type;
	protected LanguagePattern pattern;
	
	public static void init(int count) {
		//update count
		LanguageMapping.count = count + 1;
		Logger.log("language mapping init success");
	}
	
	public static boolean empty() {
		return count == 1;
	}
	
	//for child class deserialization
	public LanguageMapping() {
		id = count++;
		type = TYPE_UNKNOWN;
		pattern = null;
	}
	
	public LanguageMapping(char typ, String expr) {
		id = count++;
		type = typ;
		pattern = new LanguagePattern(expr);
	}
	
	public char getType() {
		return type;
	}
	
	public int getId() {
		return id;
	}
	
	public ArrayList<PatternNode> getLeaders(String token) {
		ArrayList<PatternNode> leaders = new ArrayList<>();
		
		if (pattern.graph.token == null) { //move inward
			leaders.addAll(pattern.graph.getFollowers(token));
		}
		else if (pattern.graph.token.equals(token) || pattern.graph.type != Arg.notarg) {
			//keyword match, or argument match
			leaders.add(pattern.graph);
		}
		
		return leaders;
	}
	
	/*
	 * Get all tokens that can begin the pattern, to be added to
	 * the dictionary. Argument tokens are abbreviated to @argtype (ex: @$, @#) 
	 */
	public LinkedList<String> getLeaders() {
		LinkedList<String> leaders = new LinkedList<String>();
		pattern.getLeaders(leaders, pattern.graph);
		return leaders;
	}
	
	public String patternDiagram() {
		return pattern.diagram();
	}
	
	@Override
	public String toString() {
		String string = type + "\t" + id + "\t" + pattern;
		
		return string;
	}
	
	public void fromString(String string) {
		String[] fields = string.split("\t");
		
		type = fields[0].charAt(0);
		id = Integer.parseInt(fields[1]);
		pattern = new LanguagePattern(fields[2]);
	}
	
	/*
	 * Valid expressions are inspired by BNF and regex, but these use prefix notation, like so:
	 * apple ?banana |cinnamon,donut,[@$egg_@$muffin],)) *flapjack grape) hazelnut icecream
	 */
	public static class LanguagePattern {
		private String expression;
		private PatternNode graph;
		
		public LanguagePattern(String expr) {
			expression = expr;
			graph = PatternNode.newGraph(expr.toCharArray());
		}
		
		/*
		 * Draws a tabular diagram of the expression. The edges list prevents infinite loops on graph cycles.
		 */
		public String diagram() {			
			return graph.diagram("");
		}
		
		public void getLeaders(LinkedList<String> leaders, PatternNode node) {
			String token = node.token;
			
			if (token == null) { //skip null node, get children
				for (PatternNode follower : node.followers) {
					getLeaders(leaders,follower);
				}
			}
			else if (node.type == Arg.notarg) { //add keyword leader
				if (!leaders.contains(token)) {
					leaders.add(token);
				}
			}
			else { //add argument leader
				String arg = "@" + node.type;
				
				if (!leaders.contains(arg)) {
					leaders.add(arg);
				}
			}
		}
		
		@Override
		public String toString() {
			return expression;
		}
	}
	
	/*
	 * Tree node for language pattern tree representation
	 */
	public static class PatternNode {
		public String token; //can be literal or an arg or null
		public LinkedList<PatternNode> followers;
		private char type;
		
		private boolean terminal;	//allowed to have no followers
		
		private static int DIAGRAM_DEPTH = 20;
		
		private PatternNode() {
			token = null;
			followers = new LinkedList<PatternNode>();
			type = Arg.notarg;
			terminal = false;
		}
		
		private void setToken(String tok) {
			if (tok.startsWith(String.valueOf(aop))) { //is arg
				type = tok.charAt(1);
				token = tok.substring(2);
			}
			else if (tok.equals("")) { //is null
				token = null;
			}
			else { //is keyword
				type = Arg.notarg;
				token = tok;
			}					
		}
		
		public char getType() {
			return type;
		}
		
		public boolean isTerminal() {
			if (terminal || followers.isEmpty()) {
				return true;
			}
			else {
				for (PatternNode follower : followers) {
					if (follower.token == null && follower.isTerminal()) {
						return true;
					}
				}
				
				return false;
			}
		}
		
		/*
		 * Returns possible follower nodes. Skips nulls.
		 */
		public LinkedList<PatternNode> getFollowers() {
			LinkedList<PatternNode> nodes = new LinkedList<PatternNode>();
			
			for (PatternNode follower : followers) {
				if (follower.token == null) {
					nodes.addAll(follower.getFollowers());
				}
				else {
					nodes.add(follower);
				}
			}
			
			return nodes;
		}
		
		public ArrayList<PatternNode> getFollowers(String token) {
			ArrayList<PatternNode> matches = new ArrayList<>();
			
			for (PatternNode node : followers) {
				if (node.token == null) { //move along
					matches.addAll(node.getFollowers(token));
				}
				else if (node.token.equals(token) || 
						(node.type != Arg.notarg && Arg.getArgValue(node.type, token) != null)) { //keyword match, arg match
					matches.add(node);
				}
			}
			
			return matches;
		}
		
		public int tokenCount() {
			int finalCount = 0;
			int candidateCount = 0;
			
			if (token != null) {
				finalCount++;
			}
			
			for (PatternNode follower : followers) {
				candidateCount = follower.tokenCount();
				
				if (candidateCount < finalCount) {
					finalCount = candidateCount;
				}
			}
			
			return finalCount;
		}
		
		public static PatternNode newGraph(char[] expr) {
			PatternNode root = new PatternNode();
			PatternNode node = root;
			Stack<PatternNode> last = new Stack<PatternNode>();
			
			LinkedList<PatternNode> nully = new LinkedList<PatternNode>();
			nully.add(root);
			
			int i=0;
			char c;
			int a=0;
			int n = expr.length;
			
			while (i < n) {
				c = expr[i];
				
				if (c == qop) {			//? = optional
					PatternNode in = new PatternNode();
					node.followers.add(in);
					
					PatternNode out = new PatternNode();
					node.followers.add(out);
					last.push(out);
					
					i++;
					node = in;
					a = i;
					
					nully.add(in);
					nully.add(out);
				}
				else if (c == sop) {	//* = 0 or more
					PatternNode in = new PatternNode();
					node.followers.add(in);
					
					PatternNode out = new PatternNode();
					out.followers.add(in);
					node.followers.add(out);
					last.push(out);
					
					i++;
					node = in;
					a = i;
					
					nully.add(in);
					nully.add(out);
				}
				else if (c == top) {	//+ = 1 or more
					PatternNode in = new PatternNode();
					node.followers.add(in);
					
					PatternNode out = new PatternNode();
					out.followers.add(in);
					last.push(out);
					
					i++;
					node = in;
					a = i;

					nully.add(in);
					nully.add(out);
				}
				else if (c == pop) {	//| = selection
					PatternNode out = new PatternNode();
					last.push(out);
					
					i++;
					a = i;
					
					nully.add(out);
				}
				else if (c == cma) {	//, = selection option
					PatternNode option = new PatternNode();
					option.followers.add(last.peek());
					option.setToken(String.copyValueOf(expr,a,i-a));
					
					node.followers.add(option);
					
					i++;
					if (expr[i] == rpr) {	//end selection
						i++;
						node = last.pop();
					}
					a = i;
					
					nully.add(option);
				}
				else if (c == rpr) {	//) = end group (non-selection)
					String str = String.copyValueOf(expr,a,i-a);
					if (str.length() != 0) {
						node.setToken(str);
					}
					
					PatternNode tail = last.pop();
					node.followers.add(tail);
					
					PatternNode next = new PatternNode();
					tail.followers.add(next);
					
					i++;
					node = next;
					a = i;
				}
				else if (c == spc) {	//space = finish this token, begin next token
					String str = String.copyValueOf(expr,a,i-a);
					
					if (str.length() != 0) {
						node.setToken(str);
						
						PatternNode next = new PatternNode();
						node.followers.add(next);
						
						i++;
						node = next;
						
						nully.add(next);
					}
					else {
						i++;
					}
					a = i;
				}
				else if (c == lbr) {	//[ = begin composite option
					last.push(node);
					i++;			
					a = i;
				}
				else if (c == usr) {	//_ = space for selection
					PatternNode next = new PatternNode();
					next.setToken(String.copyValueOf(expr,a,i-a));
					
					node.followers.add(next);
					
					i++;
					node = next;
					a = i;
					
					nully.add(next);
				}
				else if (c == rbr) {	//] = end composite option
					PatternNode leader = last.pop();
					
					PatternNode next = new PatternNode();
					String str = String.copyValueOf(expr,a,i-a);
					
					if (str.length() != 0) {
						next.setToken(str);
						
						node.followers.add(next);
						next.followers.add(last.peek());
						
						nully.add(next);
					}
					else {
						node.followers.add(last.peek());
					}
					
					i += 2;
					if (expr[i] == rpr) {	//end selection
						i++;
						node = last.pop();
					}
					else {
						node = leader;
					}
					a = i;
				}
				else {						//token chars
					i++;
				}
			}
			
			//end of expr
			node.setToken(String.copyValueOf(expr,a,n-a));
			node.followers.clear();
			node.terminal = true;
			
			//remove single-parent nulls
			Iterator<PatternNode> walker = nully.iterator();
			while (walker.hasNext()) {
				node = walker.next();
				
				if (node.token == null) {
					PatternNode parent = null;
					boolean orphan = true;
					boolean multiparent = false;
					
					//get parent
					for (PatternNode p : nully) {
						if (p.followers.contains(node)) {
							if (orphan) {
								parent = p;
								orphan = false;
							}
							else {
								multiparent = true;
							}
						}
					}
					
					if (!orphan && !multiparent) {
						//pass followers to parent
						for (PatternNode f : node.followers) {
							if (!parent.followers.contains(f)) {
								parent.followers.add(f);
							}
						}
						
						//remove null node
						parent.followers.remove(node);
					}
				}
			}
			
			return root;
		}
		
		public String diagram(String indent) {
			String tok = token;
			if (type != Arg.notarg) {
				tok = "<" + tok + ">";
			}
			String diagram = indent + tok;
			
			if (indent.length() < DIAGRAM_DEPTH) {
				for (PatternNode follower : followers) {
					diagram += "\n" + follower.diagram(indent + "\t");
				}
			}
			
			return diagram;
		}
	}
}
