package jvs.workers;

import java.io.File;
import java.time.Duration;
import java.util.*;

import jvs.Constants;
import jvs.RTSPMode;
import jvs.stream.Stream;
import jvs.stream.StreamInfo;
import jvs.command.CommandsGenerator;
import jvs.command.Commands;
import jvs.config.ConfigManager;
import jvs.utils.Logger;

/**
 * Starts ffmpeg with custom parameters if declared,
 * reads standard error to get current timecode (if needed)
 * if webm dash profile is chosen, handles the input close correctly
 * and updates manifest information (static and declare duration).
 */
public class Encoder {

    private boolean isRunning; //define whether this encoder is running
	private Stream stream; //contains all information about the input stream
	private EncodingType type; //the client requested encoding type
    private RTSPMode mode; //the client requested rtsp mode
    private List<String> extraArgs = null; //client defined extra arguments for ffmpeg
	private Map<EncoderType, Thread> workers = null; //the worker that process the arguments
    private Analyzer analyzer = null; //the analyzer
    private StreamInfo userStreamInfo = null; //the stream info parsed from the user request when RTSP server mode is enabled
    private FFMpegWorker mainWorker = null; //used to access ffmpeg worker stop under RTSP server
	
	/**
	 * Encoder constructor
	 * 
	 * @param stream The stream object that needs to be encoded.
	 * @param type The type of encoding.
	 */
	public Encoder(final Stream stream, final EncodingType type, final RTSPMode mode) {
		this.stream = stream;
		this.type = type;
		this.mode = mode;
		this.workers = new HashMap<>();
		this.isRunning = false;
		this.extraArgs = null;
	}

    /**
     * Gets the encoding type selected for this encoder.
     * @return The encoding type.
     */
    public EncodingType getType() {
        return type;
    }

    /**
     * Set extra ffmpeg arguments.
     * @param extraArgs List of extra ffmpeg commands.
     */
    public void setExtraArgs(final List<String> extraArgs) {
        this.extraArgs = extraArgs;
    }

    /**
	 * Starts the worker.
	 * @return Boolean True, if the worker has been successfully started; otherwise false.
	 */
	public Boolean run() {

        if (isRunning) {
            Logger.error("Unable to start encoder (stream id: " + stream.getID() + ") if it's already running.");
            return false;
        }

		String outputPath = stream.getDirectory();
		File outputDir = new File(outputPath);

		if (!outputDir.exists() && !outputDir.mkdirs()) {
			Logger.error("Failed to create new folder: " + outputPath);
            stream.setError();
            isRunning = false;
			return false;
		}

		CommandsGenerator cmdgen = new CommandsGenerator(stream);

		//set client extra arguments if needed
		if (extraArgs != null) {
		    cmdgen.setExtraArgs(extraArgs);
        }
		
		switch (type) {
            case MPEG_DASH_H264_AAC: //single worker needed
            case MPEG_DASH_PASSTHROUGH:

                if (type == EncodingType.MPEG_DASH_H264_AAC) {
                    if (mode == RTSPMode.SERVER) {
                        //when rtsp server mode is selected we need user to send
                        //information about the stream because ffprobe cannot be
                        //use in this case.
                        if (userStreamInfo != null) {
                            cmdgen.setStreamInfo(userStreamInfo);
                            encodeMpeg(cmdgen, outputPath);
                        } else {
                            Logger.error("Unable to start MPEG-DASH encoding of the stream with id: " + stream.getID() +
                                    ".\r\nMissing stream information from the user");
                            stream.setError();
                            isRunning = false;
                            return false;
                        }
                    } else {
                        Analyzer analyzer = new Analyzer(stream);
                        analyzer.addOnCompleteListener(args ->  {
                            //get information about the input stream
                            cmdgen.setStreamInfo((StreamInfo)args.getResult());
                        });

                        isRunning = analyzer.run();
                        return isRunning;
                    }
                } else {
                    encodeMpeg(cmdgen, outputPath);
                }
                break;

            case WEBM_DASH_VP8_VORBIS:
            case WEBM_DASH_VP9_OPUS:

                if (mode == RTSPMode.SERVER) {
                    //when rtsp server mode is selected we need user to send
                    //information about the stream because ffprobe cannot be
                    //use in this case.
                    if (userStreamInfo != null) {
                        cmdgen.setStreamInfo(userStreamInfo);
                        encodeWebM(cmdgen, outputPath);
                        isRunning = true;
                    } else {
                        Logger.error("Unable to start WEBM-DASH encoding of the stream with id: " + stream.getID() +
                                ".\r\nMissing stream information from the user");
                        stream.setError();
                        isRunning = false;
                    }

                    return isRunning;

                } else {
                    Analyzer analyzer = new Analyzer(stream);
                    analyzer.addOnCompleteListener(args ->  {
                        //get information about the input stream
                        cmdgen.setStreamInfo((StreamInfo)args.getResult());
                        encodeWebM(cmdgen, outputPath);
                    });

                    isRunning = analyzer.run();
                    return isRunning;
                }

            default:
                Logger.warn("Encoder cannot start workers because the encoding type is unknown.");
                isRunning = false;
                return false;
		}

        isRunning = true;
		return true;
	}

