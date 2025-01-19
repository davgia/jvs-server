package jvs.utils;

/**
 * Generic easy to use logger implementation
 */
public class Logger {
	/**
	 * Logs a normal information message.
	 * 
	 * @param message The message to log
	 */
	public static void info(final String message) {	
		System.out.println("[INFO] " + message);
	}
	
	/**
	 * Logs a warning message
	 * 
	 * @param message The message to log
	 */
	public static void warn(final String message) {	
		System.out.println("[WARNING] " + message);
	}
	
	/**
	 * Logs an error message
	 * 
	 * @param message The message to log
	 */
	public static void error(final String message) {	
		System.out.println("[ERROR] " + message);
	}
	
	/**
	 * Logs a generic message
	 * 
	 * @param message The message to log
	 */
	public static void log(final String message) {	
		System.out.println(message);
	}
}
