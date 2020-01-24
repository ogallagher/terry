package com.terry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

/*
 * trivials = word word...
 * dictionary = token lm1 lm2 ... lmn
 * mappings = type	id	expr<subclass-specific>
 */
public class Memory {
	private static final String MEMORY_PATH = Terry.RES_PATH + "memory/";
	private static final String TRIV_FILE = "triv.txt";
	private static final String DICT_FILE = "dict.txt";
	private static final String MAPS_FILE = "maps.txt";
	
	private static ArrayList<String> trivials; //unimportant words commonly encountered that can be quickly ignored
	private static HashMap<String,ArrayList<LanguageMapping>> dictionary; //token,references
	private static HashMap<Integer,LanguageMapping> mappings; //key,mapping (action/lesson/widget)
	
	private static final int EDIT_DIST_MAX = 4; //edit dist (insert,delete,replace) must be less than 4 to be considered
	
	public static void init() throws MemoryException {
		File memDir = new File(Terry.class.getResource(MEMORY_PATH).getPath());
		
		trivials = new ArrayList<String>();
		File trivFile = new File(memDir, TRIV_FILE);
		if (trivFile.exists()) {
			try {
				Scanner trivsReader = new Scanner(trivFile);
				
				while (trivsReader.hasNext()) {
					trivials.add(trivsReader.next());
				}
				
				trivsReader.close();
			} 
			catch (FileNotFoundException e) {
				throw new MemoryException("failed to read trivials file at " + trivFile.getAbsolutePath());
			}
		}
		else {
			Logger.logError("no trivials file found at " + trivFile.getAbsolutePath());
		}
		
		mappings = new HashMap<Integer,LanguageMapping>();
		File mapsFile = new File(memDir, MAPS_FILE);
		if (mapsFile.exists()) {
			try {
				@SuppressWarnings("resource")
				Scanner mapsReader = new Scanner(mapsFile);
				String line;
				char type;
				
				while (mapsReader.hasNextLine()) {
					line = mapsReader.nextLine();
					type = line.charAt(0);
					
					try {
						ObjectInputStream stream = new ObjectInputStream(new ByteArrayInputStream(line.getBytes(StandardCharsets.UTF_8)));

						switch (type) {
							case LanguageMapping.TYPE_ACTION:
								Action action = (Action) stream.readObject();
								mappings.put(action.getId(), action);
								
								break;
								
							case LanguageMapping.TYPE_LESSON:
								//TODO deserialize lesson
								break;
								
							case LanguageMapping.TYPE_WIDGET:
								//TODO deserialize widget
								break;
						}
					} 
					catch (IOException | ClassNotFoundException e) {
						throw new MemoryException("failed to deserialize mapping " + line);
					}
				}
				
				mapsReader.close();
			}
			catch (FileNotFoundException e) {
				throw new MemoryException("maps file not found at " + mapsFile.getAbsolutePath());
			}
		}
		else {
			try {
				mapsFile.createNewFile();
			}
			catch (IOException e) {
				throw new MemoryException("could not create blank maps file at " + mapsFile.getAbsolutePath());
			}
		}
		
		dictionary = new HashMap<String,ArrayList<LanguageMapping>>();
		
		//TODO init from dict files
		File dictFile = new File(memDir, DICT_FILE);
		if (dictFile.exists()) {
			try {
				Scanner dictReader = new Scanner(dictFile);
				String[] line;
				String token;
				int ref;
				
				while (dictReader.hasNextLine()) {
					line = dictReader.nextLine().split(" ");
					token = line[0];
					
					ArrayList<LanguageMapping> refs = new ArrayList<LanguageMapping>();
					for (int i=1; i<line.length; i++) {
						ref = Integer.parseInt(line[i]);
						refs.add(mappings.get(ref));
					}
					
					dictionary.put(token, refs);
				}
				
				dictReader.close();
			} 
			catch (FileNotFoundException e) {
				throw new MemoryException("dict file not found at " + dictFile.getAbsolutePath());
			}
		}
		else {
			try {
				dictFile.createNewFile();
			}
			catch (IOException e) {
				throw new MemoryException("could not create blank dict file at " + dictFile.getAbsolutePath());
			}
		}
	}
	
	public static boolean isTrivial(String token) {
		return trivials.contains(token);
	}
	
	/*
	 * Return either the entry for the exact token-key match, the entry for the match with the lowest edit distance,
	 * or null if no match is found.
	 */
	public static ArrayList<LanguageMapping> dictionaryLookup(String token) {
		ArrayList<LanguageMapping> entry = dictionary.get(token);
		
		//score keys if not found
		if (entry == null) {
			Set<String> keys = dictionary.keySet();
			
			int best = EDIT_DIST_MAX;
			String match = null;
			
			/*
			 * Check edit distance. 
			 * Uses the Wagner-Fischer algorithm for Levenshtein edit distance.
			 */
			for (String key : keys) {
				char[] t = token.toCharArray(); //token = first row
				char[] k = key.toCharArray(); //key = first col
				
				int w = t.length+1;
				int h = k.length+1;
				
				int[][] d = new int[h][w]; //matrix of substring distances
				
				for (int i=0; i<w; i++) { //init first row
					d[0][i] = i;
				}
				for (int i=0; i<h; i++) { //init first col
					d[i][0] = i;
				}
				
				int sc; //substitution cost
				int mc; //min cost (between insert, delete, substitute)
				int c;	//temp cost var for comparison
				
				int dist = 0; //running count of current edit distance
				
				//compute distance, keeping in mind best distance so far
				for (int y=1; y<h && dist < best; y++) {
					for (int x=1; x<w; x++) {
						if (t[x-1] == k[y-1]) {
							sc = 0;
						}
						else {
							sc = 1;
						}
						
						mc = d[y][x-1];
						c = d[y-1][x];
						if (c < mc) {
							mc = c;
						}
						c = d[y-1][x-1] + sc;
						if (c < mc) {
							mc = c;
						}
						
						d[y][x] = mc;
						if (x == y || y == h-1 || x == w-1) {
							dist = mc;
						}
					}
				}
				
				if (dist < best) {
					match = key;
				}
			}
			
			if (match == null) {
				//no results found
				return null;
			}
			else {
				//return closest match
				return dictionary.get(match);
			}
		}
		
		return entry;
	}
	
	public static class MemoryException extends Exception {
		private static final long serialVersionUID = -5312985469881250543L;
		private String message;
		
		public MemoryException(String message) {
			this.message = message;
		}
		
		public MemoryException() {
			this.message = "memory failed for unknown reason";
		}
		
		@Override
		public String getMessage() {
			return message;
		}
	}
}
