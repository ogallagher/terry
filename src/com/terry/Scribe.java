package com.terry;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioPermission;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

import javafx.application.Platform;
import javafx.stage.Stage;

public class Scribe {
	/*
	 * variation of wav format:
	 * 	- signed int pcm
	 * 	- 16 kHz sample rate (max = 44.1)
	 * 	- 2 byte sample size
	 * 	- mono (1 channel)
	 * 	- frame size = sample size (sample_size * num_channels)
	 * 	- frame rate = sample rate
	 */
	private static final AudioFormat FORMAT_WAV = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 16000, 16, 1, 2, 16000, false);
	
	private static TargetDataLine microphone;
	private static int microphoneBufferSize; 
	
	private static RecordThread recorder;
	private static TranscribeThread transcriber;
	private static ReadThread reader;
	
	private static File speechDir;
	private static File speechFile; //temporary wav file, deleted after successful transcription
	private static final String TRANSCRIBE_PATH = Terry.RES_PATH + "transcription/";
	private static final String SPEECH_FILE = "speech.wav";
	
	private static String transcription;
	
	public static final char STATE_IDLE = 0;
	public static final char STATE_RECORDING = 1;
	public static final char STATE_TRANSCRIBING = 2;
	public static final char STATE_DONE = 3;
	public static CharProperty state = new CharProperty(STATE_IDLE);
	
	public static void init() throws ScribeException {
		state = new CharProperty(STATE_IDLE);
		
		DataLine.Info info = new DataLine.Info(TargetDataLine.class, FORMAT_WAV); // format is an AudioFormat object
		Logger.log(info.toString());
		
		speechDir = new File(Terry.class.getResource(TRANSCRIBE_PATH).getPath());
		speechFile = new File(speechDir,SPEECH_FILE);
		
		if (!AudioSystem.isLineSupported(info)) {
		    throw new ScribeException("microphone not supported");
		}
		else {
			try {
				//connect to microphone
				microphone = (TargetDataLine) AudioSystem.getLine(info);
				AudioPermission audioPermission = new AudioPermission("record");
				
				//init transcriber
				TranscribeThread.init();
				
				Logger.log("scribe init success");
			}
			catch (LineUnavailableException e) {
				throw new ScribeException("microphone not available");
			}
			catch (SecurityException e) {
				throw new ScribeException("not allowed to use system mic");
			}
		}
	}
	
	public static void start() throws ScribeException {
		try {
			//open the line
		    microphone.open(FORMAT_WAV); //opens input stream with default buffer size
		    microphoneBufferSize = microphone.getBufferSize();
		    
		    //record audio file for transcription
		    recorder = new RecordThread();
		    recorder.start();
		    
		    //TODO recording ends on detected silence, unexpected interruption, or Prompter.intercom click
		} 
		catch (LineUnavailableException e) {
			throw new ScribeException("microphone not available");
		}
		catch (SecurityException e) {
			throw new ScribeException("not allowed to use system mic");
		}
	}
	
	public static void stop() {
		recorder.quit();
	}
	
	public static String getTranscription() {
		return transcription;
	}
	
	private static class RecordThread extends Thread {		
		@Override
		public void run() {
			//delete old temporary speech file if exists
			if (speechFile.exists()) {
				speechFile.delete();
			}
			
			try {
				speechFile.createNewFile();
				Logger.log("created file " + speechFile.getAbsolutePath());
			} 
			catch (IOException e) {
				Logger.logError("scribe could not create speech file:\n" + speechFile.getAbsolutePath());
			}
			
			microphone.start();
			
			AudioInputStream speechStream = new AudioInputStream(microphone);
			
			try {
				Logger.log("scribe started listening");
				state.set(STATE_RECORDING);
				AudioSystem.write(speechStream, AudioFileFormat.Type.WAVE, speechFile);
			}
			catch (IOException e) {
				Logger.logError("scribe could not write to speech file");
			}
		}
		
		/*
		 * quick way to allow other threads to interrupt this one without throwing
		 * an access exception.
		 */
		public void quit() {
			microphone.stop();
			Logger.log("scribe stopped listening");
			state.set(STATE_TRANSCRIBING);
			
			//recording finished, pass to deepspeech to get transcription
			transcriber = new TranscribeThread();
			transcriber.start();
		}
	}
	
	/*
	 * Launches deepspeech to convert the speech audio file to a transcription via os/system command.
	 */
	private static class TranscribeThread extends Thread {
		private static String cmd = "<deepspeech> --model <model> --lm <lm> --trie <trie> --stream <stream> --beam_width <beam_width> --audio <audio>";
		private static String deepspeech = "./deepspeech_mac.sh";
		private static String model = "models/output_graph.pbmm";
		private static String lm = "models/lm.binary";
		private static String trie = "models/trie";
		private static String stream = "320";
		private static String beamWidth = "28";
		private static String audio = "speech.wav";
		
		private static File cmdDir;
		
		public static void init() {
			//default settings are for mac
			if (Terry.os == Terry.OS_WIN) {
				String argPrefix = "/";
				String pathDelim = "\\";
			}
			
			cmdDir = new File(Terry.class.getResource(TRANSCRIBE_PATH).getPath());
			
			cmd = cmd.replace("<deepspeech>", deepspeech)
					 .replace("<model>", model)
					 .replace("<lm>", lm)
					 .replace("<trie>", trie)
					 .replace("<stream>", stream)
					 .replace("<beam_width>", beamWidth)
					 .replace("<audio>", audio);
			
			Logger.log("scribe transcriber init success: \n" + cmd + "\nexecuted in: " + cmdDir);
		}
		
		@Override
		public void run() {
			Process process;
			if (Terry.os == Terry.OS_MAC) {
				try {
					process = Runtime.getRuntime().exec(cmd, null, cmdDir);
					Logger.log("scribe started transcribing");
					
					reader = new ReadThread(process.getInputStream());
					reader.start();
					
					int exitCode = process.waitFor();
					
					if (exitCode == 0) {
						Logger.log("transcription complete");
					}
					else {
						Logger.logError("transcription failed with exit code " + exitCode);
					}
				}
				catch (IOException e) {
					Logger.logError("could not execute os transcription cmd");
				} 
				catch (InterruptedException e) {
					Logger.logError("transcription was interrupted");
				}
			}
			else if (Terry.os == Terry.OS_WIN) {
				Logger.logError("scribe does not support windows yet");
			}
		}
	}
	
	/*
	 * Reads the progessive transcription updates from deepspeech.
	 */
	private static class ReadThread extends Thread {
		private BufferedReader reader;
		
		public ReadThread(InputStream stdout) {
			reader = new BufferedReader(new InputStreamReader(stdout));
		}
		
		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					transcription = reader.readLine();
					
					if (transcription != null) {
						Logger.log("transcription = " + transcription);
					}
					else {
						interrupt();
					}
				} 
				catch (IOException e) {
					interrupt();
				}
			}
			
			state.set(STATE_DONE); //trigger prompter to pass transcription to next module
			state.set(STATE_IDLE);
		}
	}
	
	public static class ScribeException extends Exception {
		private static final long serialVersionUID = 5174766874051468658L;
		private String message;

		public ScribeException(String message) {
			this.message = message;
		}
		
		public ScribeException() {
			this.message = "scribe failed for unknown reason";
		}
		
		@Override
		public String getMessage() {
			return message;
		}
	}
}
