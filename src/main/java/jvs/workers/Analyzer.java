package jvs.workers;

import java.util.Arrays;
import java.util.Optional;

import jvs.stream.*;
import jvs.stream.track.*;
import jvs.Constants;
import jvs.utils.Logger;
import jvs.workers.events.CompletedEventArgs;
import jvs.workers.events.CompletedEventListener;

/**
 * Starts ffprobe analysis of the input stream
 */
public class Analyzer {

    private boolean isRunning; //determine whether the
    private Stream stream; //contains all informations about the input stream
    private Thread worker; //the thread used by the analyzer to start the background operation
    private CompletedEventListener completedListener; //completed event listener

    /**
     * Analyzer constructor
     *
     * @param stream The stream object that needs to be encoded.
     */
    public Analyzer(final Stream stream) {
        this.stream = stream;
        this.isRunning = false;
    }

    /**
     * Starts the worker.
     * @return Boolean True, if the worker has been successfully started; otherwise false.
     */
    public Boolean run() {

        if (isRunning) {
            Logger.error("Unable to start analyzer (stream id: " + stream.getID() + ") if it's already running .");
            return false;
        }

        Worker mainWorker = new FFProbeWorker(Arrays.asList("-hide_banner", "-print_format", "json",
                "-show_streams", stream.getInputUrl()));

        mainWorker.addOnCompleteListener(args -> {
            Optional<Integer> opt = args.getExitCode();

            //mark stream as removable
            if (!opt.isPresent() || opt.get() != 0) {
                stream.setError();
            }

            if (opt.isPresent()) {
                if (opt.get() == 0 && args.getResult() != null) {
                    Logger.info("Analysis completed for stream with id: " + stream.getID());

                    String json = (String)args.getResult();
                    StreamInfo streamInfo = StreamInfo.parseStreamInfo(json);

                    //check if parsing failed or succeeded
                    if (streamInfo != null) {

                        // prettily print all parsed tracks
                        if (Constants.DEBUG_MODE) {

                            String log = "Parsed Tracks:\r\n";
                            for (TrackInfo si : streamInfo.getAllTracks()) {
                                if (si.getStreamType() == TrackType.VIDEO) {
                                    VideoTrackInfo vsi = (VideoTrackInfo) si;
                                    log += "#" + si.getIndex() + " - type: video, codec: " + vsi.getCodecName() + ", w: " +
                                            vsi.getWidth() + ", h: " + vsi.getHeight() + ", fps: " +
                                            String.format("%.3f", vsi.getFrameRate()) + ", pixFmt: " + vsi.getPixelFormat() + "\r\n";
                                } else if (si.getStreamType() == TrackType.AUDIO) {
                                    AudioTrackInfo asi = (AudioTrackInfo) si;
                                    log += "#" + si.getIndex() + " - type: audio, codec: " + si.getCodecName() + ", ch:" +
                                            asi.getChannels() + ", sRate: " + asi.getSampleRate() + ", bitDepth: " + asi.getBitDepth() + "\r\n";
                                }
                            }

                            Logger.log(log);
                        }

                        if (completedListener != null) {
                            completedListener.handle(new CompletedEventArgs(0, streamInfo));

                        } else {
                            Logger.warn("Unable to trigger completed event listener after analysis (stream id: " +
                                    stream.getID()+ ") because it was null.");
                        }

                    } else {
                        Logger.warn("Unable to parse json from ffprobe while analyzing stream with id: " +
                                stream.getID() + " (value: " + json + ").");
                    }

                } else {
                    Logger.warn("Analysis failed for stream with id: " + stream.getID() + " (exit code: " +
                            args.getExitCode().get() + ").");
                }

            } else {
                Logger.warn("Unable to verify if the analysis (stream id: " + stream.getID() +
                        ") was completed. FFProbe worker returned no exit code.");
            }
            isRunning = false;
        });

        worker = new Thread(mainWorker);
        worker.start();

        isRunning = true;
        return true;
    }

    /**
     * Attach a listener to the on analysis complete event of the analyzer.
     *
     * @param listener A CompletedEventListener
     */
    public void addOnCompleteListener(final CompletedEventListener listener) {
        this.completedListener = listener;
    }

    /**
     * Stops the worker.
     */
    public Boolean stop() {
        try {
            if (isRunning && worker != null && worker.isAlive()) {
                worker.interrupt();
                worker = null;
                isRunning = false;
                return true;
            }
        } catch (SecurityException e) {
            Logger.error("The worker thread of the analyzer could not be stopped.");
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Returns whether the analyzer is running or not.
     * @return True, if the analyzer is running; otherwise false.
     */
    public boolean isRunning() {
        return isRunning;
    }
}
