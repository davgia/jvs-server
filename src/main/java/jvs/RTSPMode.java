package jvs;

/**
 * Defines all types of RTSP modes in which ffmpeg can be configured
 */
public enum RTSPMode {
    /**
     *  Server, waits for client to announce his stream.
     */
    SERVER,
    /**
     * Client, play stream broadcasted by another server.
     */
    CLIENT
}
