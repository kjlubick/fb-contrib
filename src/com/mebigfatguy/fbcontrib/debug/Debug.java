package com.mebigfatguy.fbcontrib.debug;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Debug {

	
	private static PrintStream out;

	static {
		try {
			out = new PrintStream(new FileOutputStream("/tmp/findbugsConsole.txt"),true);
			out.println("Hello findbugs");
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		}
	}
	
	private Debug(){}

	public static void println(Object x) {
		out.println(x);
	}
	
	
	
	
}
