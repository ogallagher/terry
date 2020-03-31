package com.terry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.security.AccessController;

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

import com.google.api.gax.core.CredentialsProvider;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.speech.v1.RecognitionAudio;
import com.google.cloud.speech.v1.RecognitionConfig;
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding;
import com.google.protobuf.ByteString;
import com.google.cloud.speech.v1.RecognizeResponse;
import com.google.cloud.speech.v1.SpeechClient;
import com.google.cloud.speech.v1.SpeechRecognitionAlternative;
import com.google.cloud.speech.v1.SpeechRecognitionResult;
import com.google.cloud.speech.v1.SpeechSettings;

import javafx.application.Platform;

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
	private static GoogleTranscribeThread gtranscriber;
	private static DeepspeechTranscribeThread dtranscriber;
	private static ReadThread reader;
	
	private static final char TRANSCRIBER_GOOGLE = 'g';
	private static final char TRANSCRIBER_DEEPSPEECH = 'd';
	private static char selectedTranscriber;
	
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
	
	static int testing = 0;
	
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
			AudioPermission audioPermission = new AudioPermission("record");
			
			try {
				//AccessController.checkPermission(audioPermission); this does not work, always throws security exception
				
				//connect to microphone
				microphone = (TargetDataLine) AudioSystem.getLine(info);
				
				//init transcriber
				selectedTranscriber = TRANSCRIBER_GOOGLE;
				
				switch (selectedTranscriber) {
					case TRANSCRIBER_GOOGLE:
						GoogleTranscribeThread.init();
						break;
						
					case TRANSCRIBER_DEEPSPEECH:
					default:
						DeepspeechTranscribeThread.init();
						break;
				}
				
				
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
		/*
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
		*/
		
		//testing
		///*
		state.set(STATE_RECORDING);
		
		state.set(STATE_TRANSCRIBING);
		
		//gtranscriber = new GoogleTranscribeThread();
		//gtranscriber.start();
		
		String[] instructions = new String[] {
				"play is a button",
				"show me where play is",
				"move mouse to x sixty five y eighty",
				"go to play",
				"take a screen shot",
				"click",
				"type a hello from terry",
				"do overlay demo one",
				"do driver demo one",
				"show state",
				"quit"
				};
		
		transcription = instructions[testing];
		testing++;
		if (testing >= instructions.length) {
			testing = 0;
		}
		
		Logger.log(transcription);
		
		state.set(STATE_DONE);
		state.set(STATE_IDLE);
		//*/
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
			
			//recording finished, pass to transcriber to get transcription
			switch (selectedTranscriber) {
				case TRANSCRIBER_GOOGLE:
					gtranscriber = new GoogleTranscribeThread();
					gtranscriber.start();
					break;
					
				case TRANSCRIBER_DEEPSPEECH:
				default:
					dtranscriber = new DeepspeechTranscribeThread();
					dtranscriber.start();
					break;
			}
		}
	}
	
	/*
	 * Sends a transcription request to google's cloud speech api using the provided google speech
	 * and general api client libraries.
	 * 
	 * Current implementation only supports transcription of short audio files (duration less than 1 minute), since
	 * it uses Synchronous Speech Recognition (instead of asynchronous). This should be all that is needed for Terry,
	 * but the api can transcribe audio up to 1 hour in length for free if done asynchronously.
	 * 
	 */
	private static class GoogleTranscribeThread extends Thread {
		private static File transcribeDir;
		private static File audioFile;
		private static SpeechClient speechClient;
		
		private static File gcloudCredentialsFile;
		private static final String GCLOUD_CREDENTIALS_PATH = "google_speech_credentials.json";
		
		public static void init() throws ScribeException {
			transcribeDir = new File(Terry.class.getResource(TRANSCRIBE_PATH).getPath());
			
			//load google cloud credentials
			gcloudCredentialsFile = new File(transcribeDir,GCLOUD_CREDENTIALS_PATH);
			try {
				CredentialsProvider credentials = FixedCredentialsProvider.create(ServiceAccountCredentials.fromStream(new FileInputStream(gcloudCredentialsFile)));
				SpeechSettings settings = SpeechSettings.newBuilder().setCredentialsProvider(credentials).build();
				
				//connect to speech file
				audioFile = new File(transcribeDir,"speech.wav");
				
				//create speech api client
				speechClient = SpeechClient.create(settings);
				
				Logger.log("google transcriber init success: " + "info");
			} 
			catch (FileNotFoundException e) {
				throw new ScribeException("google transcriber failed to load cloud credentials");
			} 
			catch (IOException e) {
				throw new ScribeException("google transcriber failed to create speech client: " + e.getMessage());
			}
		}
		
		@Override
		public void run() {
			try {
				//InputStream audio = new FileInputStream(audioFile);
				byte[] data = Files.readAllBytes(audioFile.toPath());
				ByteString audio = ByteString.copyFrom(data);
				
				RecognitionAudio speech = RecognitionAudio
											.newBuilder()
											.setContent(audio)
											.build();
				
				RecognitionConfig config = RecognitionConfig
														.newBuilder()
														.setLanguageCode("en-US")
														.setMaxAlternatives(1)
														.setModel("command_and_search")
														.setEnableAutomaticPunctuation(false)
														.build();
				
				Logger.log("scribe began transcribing");
				RecognizeResponse response = speechClient.recognize(config, speech);
				
				String transcript = "";
				for (SpeechRecognitionResult result : response.getResultsList()) {
					transcript += result.getAlternatives(0).getTranscript() + " ";
				}
				
				transcription = transcript; //no read stream needed since the call in synchronous and only updates once
				Logger.log("transcription = " + transcription);
			} 
			catch (FileNotFoundException e) {
				Logger.logError("scribe could not find audio file at " + audioFile.getAbsolutePath());
			}
			catch (IOException e) {
				Logger.logError("scribe could not parse audio file: " + e);
			}
			
			state.set(STATE_DONE); //trigger prompter to pass transcription to next module
			state.set(STATE_IDLE);
		}
	}
	
	/*
	 * Launches deepspeech to convert the speech audio file to a transcription via os/system call.
	 */
	private static class DeepspeechTranscribeThread extends Thread {
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
			
			Logger.log("deepspeech transcriber init success: \n" + cmd + "\nexecuted in: " + cmdDir);
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
		
		public ReadThread(InputStream transcriptUpdates) {
			reader = new BufferedReader(new InputStreamReader(transcriptUpdates));
		}
		
		@Override
		public void run() {
			while (!isInterrupted()) {
				try {
					String update = reader.readLine();
					
					if (update != null) {
						transcription = update;
					}
					else {
						interrupt();
					}
				} 
				catch (IOException e) {
					interrupt();
				}
			}
			
			Logger.log("transcription = " + transcription);
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
