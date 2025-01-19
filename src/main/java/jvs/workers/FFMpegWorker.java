package jvs.workers;

import jvs.config.ConfigManager;
import jvs.Constants;
import jvs.utils.DurationUtils;
import jvs.utils.Logger;
import jvs.workers.events.CompletedEventArgs;
import jvs.workers.events.ProgressEventArgs;

import java.io.*;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jvs.Constants.PATTERNS.*;

/**
 * Worker used to execute ffmpeg commands, extends Worker
 */
public class FFMpegWorker extends Worker {

    /**
     * Save output stream of the running process, to be able to send kill commands if stopGracefully method is called.
     */
    private OutputStream outputStream = null;

    /**
     * FFMpegWorker constructor
     *
     * @param workingDir The working directory of the process
     * @param commands   The command to execute in background
     */
    public FFMpegWorker(String workingDir, List<String> commands) {
        super(workingDir, commands);
    }

    /**
     * Defines the thread operations.
     */
    @Override
    public void run() {

        Process process = null;
        BufferedReader br = null;
        Integer exitCode = null;
        StringBuilder errorMessage = new StringBuilder();

        try {
            //add ffmpeg path as the first element of the commands
            commands.add(0, ConfigManager.getConfig().getFfmpegPath());

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(commands);
            pb.directory(new File(workingDir));
            pb.redirectError(ProcessBuilder.Redirect.PIPE);
            pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
            process = pb.start();
            //process.getOutputStream().close();
            outputStream = process.getOutputStream(); //save output stream of the process to kill ffmpeg rtsp server

            if (Constants.DEBUG_MODE) {
                Logger.info("FFMpeg Process started at: " + new Date().toString());
            }

            br = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            String lastWarning = null;
            int step = 0;

            while ((line = br.readLine()) != null) {

                if (Constants.DEBUG_MODE) {
                    Logger.log(line);
                }

                //input informations
                if (step == 0) {
                    if (line.startsWith("WARNING: ")) {
                        Logger.warn("FFMpeg reported: " + line);
                    } else if (!line.startsWith("Output #0")) {
                        //errorMessage.append(line + "\n");
                    } else {
                        step++;
                    }
                }

                //output informations
                if (step == 1) {
                    if (line.startsWith("WARNING: ")) {
                        Logger.warn("FFMpeg reported: " + line);
                    } else if (!line.startsWith("Output #0")) {
                        //errorMessage.append(line + "\n");
                    } else {
                        step++;
                    }
                } else if (step == 2) {
                    if (!line.startsWith("  ")) {
                        step++;
                    }
                }

                //other output informations
                if (step == 3) {
                    if (!line.startsWith("Stream mapping:")) {
                        errorMessage.append(line + "\n");
                    } else {
                        step++;
                    }
                } else if (step == 4) {
                    if (!line.startsWith("  ")) {
                        step++;
                    }
                }

                //progress update
                if (line.startsWith("frame="))
                {
                    try
                    {
                        line = line.trim();
                        if (line.length() > 0) {
                            HashMap<String, String> table = parseProgressInfoLine(line);

                            if (table == null) {
                                lastWarning = line;
                            } else {
                                String time = table.get("time");

                                if (time != null) {

                                    Duration duration = DurationUtils.parseDuration(time);
                                    progress(new ProgressEventArgs(duration, null));
                                }
                                lastWarning = null;
                            }
                        }
                    }
                    catch (Exception ex)
                    {
                        Logger.warn("Error in progress parsing for line: " + line);
                    }
                }
            }
            if (lastWarning != null) {
                Pattern pattern = ConfigManager.getConfig().getPattern(SUCCESS);
                if (!pattern.matcher(lastWarning).matches()) {
                    Logger.error("No match for success pattern in " + lastWarning);
                }
            }

            exitCode = process.waitFor();
        } catch (IOException e) {
            Logger.error("FFMpeg process reported an error:");
            e.printStackTrace();
        } catch (InterruptedException e) {
            Logger.error("FFMpeg process has been interrupted.");
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (process != null) {
                process.destroy();
            }
        }

        completed(new CompletedEventArgs(exitCode, errorMessage.toString()));
    }

    /**
     * Sends the stop command to the ffmpeg process.
     * @return True, if the stop command is successfully sent to the process; otherwise false.
     */
    public boolean stopGracefully() {
        try {
            Logger.info("Sending graceful kill command to ffmpeg process...");
            outputStream.write("q\n".getBytes());
            outputStream.flush();
            outputStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Parses ffmpeg progress information.
     * @param line The untouched output ffmpeg line.
     * @return HashMap of all parsed information and relative values
     */
    private HashMap<String, String> parseProgressInfoLine(String line) {
        HashMap<String, String> table = null;
        Matcher m = ConfigManager.getConfig().getPattern(PROGRESS).matcher(line);
        while (m.find()) {
            if (table == null) {
                table = new HashMap<>();
            }
            String key = m.group(1);
            String value = m.group(2);
            table.put(key, value);
        }
        return table;
    }
}