	/**
	 * Stops the worker.
	 */
	public Boolean stop() {
		try {
		    if (isRunning) {
                workers.forEach((k,v)-> {
                    if (v.isAlive()) {
                        v.interrupt();
                    }
                });
                workers.clear();
                isRunning = false;

                if (analyzer != null && analyzer.isRunning()) {
                    isRunning = analyzer.stop();
                }
                return isRunning;
            }
		} catch (SecurityException e) {
			Logger.error("The worker thread of the encoder could not be stopped.");
			e.printStackTrace();
		}		
		return false;
	}

    /**
     * Gracefully stops the worker.
     * @return True, if the stop command is successfully sent to the worker process; otherwise false.
     */
	public Boolean stopGracefully() {
        try {
            if (isRunning && mainWorker != null) {
                return mainWorker.stopGracefully();
            }
        } catch (SecurityException e) {
            Logger.error("The worker thread of the encoder could not be gracefully stopped.");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Returns whether the encoder is running or not.
     * @return True, if the encoder is running; otherwise false.
     */
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * Sets the user signaled stream info in case ffmpeg must be run as RTSP server. If no stream info
     * are provided by the user the encode will fail, unless the choose encoding type is mpeg-dash passthrough.
     * @param streamInfo The StreamInfo object parsed from the user request.
     */
    public void setUserStreamInfo(final StreamInfo streamInfo) {
        this.userStreamInfo = streamInfo;
    }

    /**
     * Starts workers to encode stream into webm-dash format.
     * @param cmdGen The commandsGenerator to generate commands
     * @param outputPath The output path.
     */
    private void encodeWebM(final CommandsGenerator cmdGen, final String outputPath) {

        String encodingTag = (type == EncodingType.WEBM_DASH_VP9_OPUS ? "WEBM-DASH (VP9/Opus)" : "WEBM-DASH (VP8/Vorbis)");

        //generate commands
        Commands commands = cmdGen.generateCommands(type, mode);

        //DEBUG
        if (Constants.DEBUG_MODE) {
            Logger.log("encode: " + Arrays.toString(commands.getEncodeCommands().toArray()));
            Logger.log("manifest: " + Arrays.toString(commands.getManifestCommands().toArray()));
        }

        //update stream information
        stream.setManifest(ConfigManager.getConfig().getRemoteStreamsPath() + "/stream_" + stream.getID() + "/manifest.mpd");

        //initialize workers, event handlers and start encode
        Logger.info("Started " + encodingTag + " encoding of the stream with id: " + stream.getID());

        //manifest generator
        Worker manifestWorker = new FFMpegWorker(outputPath, commands.getManifestCommands());
        manifestWorker.addOnCompleteListener(arg -> {
            Optional<Integer> opt = arg.getExitCode();

            //mark stream as removable
            if (!opt.isPresent() || opt.get() != 0) {
                stream.setError();
            }

            if (opt.isPresent()) {
                if (opt.get() == 0) {
                    Logger.info("WEBM-DASH manifest has been successfully created for stream with id: " + stream.getID());
                } else {
                    Logger.warn("WEBM-DASH manifest generation failed for stream with id: " + stream.getID() +
                            " (exit code: " + opt.get() + ", status: " + arg.getResult().toString() + ").");
                }
            } else {
                Logger.warn("WEBM-DASH manifest generation failed for stream with id: " + stream.getID() + ".");
            }
        });

        //create thread for the mpd generator worker, does not start it for now
        Thread secondWorker = new Thread(manifestWorker);
        workers.put(EncoderType.MPDCREATOR, secondWorker);

        // stream encoder
        mainWorker = new FFMpegWorker(outputPath, commands.getEncodeCommands());
        mainWorker.addOnProgressListener(arg -> {
            Optional<Duration> progress = arg.getProgress();
            if (progress.isPresent() && !progress.get().isZero()) {

                //when the encoder worker report for the first time a progress it means that the
                // header files have been created and that ffmpeg is able to create the manifest
                if (stream.getCurrentLiveTime() == -1) {
                    secondWorker.start();
                }

                stream.setTotalDuration(progress.get());
            } else {
                Logger.warn("Worker triggered a new progress event without sending data.");
            }
        });
        mainWorker.addOnCompleteListener(arg -> {
            Optional<Integer> opt = arg.getExitCode();

            //mark stream as removable
            if ((!opt.isPresent() || opt.get() != 0) && !(mode == RTSPMode.SERVER && opt.isPresent() && opt.get() == 2)) {
                stream.setError();
            }

            if (opt.isPresent()) {
                if (opt.get() == 0 || (mode == RTSPMode.SERVER && opt.get() == 2)) {
                    Logger.info(encodingTag + " encoding completed for stream with id: " + stream.getID());

                    //update webm dash manifest from live to on-demand
                    Worker mpdUpdaterWorker = new MPDUpdaterWorker(Arrays.asList(outputPath, "manifest.mpd"),
                            Duration.ofSeconds(stream.getCurrentLiveTime()));
                    Thread thirdWorker = new Thread(mpdUpdaterWorker);
                    workers.put(EncoderType.MPDFINALIZER, thirdWorker);
                    thirdWorker.start();

                } else {
                    Logger.warn(encodingTag + " encoding failed for stream with id: " + stream.getID() +
                            " (exit code: " + opt.get() + ", status: " + arg.getResult().toString() + ").");
                }
            } else {
                Logger.warn(encodingTag + " encoding failed for stream with id: " + stream.getID() + ".");
            }

            isRunning = false;
            mainWorker = null;
        });

        //create new thread for the encode worker and start it
        Thread firstWorker = new Thread(mainWorker);
        workers.put(EncoderType.MAIN, firstWorker);
        firstWorker.start();
    }

    /**
     * Starts workers to encode stream into mpeg-dash format.
     * @param cmdGen The commandsGenerator to generate commands
     * @param outputPath The output path.
     */
    private void encodeMpeg(final CommandsGenerator cmdGen, final String outputPath) {

        String encodingTag = (type == EncodingType.MPEG_DASH_H264_AAC ? "MPEG-DASH (H264/AAC)" : "MPEG-DASH (passthrough)");

        Commands cmd = cmdGen.generateCommands(type, mode);

        mainWorker = new FFMpegWorker(outputPath, cmd.getEncodeCommands());
        mainWorker.addOnProgressListener(args -> {
            Optional<Duration> progress = args.getProgress();
            if (progress.isPresent()) {
                stream.setTotalDuration(progress.get());
            } else {
                Logger.warn("Worker triggered a new progress event without sending data.");
            }
        });
        mainWorker.addOnCompleteListener(args -> {
            Optional<Integer> opt = args.getExitCode();

            //mark stream as removable
            if ((!opt.isPresent() || opt.get() != 0) && !(mode == RTSPMode.SERVER && opt.isPresent() && opt.get() == 2)) {
                stream.setError();
            }

            if (opt.isPresent()) {
                if (opt.get() == 0 || (mode == RTSPMode.SERVER && opt.get() == 2)) {
                    Logger.info(encodingTag + " encoding completed for stream with id: " + stream.getID());
                } else {
                    Logger.warn(encodingTag + " encoding failed for stream with id: " + stream.getID() + " (exit code: " +
                            opt.get() + ", status: " + args.getResult().toString() + ").");
                }
            } else {
                Logger.warn(encodingTag + " encoding failed for stream with id: " + stream.getID() + ".");
            }
            isRunning = false;
            mainWorker = null;
        });

        //update stream information
        stream.setManifest(ConfigManager.getConfig().getRemoteStreamsPath() +
                "/stream_" + stream.getID() + "/manifest.mpd");

        Logger.info("Started " + encodingTag + " encoding of the stream with id: " + stream.getID());

        Thread worker = new Thread(mainWorker);
        workers.put(EncoderType.MAIN, worker);
        worker.start();
    }
}
