package com.terry;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public class InstructionPossibilities {
	private LinkedList<InstructionPossibility> possibilities;
	
	public InstructionPossibilities() {
		possibilities = new LinkedList<InstructionPossibility>();
	}
	
	private static class InstructionPossibility {
		private String token;
		private ArrayList<InstructionPossibility> children;
		
		public InstructionPossibility(Entry<String,ArrayList<LanguageMapping>> entry) {
			token = entry.getKey();
			
			children = new ArrayList<InstructionPossibility>();
			for (LanguageMapping lm : entry.getValue()) {
				List<String> followers = lm.getFollowers(token);
				
				for (String follower : followers) {
					children.add(new InstructionPossibility(follower));
				}
			}
		}
		
		public InstructionPossibility(String token) {
			this.token = token;
			children = null;
		}
	}
}
