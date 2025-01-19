package jvs.command;

import jvs.Constants;
import jvs.RTSPMode;
import jvs.stream.Stream;
import jvs.stream.StreamInfo;
import jvs.stream.track.AudioTrackInfo;
import jvs.stream.track.TrackType;
import jvs.stream.track.VideoTrackInfo;
import jvs.config.ConfigManager;
import jvs.config.Configuration;
import jvs.utils.Logger;
import jvs.utils.Utils;
import jvs.workers.EncodingType;

import java.util.*;
import static jvs.Constants.DEFAULTS.*;
import static jvs.Constants.COMMANDS.*;

/**
 * Generates command line for the encoder
 */
public class CommandsGenerator {

    /**
     * The general information about the input stream.
     */
    private Stream stream;
    /**
     * The detailed information of each track in the input stream.
     */
    private StreamInfo streamInfo;

    /* Variables used to create ffmpeg command line to generate dash manifest */
    private int trackCount = 0; //total numer of tracks
    private List<String> manifestCommands = new ArrayList<>(); //completed command line to generate manifest
    private List<String> mapCommands = new ArrayList<>(); //map command to define inputs for the manifestCommands
    private List<String> extraArgs = new ArrayList<String>(); //extra ffmpeg arguments
    private StringBuilder adaptationSets = new StringBuilder(); //the disposition of the input track in the manifest

    /**
     * Default constructor
     */
    public CommandsGenerator() {
        this.stream = null;
        this.streamInfo = null;
    }

    /**
     * Constructor #1
     * @param stream The stream used to generate the command line for the encoder
     */
    public CommandsGenerator(final Stream stream) {
        this();
        this.stream = stream;
    }

    /**
     * Constructor #2
     * @param stream The stream used to generate the command line for the encoder
     * @param streamInfo The stream informations used to generate the command line for the encoder
     */
    public CommandsGenerator(final Stream stream, final StreamInfo streamInfo) {
        this(stream);
        this.streamInfo = streamInfo;
    }

    /**
     * Sets the stream object to use to generate the commands.
     * @param stream The input stream
     */
    public void setStream(final Stream stream) {
        this.stream = stream;
    }

    /**
     * Sets the streaminfo object to use to generate the commands.
     * @param streamInfo The input stream information
     */
    public void setStreamInfo(final StreamInfo streamInfo) {
        this.streamInfo = streamInfo;
    }

    /**
     * Sets extra ffmpeg arguments signaled by the client.
     * @param extraArgs List of extra arguments to pass to ffmpeg.
     */
    public void setExtraArgs(final List<String> extraArgs) {
        this.extraArgs = extraArgs;
    }

