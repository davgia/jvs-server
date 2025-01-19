package jvs.command;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Stores ffmpeg command for both stream encoding and manifest generation.
 */
public class Commands {

    private List<String> encodeCommands;
    private List<String> manifestCommands;

    /**
     * Default constructor
     */
    public Commands() {
        encodeCommands = new LinkedList<String>(){};
        manifestCommands = new LinkedList<String>(){};
    }

    /* GETTERS */

    /**
     * Gets the ffmpeg commands to encode a stream.
     * @return List of all ffmpeg commands to encode the stream
     */
    public List<String> getEncodeCommands() {
        return encodeCommands;
    }

    /**
     * Gets the ffmpeg commands to generate dash manifest.
     * @return List of all ffmpeg commands to generate manifest
     */
    public List<String> getManifestCommands() {
        return manifestCommands;
    }

    /* METHODS TO APPEND NEW COMMANDS */

    /**
     * Appends command to the current list of manifest commands.
     * @param encodeCommands Array of commands to add to the current commands
     */
    public void appendEncodeCommands(String... encodeCommands) {
        this.encodeCommands.addAll(Arrays.asList(encodeCommands));
    }

    /**
     * Appends command to the current list of manifest commands.
     * @param encodeCommands List of commands to add to the current commands
     */
    public void appendEncodeCommands(List<String> encodeCommands) {
        this.encodeCommands.addAll(encodeCommands);
    }

    /**
     * Appends command to the current list of manifest commands.
     * @param manifestCommands Array of commands to add to the current commands
     */
    public void appendManifestCommands(String...  manifestCommands) {
        this.manifestCommands.addAll(Arrays.asList(manifestCommands));
    }

    /**
     * Appends command to the current list of manifest commands.
     * @param manifestCommands List of commands to add to the current commands
     */
    public void appendManifestCommands(List<String>  manifestCommands) {
        this.manifestCommands.addAll(manifestCommands);
    }
}
