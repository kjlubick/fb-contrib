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

	/**
	 * Like println, but will print PC, if it's passed in
	 * 
	 * e.g. Debug.println(getPC(), "Hello world");
	 * will print
	 * [PC:42] Hello world
	 * 
	 * @param pc
	 * @param obj
	 */
	public static void println(int pc, Object obj) {
		out.printf("[PC:%d] %s%n", pc,obj);
	}

	/**
	 * Like printf, but appends the current pc (be sure to include this in the format objects)
	 * 
	 * e.g. Debug.printf_pc("%1.3f%n", getPC(), getFloat());
	 * will print
	 * [PC:42] 37.664\n    //assuming PC is currently 42
	 * 
	 * @param formatString
	 * @param objects
	 */
	public static void printf_pc(String formatString, Object... objects) {
		out.printf("[PC:%d] "+formatString, objects);
	}
	
	
	
	
}
