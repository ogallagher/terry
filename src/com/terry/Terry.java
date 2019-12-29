/*
 * Author:	Owen Gallagher
 * Start:	December 2019
 */

package com.terry;

import com.terry.Driver.DriverException;
import com.terry.Scribe.ScribeException;

public class Terry {
	public static Prompter prompter;
	
	public static final String RES_PATH = "res/";
	
	public static final char OS_MAC = 1;
	public static final char OS_WIN = 2;
	public static final char OS_OTHER = 3;
	public static char os;

	public static void main(String[] args) {
		Logger.init();
		
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.startsWith("win")) {
			os = OS_WIN;
			Logger.log("detected win os");
		}
		else if (osName.startsWith("mac")) {
			os = OS_MAC;
			Logger.log("detected mac os");
		}
		else {
			os = OS_OTHER;
			Logger.logError("detected unsupported os: " + osName);
			System.exit(1);
		}
		
		try {
			Scribe.init();
		}
		catch (ScribeException e) {
			Logger.logError(e.getMessage());
		}
		
		try {
			Driver.init();
		}
		catch (DriverException e) {
			Logger.logError(e.getMessage());
		}
		
		prompter = new Prompter();
		prompter.init(args);
	}
}
