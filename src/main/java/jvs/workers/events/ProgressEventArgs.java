package jvs.workers.events;

import java.time.Duration;
import java.util.Optional;

/**
 * The progress event arguments.
 */
public class ProgressEventArgs {

    private Duration progress;
    private String status;

    /**
     * ProgressEventArgs constructor.
     * @param progress The duration that represent the current length of the stream
     * @param status The string that represent the current status (can be null)
     */
    public ProgressEventArgs(final Duration progress, final String status) {
        this.progress = progress;
        this.status = status;
    }

    /**
     * Gets the progress.
     * @return The progress.
     */
    public Optional<Duration> getProgress() {
        return Optional.of(this.progress);
    }

    /**
     * Gets the status.
     * @return The status string.
     */
    public Optional<String> getStatus() {
        return Optional.of(this.status);
    }
}
