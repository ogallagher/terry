package com.terry;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

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
