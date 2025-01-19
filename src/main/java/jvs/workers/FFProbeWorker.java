package jvs.workers;

import jvs.config.ConfigManager;
import jvs.Constants;
import jvs.utils.Logger;
import jvs.workers.events.CompletedEventArgs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Worker used to execute ffprobe commands, extends Worker
 */
public class FFProbeWorker extends Worker {

    /**
     * FFProbeWorker constructor
     *
     * @param commands The commands to execute in background.
     */
    protected FFProbeWorker(List<String> commands) {
        super(commands);
    }

    /**
     * Defines the thread operations.
     */
    @Override
    public void run() {

        BufferedReader br = null;
        Process process = null;
        Integer exitCode = null;
        StringBuilder sb = new StringBuilder();
        Boolean error = false;

        try {
            ArrayList<String> cmd = new ArrayList<>();
            cmd.add(ConfigManager.getConfig().getFfprobePath());
            cmd.addAll(commands);

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(cmd);
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            process = pb.start();
            process.getOutputStream().close();

            if (Constants.DEBUG_MODE) {
                Logger.info("FFProbe Process started at: " + new Date().toString());
            }

            br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = br.readLine()) != null) {
                if (Constants.DEBUG_MODE) {
                    Logger.log(line);
                }
                sb.append(line);
            }

            exitCode = process.waitFor();
        } catch (IOException e) {
            Logger.error("FFProbe process reported an error: " + e.getLocalizedMessage());
            error = true;
        } catch (InterruptedException e) {
            Logger.error("FFProbe process has been interrupted.");
            error = true;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }

            if (process != null) {
                process.destroy();
            }
        }

        completed(new CompletedEventArgs(exitCode, error ? null : sb.toString()));
    }
}
