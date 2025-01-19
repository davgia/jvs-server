package jvs.stream.track;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Video track general informations
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VideoTrackInfo extends TrackInfo {

    @JsonProperty("pix_fmt")
    private String pixelFormat;
    private int width;
    private int height;
    @JsonProperty("r_frame_rate")
    private String frameRate;

    /**
     * Gets the video width.
     * @return The width of the video track.
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the video height.
     * @return The height of the video track.
     */
    public int getHeight() {
        return height;
    }

    /**
     * Gets the video pixel format.
     * @return The pixel format of the video track.
     */
    public String getPixelFormat() {
        return pixelFormat;
    }

    /**
     * Gets the video framerate.
     * @return The framerate of the video track.
     */
    public Double getFrameRate() {

        Double parsedValue = 24d; //default

        if (!frameRate.isEmpty()) {
            Pattern pattern = Pattern.compile("(\\d+)/(\\d+)");
            Matcher matcher = pattern.matcher(frameRate);

            if (matcher.matches()) {
                Double num = Double.parseDouble(matcher.group(1));
                Double den = Double.parseDouble(matcher.group(2));

                if (den > 0) {
                    parsedValue = num / den;
                }
            }
        }
        return parsedValue;
    }
}
