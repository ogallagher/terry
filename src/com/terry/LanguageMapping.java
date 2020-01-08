package com.terry;

import java.io.Serializable;
import java.util.Iterator;
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
	 * apple ?banana |cinnamon,donut,[@egg_@muffin],)) *flapjack grape) hazelnut icecream
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
		private static final char lbr = '['; //begin option with underscores
		private static final char rbr = ']'; //end option with underscores
		private static final char usr = '_'; //token delimiter for selection
		
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
		
		/*
		 * Tree node for language pattern tree representation
		 */
		private static class PatternNode {
			private String token; //can be literal or an arg or null
			private LinkedList<PatternNode> followers;
			private boolean isArg; //true if token is an id
			
			private static int DIAGRAM_DEPTH = 20;
			
			private PatternNode() {
				token = null;
				followers = new LinkedList<PatternNode>();
				isArg = false;
			}
			
			private void setToken(String tok) {
				if (tok.startsWith("@")) {
					token = tok.substring(1);
					isArg = true;
				}
				else {
					if (tok.equals("")) {
						token = null;
					}
					else {
						token = tok;
					}
					
					isArg = false;
				}
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
				if (isArg) {
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
}
