package com.terry;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Speaker {
	//commands
	private static String cmdWin = "cmd.exe /C echo \"<transcript>\" | <bin> --volume <volume> --name <voice> --rate <speed>"; //volume=0..100, rate=-10..10
	private static String cmdMac = "<bin>  --voice=<voice> --rate=<speed> <transcript>"; //rate=80...350wpm, 
	private static String cmd;
	
	//args (numeric ranges are 0..1, to be converted per os)
	private static String binWin = "voice.exe"; //converted to absolute path later
	private static String binMac = "say";
	private static float volume = 0.25f; //only available in win
	private static String voice = null; //null is default
	private static float speed = 0.5f;	
	
	private static final String SPEECH_PATH = Terry.RES_PATH + "speech/";
	private static File cmdDir; 
	
	private static String cmdListVoicesWin = binWin + " --list"; //converted to absolute path later
	private static String cmdListVoicesMac = binMac + " --voice=?";
	private static Pattern regexVoiceNameWin = Pattern.compile("^\".+\"");	//"Name Name" - age,gender,language
	private static Pattern regexVoiceNameMac = Pattern.compile("^\\w+");	//Name   language: "message"
	
	private static ArrayList<String> voices;
	private static ArrayList<String> voicesInfo;
	
	private static boolean enabled = true;
	
	public static void init() throws SpeakerException {
		//set cmdDir
		File cmdDir = new File(Terry.class.getResource(SPEECH_PATH).getPath());
		
		//convert binWin to absolute path
		binWin = cmdDir.getAbsolutePath() + "\\" + binWin;
		cmdListVoicesWin = cmdDir.getAbsolutePath() + "/" + cmdListVoicesWin;
		
		if (Terry.os == Terry.OS_WIN && !cmdDir.exists()) {
			//fail in windows if cannot use cmdDir
			throw new SpeakerException("could not find speech program in " + cmdDir.getAbsolutePath());
		}
		
		//set cmd
		updateCmd();
		
		//set available voice list
		voices = new ArrayList<String>();
		voicesInfo = new ArrayList<String>();
		try {
			String cmdListVoices = cmdListVoicesMac;
			Pattern regexVoiceName = regexVoiceNameMac;
			if (Terry.os == Terry.OS_WIN) {
				cmdListVoices = cmdListVoicesWin;
				regexVoiceName = regexVoiceNameWin;
			}
			
			Process process = Runtime.getRuntime().exec(cmdListVoices, null, cmdDir);
			BufferedReader voicesReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			
			Logger.log("getting available voices for speaker");
			process.waitFor();
			
			String voiceInfo = voicesReader.readLine();
			String voiceName;
			Matcher matcher;
			while (voiceInfo != null) {
				matcher = regexVoiceName.matcher(voiceInfo);
				
				if (matcher.find()) {
					voiceName = matcher.group().toLowerCase().trim();
					voiceInfo = voiceInfo.toLowerCase();
					
					voices.add(voiceName);
					voicesInfo.add(voiceInfo);
					
					Logger.log(voiceInfo);
				}
				
				voiceInfo = voicesReader.readLine();
			}
		} 
		catch (IOException e) {
			Logger.logError("speaker unable to determine available voices");
			Logger.logError(e.getMessage());
		} 
		catch (InterruptedException e) {
			Logger.logError("speaker interrupted when reading available voices");
		}
		
		Logger.log("speaker init success");
	}
	
	private static void updateCmd() {
		if (Terry.os == Terry.OS_MAC) {
			//bin
			cmd = cmdMac.replace("<bin>", binMac);
			
			//voice
			if (voice == null) {
				cmd = cmd.replace("--voice=<voice> ", ""); //remove arg
			}
			else {
				cmd = cmd.replace("<voice>", voice);
			}
			
			//speed~80..350wpm
			String speedArg = String.valueOf((int) ((speed*270f) + 50f));
			cmd = cmd.replace("<speed>", speedArg);
		}
		else if (Terry.os == Terry.OS_WIN) {
			//bin
			cmd = cmdWin.replace("<bin>", binWin);
			
			//volume=0..100
			String volumeArg = String.valueOf((int)(volume*100f));
			cmd = cmd.replace("<volume>", volumeArg);
			
			//voice
			if (voice == null) {
				cmd = cmd.replace("--name <voice> ", ""); //remove arg
			}
			else {
				
			}
			
			//speed=-10..10
			String speedArg = String.valueOf((int) ((speed*20f) - 10f));
			cmd = cmd.replace("<speed>", speedArg);
		}
	}
	
	public static void speak(String transcript) throws SpeakerException {
		if (enabled) {
			try {
				Runtime.getRuntime().exec(cmd.replace("<transcript>", transcript), null, cmdDir);
			} 
			catch (IOException e) {
				throw new SpeakerException("speaker failed to speak");
			}
		}
	}
	
	public static void setVolume(float v) throws SpeakerException {
		if (Terry.os == Terry.OS_WIN) {
			if (v >= 0f && v <= 1f) {
				//update arg
				volume = v;
				updateCmd();
			}
			else {
				throw new SpeakerException("cannot set speaker volume below zero or above one");
			}
		}
		else if (Terry.os == Terry.OS_MAC) {
			throw new SpeakerException("cannot change volume on mac");
		}
	}
	
	public static void setVoice(String v) throws SpeakerException {
		if (voices.contains(v)) {
			voice = v;
			updateCmd();
		}
		else {
			throw new SpeakerException("speaker voice " + v + " not in list of available voices");
		}
	}
	
	public static void setSpeed(float s) throws SpeakerException {
		if (s >= 0f && s <= 1f) {
			//update arg
			speed = s;
			updateCmd();
		}
		else {
			throw new SpeakerException("cannot set speaker speed below zero or above one");
		}
	}
	
	public static void setEnabled(boolean enable) {
		enabled = enable;
		
		if (enabled) {
			Logger.log("speaker enabled", Logger.LEVEL_CONSOLE);
		}
		else {
			Logger.log("speaker disabled", Logger.LEVEL_CONSOLE);
		}
	}
	
	public static class SpeakerException extends Exception {
		private static final long serialVersionUID = -9148568036890024500L;
		
		public SpeakerException(String message) {
			super(message);
		}
	}
}