    /**
     * Generates the list of commands to pass to the encoder using information
     * about all tracks in the input stream and the type of encode specified.
     * @param type The type of encoding
     * @param mode The ffmpeg RTSP mode
     * @return The list of command line to pass to the encoder
     */
    public Commands generateCommands(final EncodingType type, final RTSPMode mode) {

        //valid for all types of encode
        Commands commands = new Commands();

        if (type == EncodingType.MPEG_DASH_PASSTHROUGH) {
            commands.appendEncodeCommands(ConfigManager.getConfig().getCommands(ADVOPT_FFMPEG));

            //add rtsp server ffmpeg commands if the rstp mode is set to server
            if (mode == RTSPMode.SERVER) {
                commands.appendEncodeCommands(ConfigManager.getConfig().getCommands(ADVOPT_RTSPSERVER));
                commands.appendEncodeCommands("-i", generateServerAnnounceUrl(), "-f", "dash");
            } else {
                commands.appendEncodeCommands("-i", stream.getInputUrl(), "-f", "dash");
            }
            commands.appendEncodeCommands("-min_seg_duration", ConfigManager.getConfig().getDefaultValue(Constants.DEFAULTS.MIN_SEGMENT_DURATION));
            commands.appendEncodeCommands("-c:v", "copy", "-c:a", "copy", "-sn");
            commands.appendEncodeCommands(ConfigManager.getConfig().getCommands(ADVOPT_MPEGDASH));
            commands.appendEncodeCommands("manifest.mpd");
        } else {

            //encode the first video if present else skip encoding
            if (streamInfo.getVideoTracks().size() > 0) {

                VideoTrackInfo vti = streamInfo.getVideoTracks().get(0);
                String fps = String.format("%d",(int)Math.ceil(vti.getFrameRate()));

                //input declaration and ffmpeg flags
                commands.appendEncodeCommands(ConfigManager.getConfig().getCommands(ADVOPT_FFMPEG));

                //add rtsp server ffmpeg commands if the rstp mode is set to server
                if (mode == RTSPMode.SERVER) {
                    commands.appendEncodeCommands(ConfigManager.getConfig().getCommands(ADVOPT_RTSPSERVER));
                    commands.appendEncodeCommands("-r", fps, "-i", generateServerAnnounceUrl());
                } else {
                    commands.appendEncodeCommands("-r", fps, "-i", stream.getInputUrl());
                }

                //add extra client ffmpeg arguments
                if (extraArgs != null) {
                    commands.appendEncodeCommands(extraArgs);
                }

                //add command to declare input video track and encode it
                commands.appendEncodeCommands(generateTrackCommand(type, TrackType.VIDEO, 0));

                //audio streams
                if (streamInfo.getAudioTracks().size() > 0) {
                    //add command to declare input audio track and encode it
                    commands.appendEncodeCommands(generateTrackCommand(type, TrackType.AUDIO, 0)); //encode audio
                }

                //generate complete webm dash manifest command based on all encoded tracks
                if (type != EncodingType.MPEG_DASH_H264_AAC) {
                    commands.appendManifestCommands(generateManifestCommand());
                    commands.appendManifestCommands("manifest.mpd");
                } else {
                    //generate arguments for mpeg dash output
                    commands.appendEncodeCommands("-f", "dash", "-min_seg_duration",
                            ConfigManager.getConfig().getDefaultValue(Constants.DEFAULTS.MIN_SEGMENT_DURATION));
                    commands.appendEncodeCommands(ConfigManager.getConfig().getCommands(ADVOPT_MPEGDASH));
                    commands.appendEncodeCommands("manifest.mpd");
                }
            } else {
                Logger.warn("Unable to generate commands because no video track is present for stream with id: " + stream.getID());
            }
        }

        if (Constants.DEBUG_MODE) {
            Logger.log("Encode commands:");
            Logger.log(String.join(" ", commands.getEncodeCommands()));

            if (type != EncodingType.MPEG_DASH_H264_AAC && type != EncodingType.MPEG_DASH_PASSTHROUGH) {
                Logger.log("Manifest commands:");
                Logger.log(String.join(" ", commands.getManifestCommands()));
            }
        }

        return commands;
    }

