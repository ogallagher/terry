/*
 * Author:	Owen Gallagher
 * Start:	December 2019
 */

package com.terry;

import com.terry.Driver.DriverException;

public class Terry {
	public static Prompter prompter;

	public static void main(String[] args) {
		Logger.init();
		
		prompter = new Prompter();
		prompter.init(args);
		
		try {
			Driver.init();
		}
		catch (DriverException e) {
			Logger.logError(e.getMessage());
		}
	}
}
