/*
 * Author:	Owen Gallagher
 * Start:	December 2019
 * 
 * Notes:
 * 	- dictionary 
 * 		- an entry is token paired with LangMapping ids whose patterns contain that token
 * 		- only keywords and widgets can have entries; value args would not have entries
 * 		- an entry is in the form: token ref_1 ref_2 ... ref_n
 * 	- LanguageMapping/LangMap
 * 		- superclass for lessons, actions and widgets, with a pattern and mappings for keywords and args
 * 		- LangMap.id is a unique int id for each
 * - Action
 * 		- inherited member LangMap.value is an state name and value pairing as Entry<String,Object>
 * 		- args define what widget to perform the action on and how to do the action
 * - StateTransition
 * 		- member state is a String
 * 		- member execution is a DriverExecution: { abstract void Method() }
 * - State implements Entry<String,Object>
 * 		- member name is a String
 * 		- member value is an Object (observable)
 */

package com.terry;

import com.terry.Driver.DriverException;
import com.terry.Memory.MemoryException;
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
		
		InstructionClassifier.init();
		
		/*
		try {
			Memory.init();
		}
		catch (MemoryException e) {
			Logger.logError(e.getMessage());
		}
		*/
		
		LanguageMapping lm = new LanguageMapping();
		lm.setPattern("terry |[find_@query_?online)],[|[search_?for)],[look_up],)_@query],) ?and @cmd)");
		System.out.println(lm.patternDiagram());
		
		prompter = new Prompter();
		prompter.init(args);
	}
}