    /**
     * Generates command line for a specific track type in the given index
     * @param encodingType The selected type of encoding
     * @param type The track type to consider
     * @param index The index of the track relative to all track of the same type
     * @return The array of commands to encode the specific track
     */
    private List<String> generateTrackCommand(final EncodingType encodingType, final TrackType type, final int index) {

        Configuration config = ConfigManager.getConfig();

        switch (encodingType) {
            case WEBM_DASH_VP8_VORBIS:

                if (type == TrackType.AUDIO) {

                    Optional<AudioTrackInfo> audioTrackInfo = streamInfo.getAudioTrackAtIndex(index);

                    if (audioTrackInfo.isPresent()) {

                        int trackIndex = audioTrackInfo.get().getIndex();

                        //add track to manifest command generation
                        addTrackToManifest(TrackType.AUDIO, trackIndex);

                        List<String> commands = new ArrayList<>(Arrays.asList("-map", "0:" + trackIndex, "-c:a","libvorbis", "-b:a",
                                config.getDefaultValue(VORBIS_BITRATE)));
                        commands.addAll(config.getCommands(ADVOPT_VORBIS));
                        commands.addAll(Arrays.asList("-f", "webm_chunk", "-audio_chunk_duration", config.getDefaultValue(CHUNK_DURATION),
                                "-header", "audio_" + trackIndex + ".hdr", "-chunk_start_index", "1", "audio_" + trackIndex + "_%d.chk"));

                        return commands;
                    }
                } else if (type == TrackType.VIDEO){

                    Optional<VideoTrackInfo> videoTrackInfo = streamInfo.getVideoTrackAtIndex(index);

                    if (videoTrackInfo.isPresent()) {

                        VideoTrackInfo vti = videoTrackInfo.get();

                        int trackIndex = vti.getIndex();
                        String pixFmt = vti.getPixelFormat().isEmpty() ? config.getDefaultValue(PIXEL_FORMAT) : vti.getPixelFormat();
                        String res = vti.getWidth() + "x" + vti.getHeight();

                        //compute ideal keyframes number (framerate per seconds * number of seconds per chunk)
                        Double chunkDuration = Double.parseDouble(config.getDefaultValue(CHUNK_DURATION)) / 1000; //seconds
                        String keyInt = String.format("%d", Utils.roundEven(vti.getFrameRate() * chunkDuration)); //keyint_min and g

                        //add track to manifest command generation
                        addTrackToManifest(TrackType.VIDEO, trackIndex);

                        List<String> commands = new ArrayList<>(Arrays.asList("-map", "0:" + trackIndex, "-pix_fmt", pixFmt ,"-c:v",
                                "libvpx", "-s", res, "-b:v", config.getDefaultValue(VP8_BITRATE), "-keyint_min", keyInt, "-g", keyInt ));
                        commands.addAll(config.getCommands(ADVOPT_VPX));
                        commands.addAll(Arrays.asList("-f", "webm_chunk", "-header", "video_" + trackIndex + ".hdr",
                                "-chunk_start_index", "1", "video_" + trackIndex + "_%d.chk"));

                        return commands;
                    }
                } else {
                    Logger.warn("Cannot generate specific track ffmpeg command if it's not audio or video.");
                }

                break;
            case WEBM_DASH_VP9_OPUS:

                if (type == TrackType.AUDIO) {
                    Optional<AudioTrackInfo> audioTrackInfo = streamInfo.getAudioTrackAtIndex(index);

                    if (audioTrackInfo.isPresent()) {

                        int trackIndex = audioTrackInfo.get().getIndex();

                        //add track to manifest command generation
                        addTrackToManifest(TrackType.AUDIO, trackIndex);

                        List<String> commands = new ArrayList<>(Arrays.asList("-map", "0:" + trackIndex, "-c:a","libopus", "-b:a",
                                config.getDefaultValue(OPUS_BITRATE)));
                        commands.addAll(config.getCommands(ADVOPT_OPUS));
                        commands.addAll(Arrays.asList("-f", "webm_chunk", "-audio_chunk_duration", config.getDefaultValue(CHUNK_DURATION),
                                "-header", "audio_" + trackIndex + ".hdr", "-chunk_start_index", "1", "audio_" + trackIndex + "_%d.chk"));

                        return commands;
                    }

                } else if (type == TrackType.VIDEO){

                    Optional<VideoTrackInfo> videoTrackInfo = streamInfo.getVideoTrackAtIndex(index);

                    if (videoTrackInfo.isPresent()) {

                        VideoTrackInfo vti = videoTrackInfo.get();

                        int trackIndex = vti.getIndex();
                        String pixFmt = vti.getPixelFormat().isEmpty() ? config.getDefaultValue(PIXEL_FORMAT)  : vti.getPixelFormat();
                        String res = vti.getWidth() + "x" + vti.getHeight();

                        //compute ideal keyframes number (framerate per seconds * number of seconds per chunk)
                        Double chunkDuration = Double.parseDouble(config.getDefaultValue(CHUNK_DURATION)) / 1000; //seconds
                        String keyInt = String.format("%d", Utils.roundEven(vti.getFrameRate() * chunkDuration)); //keyint_min and g

                        //add track to manifest command generation
                        addTrackToManifest(TrackType.VIDEO, trackIndex);

                        List<String> commands = new ArrayList<>(Arrays.asList("-map", "0:" + trackIndex, "-pix_fmt", pixFmt ,"-c:v",
                                "libvpx-vp9", "-s", res, "-b:v", config.getDefaultValue(VP9_BITRATE), "-keyint_min", keyInt, "-g", keyInt));
                        commands.addAll(config.getCommands(ADVOPT_VPX));
                        commands.addAll(Arrays.asList("-f", "webm_chunk", "-header", "video_" + trackIndex + ".hdr",
                                "-chunk_start_index", "1", "video_" + trackIndex + "_%d.chk"));
                        return commands;
                    }
                } else {
                    Logger.warn("Cannot generate specific track commands if it's not audio or video.");
                }
                break;
            case MPEG_DASH_H264_AAC: {

                if (type == TrackType.AUDIO) {
                    Optional<AudioTrackInfo> audioTrackInfo = streamInfo.getAudioTrackAtIndex(index);

                    if (audioTrackInfo.isPresent()) {

                        List<String> commands = new ArrayList<>(Arrays.asList("-map", "0:" + audioTrackInfo.get().getIndex(), "-c:a","libfdk_aac", "-b:a",
                                config.getDefaultValue(AAC_BITRATE)));
                        commands.addAll(config.getCommands(ADVOPT_AAC));

                        return commands;
                    }

                } else if (type == TrackType.VIDEO){

                    Optional<VideoTrackInfo> videoTrackInfo = streamInfo.getVideoTrackAtIndex(index);

                    if (videoTrackInfo.isPresent()) {

                        VideoTrackInfo vti = videoTrackInfo.get();

                        String pixFmt = vti.getPixelFormat().isEmpty() ? config.getDefaultValue(PIXEL_FORMAT)  : vti.getPixelFormat();
                        String res = vti.getWidth() + "x" + vti.getHeight();

                        //compute ideal keyframes number (framerate per seconds * number of seconds per chunk)
                        Double chunkDuration = Double.parseDouble(config.getDefaultValue(MIN_SEGMENT_DURATION)) / 1000; //seconds
                        String keyInt = String.format("%d", Utils.roundEven(vti.getFrameRate() * chunkDuration)); //keyint_min and g

                        List<String> commands = new ArrayList<>(Arrays.asList("-map", "0:" + vti.getIndex(), "-pix_fmt", pixFmt ,"-c:v",
                                "libx264", "-s", res, "-b:v", config.getDefaultValue(H264_BITRATE), "-keyint_min", keyInt, "-g", keyInt));
                        commands.addAll(config.getCommands(ADVOPT_H264));
                        return commands;
                    }
                } else {
                    Logger.warn("Cannot generate specific track commands if it's not audio or video.");
                }
                break;
            }
            case MPEG_DASH_PASSTHROUGH:
                Logger.warn("Service should not generate specific track commands" +
                        " if the selected encoding is MPEG-DASH (passthrough).");
                break;
        }

        return new ArrayList<String>(){};
    }

