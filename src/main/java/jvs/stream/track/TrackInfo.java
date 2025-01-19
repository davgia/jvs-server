package jvs.stream.track;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Track generic informations
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackInfo {

    @JsonProperty("index")
    private int index;
    @JsonProperty("codec_name")
    private String codecName;
    @JsonProperty("codec_type")
    private String streamType;

    /**
     * Default constructor
     */
    public TrackInfo() {
        this.index = -1;
        this.codecName = "";
        this.streamType = "unknown";
    }

    /**
     * Gets the index, relative to the whole stream, of this track.
     * @return The index of the track.
     */
    public int getIndex() {
        return index;
    }

    /**
     * Gets the codec name of this track.
     * @return The codec name of this track.
     */
    public String getCodecName() {
        return codecName;
    }

    /**
     * Gets the stream type.
     * @return The type of this stream.
     */
    public TrackType getStreamType() {
        switch (streamType) {
            case "video": {
                return TrackType.VIDEO;
            }
            case "audio": {
                return TrackType.AUDIO;
            }
            default: {
                return TrackType.UNKNOWN;
            }
        }
    }
}
