package com.terry;

import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;

import javafx.application.Platform;

public class Logger {
	private static final int LOG_MAX = 200;
	private static final String FILE_NAME = "log_";
	private static final String FILE_EXT = ".txt";
	
	private static LinkedList<String> log;
	private static int logLen;
	
	private static FileReader fileReader;
	private static FileWriter fileWriter;
	
	public static void init() {
		log = new LinkedList<String>();
	}
	
	public static void logError(String error) {
		error = "e_" + error;
		log(error);
	}
	
	public static void log(String entry) {
		//add to log history
		log.add(entry);
		
		//keep below max length
		if(logLen > LOG_MAX) {
			log.remove(0);
		}
		else {
			logLen++;
		}
		
		//print entry
		
		//say entry
		
		//testing only
		System.out.println(entry);
	}
}
