package com.terry;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/*
 * superclass for actions, widgets, and lessons
 */
public class LanguageMapping implements Serializable {
	private static final long serialVersionUID = 1216321362452886088L;

	public static int count = 0;
	
	public static final char TYPE_ACTION = 1;
	public static final char TYPE_LESSON = 2;
	public static final char TYPE_WIDGET = 3;
	
	private int id;
	private char type;
	private LanguagePattern pattern;
	private Object value;
	
	public void setPattern(String expr) {
		pattern = new LanguagePattern(expr);
	}
	
	//get possible following tokens after leader token
	public List<String> getFollowers(String leader) {
		LinkedList<String> followers = new LinkedList<String>();
		
		//TODO find followers
		
		return followers;
	}
	
	public String patternDiagram() {
		return pattern.diagram();
	}
	
	/*
	 * Valid expressions are inspired by BNF and regex, but these use prefix notation, like so:
	 * apple ?banana |cinnamon,donut,@egg_@muffin,)) *flapjack grape) hazelnut icecream
	 */
	private static class LanguagePattern {
		private static final char spc = ' '; //token delimiter
		private static final char rpr = ')'; //end group
		private static final char sop = '*'; //0 or more
		private static final char top = '+'; //1 or more
		private static final char qop = '?'; //0 or 1
		private static final char pop = '|'; //selection
		private static final char aop = '@'; //arg id
		private static final char cma = ','; //delimiter for options in selection
		private static final char usr = '_'; //token delimiter for selection
		
		private String expression;
		private PatternNode graph;
		
		public LanguagePattern(String expr) {
			expression = expr;
			graph = PatternNode.newGraph(expr);
		}
		
		/*
		 * Draws a tabular diagram of the expression. The edges list prevents infinite loops on graph cycles.
		 */
		public String diagram() {			
			return graph.diagram("");
		}
		
		/*
		 * Tree node for language pattern tree representation
		 */
		private static class PatternNode {
			private String token; //can be literal or an arg or null
			private LinkedList<PatternNode> followers;
			private boolean isArg; //true if token is an id
			
			private PatternNode(String tok) {
				token = tok;
				followers = new LinkedList<PatternNode>();
				isArg = false;
			}
			
			private PatternNode() {
				this(null);
			}
			
			private void setTokens(String val) {
				String[] vals = val.split("_");
				
				if (vals.length > 1) {
					token = vals[0];
					
					@SuppressWarnings("unchecked")
					LinkedList<PatternNode> ends = (LinkedList<PatternNode>) followers.clone();
					followers.clear();
					
					PatternNode leader = this;
					PatternNode follower = null;
					for (int v=1; v<vals.length; v++) {
						follower = new PatternNode();
						follower.token = vals[v];
						
						leader.followers.add(follower);
						leader = follower;
					}
					
					follower.followers = ends;
				}
				else {
					token = val;
				}
			}
			
			public static PatternNode newGraph(String expr) {
				PatternNode root = new PatternNode();
				graph(expr.toCharArray(), 0, root, new Stack<PatternNode>());
				
				return root;
			}
			
			public static void graph(char[] expr, int i, PatternNode node, Stack<PatternNode> last) {
				char c;
				int a = i;
				int n = expr.length;
				boolean go = true;
				
				while (go) {
					c = expr[i];
					
					if (c == qop) {			//? = optional
						PatternNode in = new PatternNode();
						node.followers.add(in);
						
						PatternNode out = new PatternNode();
						node.followers.add(out);
						last.push(out);
						
						i++;
						graph(expr, i, in, last);
						go = false;
					}
					else if (c == sop) {	//* = 0 or more
						PatternNode in = new PatternNode();
						node.followers.add(in);
						
						PatternNode out = new PatternNode();
						out.followers.add(in);
						node.followers.add(out);
						last.push(out);
						
						i++;
						graph(expr, i, in, last);
						go = false;
					}
					else if (c == top) {	//+ = 1 or more
						PatternNode in = new PatternNode();
						node.followers.add(in);
						
						PatternNode out = new PatternNode();
						out.followers.add(in);
						last.push(out);
						
						i++;
						graph(expr, i, in, last);
						go = false;
					}
					else if (c == pop) {	//| = selection
						last.push(new PatternNode());
						
						i++;
						graph(expr, i, node, last);
						go = false;
					}
					else if (c == cma) {	//, = selection option
						PatternNode option = new PatternNode();
						option.followers.add(last.peek());
						option.setTokens(String.copyValueOf(expr,a,i-a));
						
						node.followers.add(option);
						
						i++;
						if (expr[i] == rpr) {	//end selection
							i++;
							graph(expr, i, last.pop(), last);
						}
						else {
							graph(expr, i, node, last);
						}
						go = false;
					}
					else if (c == rpr) {	//) = end group (non-selection)
						String str = String.copyValueOf(expr,a,i-a);
						if (str.length() != 0) {
							node.token = str;
						}
						
						PatternNode tail = last.pop();
						node.followers.add(tail);
						
						i++;
						graph(expr, i, tail, last);
						go = false;
					}
					else if (c == aop) {	//@ = arg
						node.isArg = true;
						i++;
					}
					else if (c == spc) {	//space = finish this token, begin next token
						String str = String.copyValueOf(expr,a,i-a);
						
						if (str.length() != 0) {
							node.token = str;
							
							PatternNode next = new PatternNode();
							node.followers.add(next);
							
							i++;
							graph(expr, i, next, last);
							
							go = false;
						}
						else {
							i++;
							a = i;
						}
					}
					else if (i == n-1) {	//end of expr
						node.token = String.copyValueOf(expr,a,n-a);
						node.followers.clear();
						go = false;
					}
					else {
						i++;
					}
				}
			}
			
			public LinkedList<PatternNode> getFollowers() {
				return followers;
			}
			
			public String diagram(String indent) {
				String diagram = indent + token;
				
				for (PatternNode follower : followers) {
					diagram += "\n" + follower.diagram(indent + "\t");
				}
				
				return diagram;
			}
		}
	}
}
