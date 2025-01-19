package jvs.stream;

import java.security.InvalidParameterException;
import java.time.Duration;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.vertx.core.json.JsonObject;
import jvs.Constants;
import jvs.RTSPMode;
import jvs.utils.Logger;
import jvs.workers.Encoder;
import jvs.workers.EncodingType;

/**
 * Stream implementation, defines all general information about the stream
 * and all possible action that can be done with it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Stream {
		
	/* INFORMATION */

    /**
     * Stream unique identifier
     */
	private int id;
    /**
     * Stream title
     */
	private String title;
    /**
     * Stream content description
     */
	private String description;
    /**
     * Url from which the stream should be taken
     */
	private String inputUrl;
    /**
     * The total duration of the stream
     */
    private Duration duration;
    /**
     * The url of the DASH manifest that let clients to play this stream
     */
	private String manifest;
    /**
     * The date when the stream was added
     */
	private Date creationDate;
    /**
     * The local directory where all stream dash chunks are stored
     */
	private String directory;
    /**
     * Reference to current working encoder
     */
    @JsonIgnore
    private Encoder encoder;

    /**
     * Flags to determine if this instance is dirty (errors were detected during his encoding)
     */
    @JsonIgnore
    private boolean errorFlag;

    /**
     * Default constructor
     */
    @JsonCreator
    public Stream() {
        id = -1;
        title = "";
        description = "";
        inputUrl = "";
        duration = null;
        manifest = "";
        encoder = null;
        creationDate = null;
        errorFlag = false;
    }

    /**
     * Private constructor
     * @param title The title of the stream
     * @param description The description of the stream
     * @param url The input url of the stream
     * @throws InvalidParameterException If the url param is invalid
     */
	private Stream(final String title, final String description, final String url) throws InvalidParameterException  {
		
		if (!checkInputUrl(url)) {
			throw new InvalidParameterException("Stream cannot be initiated if the Url parameter is not valid (value: " + url + ")");
		}
			
		this.title = title;
		this.description = description;
		this.inputUrl = url;
	}
	
	/**
	 * Stream constructor (from json)
	 *
	 * @param json A json object where to parse parameters
	 * @throws InvalidParameterException If the input url parameters is missing or not valid.
	 */
	public Stream(JsonObject json) throws InvalidParameterException {
		this(json.getString(Constants.TITLE_KEY), json.getString(Constants.DESCR_KEY), json.getString(Constants.INPUTURL_KEY));
	}
	
	/* GETTERS */
	
	/**
	 * Gets the identifier of the stream.
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * Gets the title of the stream.
	 * @return The title.
	 */
	public String getTitle() {
		return this.title;
	}
	
	/**
	 * Gets the description of the stream.
	 * @return The description.
	 */
	public String getDescription() {
		return this.description;
	}
	
	/**
	 * Gets the original rtsp url from which the stream was taken.
	 * @return The input rtsp url.
	 */
	public String getInputUrl() {
		return this.inputUrl;
	}
	
	/**
	 * Gets the current timecode, in seconds, to seek to play this stream live.
	 * @return The current timecode, in seconds, of the original input stream.
	 */
	public int getCurrentLiveTime() {
	    if (duration != null && !duration.isZero() && !duration.isNegative()) {
	        return (int)Math.floor(duration.toMillis() / 1000);
        }
		return -1;
	}
	
	/**
	 * Gets the dash manifest related to that stream. Can be empty if the stream has not been processed yet.
	 * @return The dash manifest url.
	 */
	public String getManifest() {		
		return this.manifest;
	}

    /**
     * Gets the duration in milliseconds of the stream.
     * @return The duration of the stream in milliseconds.
     */
    public Duration getDuration() {
        if (duration != null && !duration.isZero() && !duration.isNegative()) {
            return duration;
        }
        return Duration.ofSeconds(0);
    }

    /**
     * Gets the creation date of the stream.
     * @return The creation date of the stream.
     */
    public Date getCreationDate() {
        if (creationDate != null) {
            return creationDate;
        }
        return new Date(0);
    }

    /**
     * Gets the directory where all stream files are stored.
     * @return The directory where all stream files are stored.
     */
    public String getDirectory() {
        return directory;
    }

	/* SETTERS */
	
	/**
	 * Sets the identifier of the stream.
	 * @param id The identifier.
	 */
	public void setID(final int id) {
		this.id = id;
	}
	
	/**
	 * Sets the title of the stream.
	 * @param title The title.
	 */
	public void setTitle(final String title) {
		this.title = title;
	}
		
	/**
	 * Sets the description of the stream.
	 * @param description The description.
	 */
	public void setDescription(final String description) {
		this.description = description;
	}

    /**
     * Sets the total duration of the stream.
     * @param duration The total duration of the stream, in milliseconds.
     */
    public void setTotalDuration(final Duration duration) {
        this.duration = duration;
    }
	
	/**
	 * Sets the dash manifest url of the stream.
	 * @param manifest The dash manifest url.
	 */
	public void setManifest(final String manifest) {		
		this.manifest = manifest;
	}

    /**
     * Sets the creation date of the stream.
     * @param creationDate The creation date.
     */
    public void setCreationDate(final Date creationDate) {
        this.creationDate = creationDate;
    }

    /**
     * Sets the directory where all stream files are stored.
     * @param directory The where all stream files are stored.
     */
    public void setDirectory(final String directory) {
        this.directory = directory;
    }

	/* HELPERS */
	
	/**
	 * Return a json string that represents the stream.
	 * @return The json with all stream information.
	 */
	public JsonObject toJSON() {
		return new JsonObject().put(Constants.ID_KEY, id)
                .put(Constants.TITLE_KEY, title)
                .put(Constants.DESCR_KEY, description)
				.put(Constants.LIVETIME_KEY, encoder != null && encoder.isRunning() ? getCurrentLiveTime() : 0)
                .put(Constants.DURATION_KEY, getDuration().toString())
                .put(Constants.MANIFEST_KEY, manifest)
                .put(Constants.CREATIONDATE_KEY, getCreationDate().toInstant().toString())
                .put(Constants.STREAMTYPE_KEY, encoder != null ? encoder.getType().getDescription() : "unknown")
                .put(Constants.ISLIVE_KEY, encoder != null && encoder.isRunning());
	}

	/**
	 * Prepares the encoder of this stream.
	 * @param type The encoding type.
	 * @param mode The ffmpeg RTSP mode.
	 */
	public void prepareEncoder(final EncodingType type, final RTSPMode mode) {
		if (encoder == null) {
			encoder = new Encoder(this, type, mode);
		} else {
		    Logger.warn("Encoder have been prepared multiple times for stream with id: " + id);
        }
	}

    /**
     * Sets the encoder stream information. This method must be called after
     * {@link #prepareEncoder(EncodingType, RTSPMode)} and only when RTSP Server
     * mode is enabled .
     * @param streamInfo The stream information parsed from the user request.
     */
	public void setUserStreamInfo(final StreamInfo streamInfo) {
        if (encoder != null) {
            encoder.setUserStreamInfo(streamInfo);
        } else {
            Logger.warn("Unable to set UserStreamInfo, encoder have not been prepared for stream with id: " + id);
        }
    }

    /**
     * Sets extra ffmpeg arguments for the encoder.
     * @param extraArguments List of extra ffmpeg arguments.
     */
    public void setExtraArguments(final List<String> extraArguments) {
        if (encoder != null) {
            encoder.setExtraArgs(extraArguments);
        } else {
            Logger.warn("Unable to set extraArguments, encoder have not been prepared for stream with id: " + id);
        }
    }

	/**
	 * Starts the stream encode, this method must be called after {@link #prepareEncoder(EncodingType, RTSPMode)}.
	 * @return Boolean True, if the encode has been started successfully; otherwise false.
	 */
	public Boolean startEncoding() {
	    if (encoder != null) {
            return encoder.run();
        }
        return false;
	}

    /**
     * Stops every encode started for this stream.
     */
    public void stopEncoding() {
        if (encoder != null && encoder.isRunning()) {
            if (encoder.stop()) {
                encoder = null;
            }
        }
    }

	/**
	 * Gracefully stops the encode of this stream.
	 */
	public void stopEncodingGracefully() {
		if (encoder != null && encoder.isRunning()) {
			if (encoder.stopGracefully()) {
				encoder = null;
			}
		}
	}

    /**
     * Sets error flag for the streams, so it will be removed on next cleaning.
     */
    public void setError() {
        this.errorFlag = true;
    }

    /**
     * Determine whether the stream has been encoded correctly or not.
     * @return True, if it was correctly encoded; otherwise false.
     */
    public boolean hadErrors() {
        return this.errorFlag;
    }

	/**
     * Checks whether the url parameter is valid (must be rtsp).
     * @param url The url to check
	 */
	private Boolean checkInputUrl(final String url) {
        if (url == null || url.isEmpty()) {
            Logger.error("Stream cannot be initialized with an empty input url.");
            return false;
        }
        if (!url.startsWith("rtsp://")) {
            Logger.error("Stream can only be initialized with a rtsp input url.");
            return false;
        }
		return true;
	}
}
