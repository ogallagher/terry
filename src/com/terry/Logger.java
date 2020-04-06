package com.terry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import javafx.application.Platform;

public class Logger {
	private static final int LOG_MAX = 200;
	private static final String FILE_NAME = "log_";
	private static final String FILE_EXT = ".txt";
	private static final String LOG_PATH = "logs/";
	
	private static LinkedList<String> log;
	private static LinkedList<String> backlog;
	private static int logLen;
	
	private static File logDir;
	private static FileLogger fileLogger = null;
	
	public static void init() {
		//init log data structures
		log = new LinkedList<String>();
		backlog = new LinkedList<String>();
		
		//init log file folder
		File resDir = new File(Terry.class.getResource(Terry.RES_PATH).getPath());
		logDir = new File(resDir, LOG_PATH);
		
		//create logs directory if doesn't exist
		try {
			logDir.mkdir();
		} 
		catch (SecurityException e) {
			logError("could not create logs directory at " + logDir.getAbsolutePath());
		}
		
		if (logDir.exists()) {
			try {
				fileLogger = new FileLogger();
			} 
			catch (IOException e) {
				logError("could not create new file logger");
			}
		}
		
		log("logger init success");
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
			
			//testing only
			System.out.println(entry);
		}
		else {
			backlog.add(entry);
		}
		
		//file entry
		if (fileLogger != null) {
			if (fileLogger.log(entry)) {
				//create new log file
				try {
					fileLogger = new FileLogger();
				} 
				catch (IOException e) {
					logError("could not create new file logger");
				}
			}
		}
		
		//say entry
	}
	
	public static void emptyBacklog() {
		while (!backlog.isEmpty()) {
			log(backlog.getFirst());
			backlog.removeFirst();
		}
	}
	
	public static void save() {
		if (fileLogger != null) {
			log("logs save complete");
			fileLogger.save();
		}
	}
	
	private static class ConsoleLogger extends Thread {
		String news = null;
		
		public ConsoleLogger(String news) {
			this.news = news;
		}
		
		@Override
		public void run() {
			Prompter.consoleLog(news);
		}
	}
	
	private static class FileLogger {
		private static final int LOGFILE_MAX = 500; //max lines in a single log file
		
		private Date start;
		private File logFile;
		private StringBuilder log;
		private int numEntries;
		
		public FileLogger() throws IOException {
			start = new Date(System.currentTimeMillis());
			
			String when = start.toString().toLowerCase().replace(' ', '-');
			logFile = new File(logDir, FILE_NAME + when + FILE_EXT);
			logFile.createNewFile();
			
			log = new StringBuilder(LOGFILE_MAX*10);
			numEntries = 0;
		}
		
		//return value = whether to create new log file; whether current log file hit max size
		public boolean log(String entry) {
			log.append(entry + "\n");
			numEntries++;
			
			if (numEntries > LOGFILE_MAX) {
				save();
				
				return true;
			}
			else {
				return false;
			}
		}
		
		public void save() {
			new Runnable() {
				public void run() {
					try {
						FileWriter writer = new FileWriter(logFile);
						writer.write(log.toString());
						writer.close();
					} 
					catch (IOException e) {
						logError("failed to write to log file " + logFile.getName());
					}
				}
			}.run();
		}
	}
}
