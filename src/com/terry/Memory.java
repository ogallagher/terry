package com.terry;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class Memory {
	private static final String MEMORY_PATH = Terry.RES_PATH + "dict/";
	private static final String DICT_FILE = "dict.txt";
	
	private static HashMap<String,ArrayList<LanguageMapping>> dictionary; //token,references
	
	public static void init() throws MemoryException {
		dictionary = new HashMap<String,ArrayList<LanguageMapping>>();
		
		//TODO init from dict files
		File memDir = new File(Terry.class.getResource(MEMORY_PATH).getPath());
		File dictFile = new File(memDir, DICT_FILE);
		if (!dictFile.exists()) {
			try {
				dictFile.createNewFile();
			}
			catch (IOException e) {
				throw new MemoryException("could not create blank dict index file at " + dictFile.getAbsolutePath());
			}
		}
		
		try {
			Scanner dictReader = new Scanner(dictFile);
			String[] line;
			String token,ref;
			
			while (dictReader.hasNextLine()) {
				line = dictReader.nextLine().split(" ");
				token = line[0];
				
				ArrayList<LanguageMapping> refs = new ArrayList<LanguageMapping>();
				for (int i=1; i<line.length; i++) {
					ref = line[i];
					
					dictionary.put(token, refs);
				}
			}
			
			dictReader.close();
		} 
		catch (FileNotFoundException e) {
			throw new MemoryException("dict index file not found at " + dictFile.getAbsolutePath());
		}
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
