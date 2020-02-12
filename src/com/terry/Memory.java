package com.terry;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

/*
 * trivials = word word ...
 * dictionary = token lm1 lm2 ... lmn
 * mappings = type	id	expr<subclass-specific>
 */
public class Memory {
	private static final String MEMORY_PATH = Terry.RES_PATH + "memory/";
	private static final String TRIV_FILE = "triv.txt";
	private static final String DICT_FILE = "dict.txt";
	private static final String ACTS_FILE = "acts.txt";
	private static final String LESN_FILE = "lesn.txt";
	private static final String WIGT_FILE = "wigt.txt";
	
	private static File memDir, actsFile, dictFile, lesnFile, wigtFile;
	
	private static ArrayList<String> trivials; //unimportant words commonly encountered that can be quickly ignored
	private static HashMap<String,ArrayList<LanguageMapping>> dictionary; //token,references
	private static HashMap<Integer,LanguageMapping> mappings; //key,mapping (action/lesson/widget)
	
	public static boolean saved;
	
	public static void init() throws MemoryException {
		memDir = new File(Terry.class.getResource(MEMORY_PATH).getPath());
		
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
		int lastId = 0; //int for generating unique ids
		
		actsFile = new File(memDir, ACTS_FILE);
		if (actsFile.exists()) {
			try {
				FileInputStream actionStream = new FileInputStream(actsFile);
				
				if (actionStream.available() != 0) {
					Logger.log("deserializing actions");
					ObjectInputStream deserializer = new ObjectInputStream(actionStream);
					
					Action action;
					int id;
					try {
						while (true) {
							try {
								action = (Action) deserializer.readObject();
								id = action.getId();
								if (id > lastId) {
									lastId = id;
								}
								
								//add to mappings
								mappings.put(id, action);
								
								//add to states
								action.addStates();
							}
							catch (ClassNotFoundException | InvalidClassException e) {
								e.printStackTrace();
								
								deserializer.close();
								actionStream.close();
								
								throw new MemoryException("failed to deserialize action");
							}
						}
					}
					catch (EOFException e) {
						Logger.log("no more actions found");
					}
					
					deserializer.close();
				}
				
				actionStream.close();
			}
			catch (FileNotFoundException e) {
				throw new MemoryException("action file not found at " + actsFile.getAbsolutePath());
			} 
			catch (IOException e) {
				throw new MemoryException("could not read from action file at " + actsFile.getAbsolutePath());
			}
		}
		else {
			try {
				actsFile.createNewFile();
				Logger.log("created blank mappings file");
			}
			catch (IOException e) {
				throw new MemoryException("could not create blank maps file at " + actsFile.getAbsolutePath());
			}
		}
		
		//lessons
		lesnFile = new File(memDir, LESN_FILE);
		if (lesnFile.exists()) {
			FileInputStream lessonStream;
			try {
				lessonStream = new FileInputStream(lesnFile);
				
				if (lessonStream.available() != 0) {
					Logger.log("deserializing lessons");
					ObjectInputStream deserializer = new ObjectInputStream(lessonStream);
					
					Lesson lesson;
					int id;
					try {
						while (true) {
							try {
								lesson = (Lesson) deserializer.readObject();
								
								id = lesson.getId();
								if (id > lastId) {
									lastId = id;
								}
								
								//add to mappings
								mappings.put(id, lesson);
							} 
							catch (ClassNotFoundException | InvalidClassException e) {
								e.printStackTrace();
								
								deserializer.close();
								lessonStream.close();
								
								throw new MemoryException("failed to deserialize lesson");
							}
						}
					}
					catch (EOFException e) {
						Logger.log("no more lessons found");
					}
				}
				
				lessonStream.close();
			} 
			catch (FileNotFoundException e) {
				throw new MemoryException("lesson file not found at " + lesnFile.getAbsolutePath());
			} 
			catch (IOException e) {
				throw new MemoryException("could not read from lesson file at " + lesnFile.getAbsolutePath());
			}
		}
		else {
			try {
				lesnFile.createNewFile();
				Logger.log("created blank lessons file");
			}
			catch (IOException e) {
				throw new MemoryException("could not create blank lesson file at " + lesnFile.getAbsolutePath());
			}
		}
		
		//widgets
		wigtFile = new File(memDir, WIGT_FILE);
		if (wigtFile.exists()) {
			FileInputStream widgetStream;
			try {
				widgetStream = new FileInputStream(wigtFile);
				
				if (widgetStream.available() != 0) {
					Logger.log("deserializing widgets");
					ObjectInputStream deserializer = new ObjectInputStream(widgetStream);
					
					Widget widget;
					int id;
					try {
						while (true) {
							try {
								widget = (Widget) deserializer.readObject();
								
								id = widget.getId();
								if (id > lastId) {
									lastId = id;
								}
								
								//add to mappings
								mappings.put(id, widget);
							} 
							catch (ClassNotFoundException | InvalidClassException e) {
								e.printStackTrace();
								
								deserializer.close();
								widgetStream.close();
								
								throw new MemoryException("failed to deserialize widget");
							}
						}
					}
					catch (EOFException e) {
						Logger.log("no more widgets found");
					}
				}
				
				widgetStream.close();
			} 
			catch (FileNotFoundException e) {
				throw new MemoryException("widget file not found at " + wigtFile.getAbsolutePath());
			} 
			catch (IOException e) {
				throw new MemoryException("could not read from widget file at " + wigtFile.getAbsolutePath());
			}
		}
		else {
			try {
				wigtFile.createNewFile();
				Logger.log("created blank widgets file");
			}
			catch (IOException e) {
				throw new MemoryException("could not create blank widgets file at " + wigtFile.getAbsolutePath());
			}
		}
		
		LanguageMapping.init(lastId);
		
		dictionary = new HashMap<String,ArrayList<LanguageMapping>>();
		dictFile = new File(memDir, DICT_FILE);
		if (dictFile.exists()) {
			try {
				Scanner dictReader = new Scanner(dictFile);
				String[] line;
				String token;
				int ref;
				
				while (dictReader.hasNextLine()) {
					line = dictReader.nextLine().split(" ");
					token = line[0];
					
					if (token != null && !token.isEmpty()) {
						ArrayList<LanguageMapping> refs = new ArrayList<LanguageMapping>();
						for (int i=1; i<line.length; i++) {
							ref = Integer.parseInt(line[i]);
							refs.add(mappings.get(ref));
						}
						
						if (!refs.contains(null)) {
							dictionary.put(token, refs);
						}
					}
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
		
		saved = true;
		Logger.log("memory init success");
	}
	
	public static boolean isTrivial(String token) {
		return token.trim().length() == 0 || trivials.contains(token);
	}
	
	/*
	 * Return either the entry for the exact token-key match, the entries with the lowest edit distance,
	 * or null if no match is found. For handling args, also check against each argtype and include those
	 * entries' mappings also.
	 */
	public static ArrayList<Lookup> dictionaryLookup(String token, boolean fuzzyMatch, boolean argMatch) {
		ArrayList<Lookup> mappings = new ArrayList<Lookup>();
		
		//check fuzzy match with keywords
		if (fuzzyMatch) {
			Set<String> keys = dictionary.keySet();
			
			int best = token.length()*2/3; //edit dist (insert,delete,replace) must be less than token.length()*2/3
			LinkedList<String> matches = new LinkedList<String>();
			
			//check edit distance
			for (String key : keys) {
				int dist = Utilities.editDistance(token, key, best);
				
				if (dist != -1) {
					if (dist < best) { //new best
						matches.clear();
						matches.add(key);
						best = dist;
					}
					else if (dist == best) { //tied for best
						matches.add(key);
					}
				}
			}
			
			if (matches.isEmpty()) {
				//no results found
				Logger.log(token + " not in dictionary");
			}
			else {
				//return closest matches	
				int m = 0;
				Lookup l;
				for (String match : matches) {
					l = new Lookup(match,dictionary.get(match));
					mappings.add(l);
					m += l.mappings.size();
				}
				
				Logger.log(token + " returned " +  m + " possible matches");
				
			}
		}
		//check exact match with keywords
		else {
			Lookup exact = new Lookup(token, dictionary.get(token));
			
			if (exact.mappings == null) {
				//no exact matches
				Logger.log(token + " not in dictionary");
				return null;
			}
			else {
				Logger.log(token + " returned " + exact.mappings.size() + " exact matches");
				mappings.add(exact);
			}
		}
		
		//check value with argtypes
		if (argMatch) {
			Object value = null;
			for (char argtype : Arg.argtypes) {
				value = Arg.getArgValue(argtype, token);
				
				if (value != null) {
					Lookup argLookup = new Lookup(token, dictionary.get("@" + argtype));
					
					if (argLookup.mappings == null) {
						//no mappings begin with this argtype
						Logger.log("arg type " + argtype + " not in dictionary");
					}
					else {
						Logger.log(token + " returned " + argLookup.mappings.size() + " argument matches");
						mappings.add(argLookup);
					}
				}
			}
		}
		
		if (mappings.isEmpty()) {
			return null;
		}
		else {
			return mappings;
		}
	}
	
	/*
	 * Search all mappings for another with the same language pattern.
	 */
	public static LanguageMapping patternLookup(String pattern) {
		LanguageMapping mapping = null;
		
		for (LanguageMapping checked : mappings.values()) {
			if (checked.pattern.toString().equals(pattern)) {
				mapping = checked;
				return mapping;
			}
		}
		
		return null;
	}
	
	public static void addMapping(LanguageMapping mapping) {
		LanguageMapping existing = patternLookup(mapping.pattern.toString());
		
		if (existing == null) {
			//add new mapping
			Logger.log("adding mapping " + mapping.id);
			mappings.put(mapping.id, mapping);
			
			//update dictionary
			LinkedList<String> tokens = mapping.getLeaders();
			ArrayList<LanguageMapping> entry = null;
			
			for (String token : tokens) {
				entry = dictionary.get(token);
				
				if (entry == null) {
					//add new word to dictionary
					entry = new ArrayList<LanguageMapping>();
					entry.add(mapping);
					dictionary.put(token, entry);
				}
				else {
					//add mapping to existing word's dictionary entry
					entry.add(mapping);
				}
			}
		}
		else {
			//replace existing mapping
			Logger.log("updating mapping " + existing.id);
			if (LanguageMapping.count == mapping.id+1) { //decrement id generator
				LanguageMapping.count--;
			}
			mapping.id = existing.id;
			mappings.put(mapping.id, mapping);
		}
		
		saved = false;
	}
	
	public static Collection<LanguageMapping> getMappings() {
		return mappings.values();
	}
	
	public static String printDictionary() {
		String string = "";
		
		for (Entry<String, ArrayList<LanguageMapping>> entry : dictionary.entrySet()) {
			string += entry.getKey() + ": ";
			
			for (LanguageMapping lm : entry.getValue()) {
				string += lm.id + " ";
			}
			
			string += "\n";
		}
		
		return string;
	}
	
	/*
	 * Save data structures in their corresponding files.
	 */
	public static void save() throws MemoryException {
		Logger.log("saving memory");
		
		//save dictionary
		try {
			FileWriter dictWriter = new FileWriter(dictFile);
			String out = "";
			
			for (Entry<String, ArrayList<LanguageMapping>> entry : dictionary.entrySet()) {
				//token
				out += entry.getKey();
				
				//mappings
				for (LanguageMapping lm : entry.getValue()) {
					out += " " + lm.id;
				}
				
				//next line
				out += '\n';
			}
			
			dictWriter.write(out);
			dictWriter.close();
		} 
		catch (IOException e) {
			dictFile.delete();
			throw new MemoryException("failed to save dictionary");
		}
		
		//save mappings
		try {
			FileOutputStream actsWriter = new FileOutputStream(actsFile);
			ObjectOutputStream actsSerializer = new ObjectOutputStream(actsWriter);
			FileOutputStream lesnWriter = new FileOutputStream(lesnFile);
			ObjectOutputStream lesnSerializer = new ObjectOutputStream(lesnWriter);
			FileOutputStream wigtWriter = new FileOutputStream(wigtFile);
			ObjectOutputStream wigtSerializer = new ObjectOutputStream(wigtWriter);
			
			for (LanguageMapping mapping : mappings.values()) {
				switch (mapping.getType()) {
					case LanguageMapping.TYPE_ACTION:
						actsSerializer.writeObject(mapping);
						break;
						
					case LanguageMapping.TYPE_LESSON:
						lesnSerializer.writeObject(mapping);
						break;
						
					case LanguageMapping.TYPE_WIDGET:
						wigtSerializer.writeObject(mapping);
						break;
						
					default:
						Logger.logError("skipped save of mapping " + mapping);
						break;
				}
			}
			
			actsSerializer.close();
			actsWriter.close();
			lesnSerializer.close();
			lesnWriter.close();
			wigtSerializer.close();
			wigtWriter.close();
		} 
		catch (IOException | SecurityException e) {
			actsFile.delete();
			lesnFile.delete();
			wigtFile.delete();
			e.printStackTrace();
			throw new MemoryException("failed to save mappings");
		}
		
		saved = true;
		Logger.log("memory save complete");
	}
	
	public static class Lookup {
		public String token;
		public ArrayList<LanguageMapping> mappings;
		
		public Lookup(String tok, ArrayList<LanguageMapping> lms) {
			token = tok;
			mappings = lms;
		}
	}
	
	public static class MemoryException extends Exception {
		private static final long serialVersionUID = -5312985469881250543L;
		private String message;
		
		public boolean vital = false;
		
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
