package jvs.config;

import io.vertx.core.json.JsonObject;
import jvs.utils.Utils;

import java.util.*;
import java.util.regex.Pattern;

import static jvs.Constants.CONFIG_KEYS.*;

/**
 * Stores all configurations of the program
 */
public class Configuration {

    private String address;
    private final int port;
    private final String streamsPath;
    private final String assetsPath;
    private final String remoteStreamsPath;
    private final String remoteAssetsPath;
    private final String remoteRootAPIPath;
    private final String ffmpegPath;
    private final String ffprobePath;
    private final int listeningPort;
    private final String announcePath;
    private final HashMap<String, Pattern> patterns;
    private final HashMap<String, String> defaults;
    private final HashMap<String, List<String>> commands;

    /**
     * Constructor that initialize field of this class parsing an input json
     * @param json The json where to parse configuration.
     */
    public Configuration(JsonObject json) {

        address = json.getString(ADDRESS);
        port = json.getInteger(PORT);
        streamsPath = json.getString(STREAMS_PATH);
        assetsPath = json.getString(ASSETS_PATH);
        remoteStreamsPath = json.getString(REMOTE_STREAMS_PATH);
        remoteAssetsPath = json.getString(REMOTE_ASSETS_PATH);
        remoteRootAPIPath = json.getString(REMOTE_ROOT_API_PATH);
        ffmpegPath = json.getString(FFMPEG_PATH);
        ffprobePath = json.getString(FFPROBE_PATH);
        announcePath = json.getString(ANNOUNCE_PATH);
        listeningPort = json.getInteger(LISTENING_PORT, -1);

        commands = new HashMap<>();
        patterns = new HashMap<>();
        defaults = new HashMap<>();

        json.getJsonObject(PATTERNS).getMap()
                .forEach((k,v) -> patterns.put(k, Pattern.compile(v.toString(), Pattern.CASE_INSENSITIVE)));

        json.getJsonObject(DEFAULTS).getMap()
                .forEach((k,v) -> defaults.put(k, v.toString()));

        json.getJsonObject(COMMANDS).getMap()
                .forEach((k,v) -> commands.put(k, Arrays.asList(v.toString().split("\\s+"))));
    }

    /**
     * Sets the server address.
     * @return The IPV4 server address.
     */
    public void setAddress(final String address) {
        this.address = address;
    }

    /**
     * Gets the server address.
     * @return The IPV4 server address.
     */
    public String getAddress() {
        return address;
    }

    /**
     * Gets the server listening port.
     * @return The listening port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Gets the local path where all streams are stored (the path is sanitized from trailing '/').
     * @return The local path of the streams folder.
     */
    public String getStreamsPath() {
        return Utils.trimTrailingSuffix(streamsPath, "/");
    }

    /**
     * Gets the local path of all web application assets (the path is sanitized from trailing '/').
     * @return The local path where all web application assets are stored.
     */
    public String getAssetsPath() {
        return Utils.trimTrailingSuffix(assetsPath, "/");
    }

    /**
     * Gets the remote path of the streams (the path is sanitized from trailing '/').
     * @return The remote path of the streams.
     */
    public String getRemoteStreamsPath() {
        return Utils.trimTrailingSuffix(remoteStreamsPath, "/");
    }

    /**
     * Gets the remote path of the assets (the path is sanitized from trailing '/').
     * @return The remote path of the assets.
     */
    public String getRemoteAssetsPath() {
        return Utils.trimTrailingSuffix(remoteAssetsPath, "/");
    }

    /**
     * Gets the remote root path of the REST API (the path is sanitized from trailing '/').
     * @return The remote root path of the REST API.
     */
    public String getRemoteRootAPIPath() {
        return Utils.trimTrailingSuffix(remoteRootAPIPath, "/");
    }

    /**
     * Gets the location of the ffmpeg executable.
     * @return The ffmpeg path.
     */
    public String getFfmpegPath() {
        return ffmpegPath;
    }

    /**
     * Gets the location of the ffprobe executable.
     * @return The ffprobe path.
     */
    public String getFfprobePath() {
        return ffprobePath;
    }

    /**
     * Gets the listening port of ffmpeg when running as RTSP server.
     * @return The ffmpeg listening port for incoming RTSP announces.
     */
    public int getListeningPort() {
        return listeningPort;
    }

    /**
     * Gets the path where ffmpeg can receive client announces when running
     * as RTSP server (the path is sanitized from trailing and leading '/').
     * @return The ffmpeg listening path for incoming RTSP announces.
     */
    public String getAnnouncePath() {
        return announcePath
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    /**
     * Gets the regex pattern relative to the inpuyt key.
     * @param key The key of the pattern.
     * @return The regex pattern.
     */
    public Pattern getPattern(final String key) {
        if (patterns.containsKey(key)) {
            return patterns.get(key);
        }
        return null;
    }

    /**
     * Gets the default value relative to the input key.
     * @param key The key of the default.
     * @return The default value as a string.
     */
    public String getDefaultValue(final String key) {
        if (defaults.containsKey(key)) {
            return defaults.get(key);
        }
        return "";
    }

    /**
     * Gets the command relative to the input key.
     * @param key The key of the command.
     * @return The commands as list of string.
     */
    public List<String> getCommands(final String key) {
        if (commands.containsKey(key)) {
            return commands.get(key);
        }
        return new ArrayList<>();
    }
}
