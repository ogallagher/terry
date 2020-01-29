package com.terry;

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
	private char type;
	protected LanguagePattern pattern;
	
	public static void init(int count) {
		//update count
		LanguageMapping.count = count + 1;
	}
	
	public static boolean empty() {
		return count == 1;
	}
	
	public LanguageMapping(char typ, String expr) {
		id = count++;
		type = typ;		
		pattern = null;
		pattern = new LanguagePattern(expr);
	}
	
	public char getType() {
		return type;
	}
	
	public PatternNode getLeader(String token) {
		if (pattern.graph.token == null) {
			return pattern.graph.getFollower(token);
		}
		else {
			return pattern.graph;
		}
	}
	
	//get all keywords that appear in the pattern, to be added to the dictionary
	public LinkedList<String> getTokens() {
		LinkedList<String> tokens = new LinkedList<String>();
		pattern.getTokens(tokens, pattern.graph);
		return tokens;
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
		
		id = Integer.parseInt(fields[0]);
		type = fields[1].charAt(0);
		pattern = new LanguagePattern(fields[2]);
	}
	
	/*
	 * Valid expressions are inspired by BNF and regex, but these use prefix notation, like so:
	 * apple ?banana |cinnamon,donut,[@$egg_@$muffin],)) *flapjack grape) hazelnut icecream
	 */
	private static class LanguagePattern {
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
		
		public void getTokens(LinkedList<String> tokens, PatternNode node) {
			String token;
			boolean go = true;
			
			if (node.type == PatternNode.notarg) {
				token = node.token;
				
				if (token != null) {
					if (tokens.contains(token)) {
						go = false;
					}
					else {
						tokens.add(token);
					}
				}
			}
			
			if (go) {
				for (PatternNode follower : node.followers) {
					getTokens(tokens, follower);
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
		
		//arg types denoted with suffixes
		public static final char notarg = '0';		//not arg
		public static final char strarg = '$';		//string
		public static final char numarg = '#';		//number
		public static final char wigarg = 'w';		//widget
		public static final char colarg = 'c';		//color
		public static final char spdarg = '>';		//speed
		public static final char dirarg = '+';		//direction
		
		private static int DIAGRAM_DEPTH = 20;
		
		private PatternNode() {
			token = null;
			followers = new LinkedList<PatternNode>();
			type = notarg;
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
				type = notarg;
				token = tok;
			}					
		}
		
		public char getType() {
			return type;
		}
		
		/*
		 * Returns possible follower nodes. Skips nulls!
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
		
		public PatternNode getFollower(String token) {
			for (PatternNode node : followers) {
				if (node.token == null) {
					PatternNode follower = node.getFollower(token);
					
					if (follower != null) {
						return follower;
					}
				}
				else if (node.token.equals(token)) {
					return node;
				}
			}
			
			//follower of given token not found
			return null;
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
			if (type != notarg) {
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
