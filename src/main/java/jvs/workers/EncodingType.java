package jvs.workers;

/**
 * Defines available encoding types
 */
public enum EncodingType {
    /**
     * Fastest profile, lowest server load, maximum browser compatibility, poor efficiency.
     */
    MPEG_DASH_PASSTHROUGH,
    /**
     * Fast profile, medium server load, maximum browser compatibility, good efficiency.
     */
	MPEG_DASH_H264_AAC,
    /**
     * Fast profile, medium server load, good browser compatibility, good efficiency.
     */
	WEBM_DASH_VP8_VORBIS,
    /**
     * Slowest profile, medium/high server load, limited browser compatibility, maximum efficiency.
     */
	WEBM_DASH_VP9_OPUS;

	/**
	 * Description of the encoding type.
	 */
	private String description;

    /**
     * Statically assign description to each value of the enumeration.
     */
	static {
        MPEG_DASH_PASSTHROUGH.description = "MPEG-DASH (passthrough)";
        MPEG_DASH_H264_AAC.description = "MPEG-DASH (H.264/AAC)";
        WEBM_DASH_VP8_VORBIS.description = "WEBM-DASH (VP8/Vorbis)";
        WEBM_DASH_VP9_OPUS.description = "WEBM-DASH (VP9/Opus)";
    }

    /**
     * Gets the description about the encoding type.
     * @return The string that describe the encoding type.
     */
    public String getDescription() {
        return description;
    }
}