    /**
     * Add new track definition into manifest
     * @param type The type of the track.
     * @param index The absolute index of the track.
     */
    private void addTrackToManifest(final TrackType type, final int index) {

        if (type == TrackType.UNKNOWN) {
            Logger.warn("Unable to add unknown type of track to manifest.");
            return;
        }

        String headerTitle = (type == TrackType.VIDEO) ? "video_" + index + ".hdr" : "audio_" + index + ".hdr";

        //add command to declare input header video track for manifest
        manifestCommands.addAll(Arrays.asList("-f", "webm_dash_manifest", "-live", "1", "-i", headerTitle));
        //update map commands and adaptation set definition
        mapCommands.addAll(Arrays.asList("-map", String.format("%d", trackCount)));
        adaptationSets.append("id=" + trackCount + ",streams=" + trackCount + " ");
        trackCount++;
    }

    /**
     * Creates ffmpeg commands to generate webm dash manifest.
     * @return The list of commands.
     */
    private List<String> generateManifestCommand() {

        Configuration config = ConfigManager.getConfig();

        manifestCommands.add(0, "-y");
        manifestCommands.addAll(Arrays.asList("-c", "copy"));
        manifestCommands.addAll(mapCommands);
        manifestCommands.addAll(Arrays.asList("-f", "webm_dash_manifest", "-live", "1"));
        manifestCommands.add("-adaptation_sets");
        manifestCommands.add("\"" + adaptationSets.toString().trim() + "\"");
        manifestCommands.addAll(Arrays.asList("-chunk_start_index", "1", "-chunk_duration_ms", config.getDefaultValue(CHUNK_DURATION)));
        manifestCommands.addAll(config.getCommands(Constants.COMMANDS.ADVOPT_WEBMDASH));
        return manifestCommands;
    }

    /**
     * Generates the rtsp announce url.
     * @return The string representing the rtsp announce url.
     */
    private String generateServerAnnounceUrl() {
        return "rtsp://" + ConfigManager.getConfig().getAddress() + ":" +
                ConfigManager.getConfig().getListeningPort() + "/" +
                ConfigManager.getConfig().getAnnouncePath() + "/stream" + stream.getID();
    }
}
