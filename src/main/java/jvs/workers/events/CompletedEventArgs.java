package jvs.workers.events;

import java.util.Optional;

/**
 * The completed event arguments.
 */
public class CompletedEventArgs {

    private Integer exitCode;
    private Object result;

    /**
     * CompletedEventArgs constructor.
     * @param exitCode The exit code (can be null).
     * @param result A generic object that represents the result of the operation (can be null).
     */
    public CompletedEventArgs(final Integer exitCode, final Object result) {
        this.exitCode = exitCode;
        this.result = result;
    }

    /**
     * Gets the exit code.
     * @return The exit code.
     */
    public Optional<Integer> getExitCode() {
        return Optional.of(this.exitCode);
    }

    /**
     * Gets the result.
     * @return The result.
     */
    public Object getResult() {
        return Optional.of(this.result);
    }
}
