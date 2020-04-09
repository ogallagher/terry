package com.terry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.LinkedList;

import com.terry.Speaker.SpeakerException;

import javafx.application.Platform;

public class Logger {
	private static final String FILE_NAME = "log_";
	private static final String FILE_EXT = ".txt";
	private static final String LOG_PATH = "logs/";
	
	private static LinkedList<String> backlog;
	
	private static File logDir;
	private static FileLogger fileLogger = null;
	
	public static final char LEVEL_FILE = 0;	//only written to log file
	public static final char LEVEL_CONSOLE = 1; //written to file and displayed in console
	public static final char LEVEL_SPEECH = 2;	//written to file, displayed in console, and spoken aloud
	
	public static void init() {
		//init backlog
		backlog = new LinkedList<String>();
		
		//init log file folder
		logDir = new File(Terry.class.getResource(Terry.RES_PATH).getPath() + "/" + LOG_PATH);
		
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
				logError(e.getMessage());
			}
		}
		
		log("logger init success");
	}
	
	public static void log(String entry) {
		log(entry, LEVEL_CONSOLE);
	}
	
	public static void logError(String error) {
		logError(error, LEVEL_CONSOLE);
	}
	
	public static void logError(String error, char level) {
		log("e_" + error, level);
	}
	
	public static void log(String entry, char level) {
		//file entry
		if (fileLogger != null && fileLogger.log(entry)) {
			//create new log file
			try {
				fileLogger = new FileLogger();
			} 
			catch (IOException e) {
				logError("could not create new file logger");
			}
		}
				
		//print entry
		if (level >= LEVEL_CONSOLE) {
			if (Terry.prompter != null) {
				Platform.runLater(new ConsoleLogger(entry));
			}
			else {
				backlog.add(entry);
			}
		}
		
		//say entry
		if (level >= LEVEL_SPEECH) {
			try {
				Speaker.speak(entry);
			} 
			catch (SpeakerException e) {
				logError(e.getMessage(), LEVEL_CONSOLE);
				Speaker.setEnabled(false);
			}
		}
		
		//testing only
		System.out.println(entry);
	}
	
	public static void emptyBacklog() {
		while (!backlog.isEmpty()) {
			Platform.runLater(new ConsoleLogger(backlog.getFirst()));
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
			
			String when = start.toString().toLowerCase().replaceAll("[\\s:]", "-");
			logFile = new File(logDir.getAbsolutePath() + "/" + FILE_NAME + when + FILE_EXT);
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
