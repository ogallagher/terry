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
	private static LinkedList<String> backlog;
	private static int logLen;
	
	private static FileReader fileReader;
	private static FileWriter fileWriter;
	
	public static void init() {
		log = new LinkedList<String>();
		backlog = new LinkedList<String>();
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
		if (Terry.prompter != null) {
			Platform.runLater(new ConsoleLogger(entry));
		}
		else {
			backlog.add(entry);
		}
		
		//say entry
		
		//testing only
		System.out.println(entry);
	}
	
	public static void emptyBacklog() {
		while (!backlog.isEmpty()) {
			log(backlog.getFirst());
			backlog.removeFirst();
		}
	}
	
	private static class ConsoleLogger extends Thread {
		String news = null;
		
		public ConsoleLogger(String news) {
			this.news = news;
		}
		
		@Override
		public void run() {
			Terry.prompter.consoleLog(news);
		}
	}
}
