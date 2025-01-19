package jvs.workers;

/**
 * Defines all types of worker
 */
public enum EncoderType {
	/**
	 * The encoder.
	 */
	MAIN,
	/**
	 * Generates the dash manifest.
	 */
	MPDCREATOR,
	/**
	 * Updates the dash manifest file when the input stream is closed.
	 */
	MPDFINALIZER
}
