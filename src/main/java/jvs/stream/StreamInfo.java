package jvs.stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import jvs.Constants;
import jvs.stream.track.AudioTrackInfo;
import jvs.stream.track.TrackInfo;
import jvs.stream.track.VideoTrackInfo;
import jvs.utils.JsonUtils;
import jvs.utils.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Stores information about every track inside the stream.
 */
public class StreamInfo {

    private ArrayList<VideoTrackInfo> videoTracks = new ArrayList<>();
    private ArrayList<AudioTrackInfo> audioTracks = new ArrayList<>();

    /**
     * StreamInfo Constructor
     */
    public StreamInfo() {
        videoTracks = new ArrayList<>();
        audioTracks = new ArrayList<>();
    }

    /**
     * Adds a new video track.
     * @param videoTrack The video track to add
     */
    public void addVideoTrack(final VideoTrackInfo videoTrack) {
        videoTracks.add(videoTrack);
    }

    /**
     * Adds a new audio track.
     * @param audioTrack The audio track to add
     */
    public void addAudioTrack(final AudioTrackInfo audioTrack) {
        audioTracks.add(audioTrack);
    }

    /**
     * Gets the track at a specific index (the index of the track in the input stream)
     * @param index The index of the track
     * @return The track infos; otherwise null.
     */
    public Optional<TrackInfo> getTrackAtIndex(final int index) {
        if (index < 0 || getAllTracks().size() <= 0) {
            return Optional.empty();
        }
        return getAllTracks().stream().filter(i -> i.getIndex() == index).findFirst();
    }

    /**
     * Gets the video track at a specific index
     * @param index The index of the track relative to the tracks of the same kind
     * @return The track infos; otherwise null.
     */
    public Optional<VideoTrackInfo> getVideoTrackAtIndex(final int index) {
        if (index < 0 || videoTracks.size() <= 0 || videoTracks.size() <= index) {
            return Optional.empty();
        }
        return Optional.of(videoTracks.get(index));
    }

    /**
     * Gets the audio track at a specific index
     * @param index The index of the track relative to the tracks of the same kind
     * @return The track infos; otherwise null.
     */
    public Optional<AudioTrackInfo> getAudioTrackAtIndex(final int index) {
        if (index < 0 || audioTracks.size() <= 0 || audioTracks.size() <= index) {
            return Optional.empty();
        }
        return Optional.of(audioTracks.get(index));
    }

    /**
     * Gets the list of all video tracks.
     * @return The list of video tracks
     */
    public ArrayList<VideoTrackInfo> getVideoTracks() {
        return videoTracks;
    }

    /**
     * Gets the list of all audio tracks.
     * @return The list of audio tracks
     */
    public ArrayList<AudioTrackInfo> getAudioTracks() {
        return audioTracks;
    }

    /**
     * Get the list of all tracks sorted by index.
     * @return The sorted list of all tracks
     */
    public ArrayList<TrackInfo> getAllTracks() {
        ArrayList<TrackInfo> allTracks = new ArrayList<>();
        allTracks.addAll(videoTracks);
        allTracks.addAll(audioTracks);
        allTracks.sort((i,j) -> {
            Integer a = i.getIndex();
            Integer b = j.getIndex();
            return a.compareTo(b);
        });

        return allTracks;
    }

    /**
     * Parses stream information from an input json string. (must have the same structure as the one outputted by ffprobe)
     * @param json The input json string.
     * @return The parsed StreamInfo object; otherwise null;
     */
    public static StreamInfo parseStreamInfo(final String json) {

        if (JsonUtils.isJSONValid(json)) {
            StreamInfo streamInfo = new StreamInfo();

            JsonArray streams = new JsonObject(json).getJsonArray("streams");

            for (int i = 0; i < streams.size(); i++) {
                try {
                    JsonObject obj = streams.getJsonObject(i);

                    if (obj.containsKey(Constants.CODEC_TYPE_KEY)) {
                        if (obj.getString(Constants.CODEC_TYPE_KEY).equals("video")) {

                            VideoTrackInfo videoStreamInfo = new ObjectMapper().readValue(obj.encode(), VideoTrackInfo.class);
                            streamInfo.addVideoTrack(videoStreamInfo);

                        } else if (obj.getString(Constants.CODEC_TYPE_KEY).equals("audio")) {

                            AudioTrackInfo audioStreamInfo = new ObjectMapper().readValue(obj.encode(), AudioTrackInfo.class);
                            streamInfo.addAudioTrack(audioStreamInfo);

                        } else {
                            Logger.warn("Track with index " + i + " of type '" + obj.getString(Constants.CODEC_TYPE_KEY) +
                                    "' is not supported. It will be skipped.");
                        }
                    } else {
                        Logger.warn("Track with index " + i + " has no declared 'codec_type'. It will be skipped.");
                    }

                } catch (IOException e) {
                    Logger.error("Unable to map Object from: " + json);
                    e.printStackTrace();
                }
            }

            return streamInfo;
        }

        return null;
    }
}
