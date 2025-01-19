package jvs;

public class Constants {
	
	public static final Boolean DEBUG_MODE = true;

	/* Client add stream request keys */
	public static final String INPUTURL_KEY = "url";
	public static final String TITLE_KEY = "title";
	public static final String DESCR_KEY = "descr";
	public static final String ENCODINGTYPE_KEY = "encType";
	public static final String MODE_KEY = "mode";
	public static final String INFOS_KEY = "infos";
	public static final String CUSTOM_ARGS_KEY = "customArgs";

	/* Additional keys reported by the server after a stream information request */
    public static final String ID_KEY = "id";
    public static final String LIVETIME_KEY = "live";
    public static final String MANIFEST_KEY = "manifest";
    public static final String DURATION_KEY = "duration";
    public static final String ISLIVE_KEY = "isLive";
    public static final String CREATIONDATE_KEY = "creationDate";
    public static final String STREAMTYPE_KEY = "streamType";
    public static final String ANNOUNCEPATH_KEY = "annPath";
    public static final String LISTENINGPORT_KEY = "listPort";

	/* FFProbe report keys */
	public static final String CODEC_TYPE_KEY = "codec_type";

	/* Configuration file name */
	public static final String CONFIG_FILE_PATH = "config.json";

	/* Streams list backup file name */
    public static final String STREAMS_FILE_PATH = "streams.json";

	/* Configuration file keys */
	public class CONFIG_KEYS {
        public static final String ADDRESS = "address";
        public static final String PORT = "port";
        /* Local path where manifest files are stored */
        public static final String STREAMS_PATH = "streamsPath";
        /* Local path where web-app assets are stored */
        public static final String ASSETS_PATH = "assetsPath";
        /* Remote path used to access file system where manifest files are stored */
        public static final String REMOTE_STREAMS_PATH = "remoteStreamsPath";
        /* Remote root path used to access REST API functions */
        public static final String REMOTE_ROOT_API_PATH = "remoteRootAPIPath";
        /* Remote path used to access file system where web-app assets are stored */
        public static final String REMOTE_ASSETS_PATH = "remoteAssetsPath";
        public static final String FFMPEG_PATH = "ffmpegPath";
        public static final String FFPROBE_PATH = "ffprobePath";
        public static final String ANNOUNCE_PATH = "announcePath";
        public static final String LISTENING_PORT = "listeningPort";
        public static final String PATTERNS = "patterns";
        public static final String COMMANDS = "commands";
        public static final String DEFAULTS = "defaults";
    }

    /* Patterns keys */
    public class PATTERNS {
        public static final String PROGRESS = "progress";
        public static final String SUCCESS = "success";
        public static final String DURATION = "duration";
    }

    /* Defaults keys */
    public class DEFAULTS {
        public static final String PIXEL_FORMAT = "pixelFormat";

        /* MPEG-DASH */
        public static final String MIN_SEGMENT_DURATION = "minSegmentDuration";
        public static final String H264_BITRATE = "h264Bitrate";
        public static final String AAC_BITRATE = "aacBitrate";

        /* WEBM-DASH */
        public static final String CHUNK_DURATION =  "chunkDuration";
        public static final String VP8_BITRATE = "vp8Bitrate";
        public static final String VORBIS_BITRATE = "vorbisBitrate";
        public static final String VP9_BITRATE = "vp9Bitrate";
        public static final String OPUS_BITRATE = "opusBitrate";
    }

    /* Commands keys */
    public class COMMANDS {
        public static final String ADVOPT_FFMPEG = "advOptFFMpeg";
        public static final String ADVOPT_RTSPSERVER = "advOptRtspServer";

        public static final String ADVOPT_MPEGDASH = "advOptMpegDash";
        public static final String ADVOPT_H264 = "advOptH264";
        public static final String ADVOPT_AAC = "advOptAac";

        public static final String ADVOPT_WEBMDASH = "advOptWebmDash";
        public static final String ADVOPT_VPX = "advOptVpx";
        public static final String ADVOPT_OPUS = "advOptOpus";
        public static final String ADVOPT_VORBIS = "advOptVorbis";
    }
}
