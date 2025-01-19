package jvs.stream.track;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Audio track specific informations
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioTrackInfo extends TrackInfo {

    @JsonProperty("sample_rate")
    private int sampleRate;
    private int channels;
    @JsonProperty("bits_per_raw_sample")
    private int bitDepth;

    /**
     * Gets the audio sample rate.
     * @return The sample rate of the audio track.
     */
    public int getSampleRate() {
        return sampleRate;
    }

    /**
     * Gets the number of channels.
     * @return The number of channels of the audio track.
     */
    public int getChannels() {
        return channels;
    }

    /**
     * Gets the bit depth.
     * @return The bit depth of the audio track.
     */
    public int getBitDepth() {
        return bitDepth == 0 ? 16 : bitDepth;
    }
}
