package jvs.workers;

import jvs.workers.events.CompletedEventArgs;
import jvs.workers.events.CompletedEventListener;
import jvs.workers.events.ProgressEventArgs;
import jvs.workers.events.ProgressEventListener;

import java.util.List;

/**
 * Generic worker implementation.
 * Execute background operations creating a new process and exposes
 * events to handle both a progress update and completion.
 */
public abstract class Worker implements Runnable {

	//basic worker informations
    /**
     * The list of commands to execute with the process to start.
     */
	protected List<String> commands;
    /**
     * The working directory of the process to start.
     */
	protected String workingDir;
	
	//event listeners
	private ProgressEventListener progressListener;
	private CompletedEventListener completedListener;

    /**
     * Worker constructor
     *
     * @param commands The command to execute in background
     */
    protected Worker(final List<String> commands) {
        this.commands = commands;
        this.workingDir = "";
    }

	/**
	 * Worker constructor
	 * 
	 * @param commands The command to execute in background
	 * @param workingDir The working directory of the process
	 */
	protected Worker(final String workingDir, final List<String> commands) {
		this.commands = commands;
		this.workingDir = workingDir;
	}

	/**
	 * Attach a listener to the on progress event of the worker.
	 * 
	 * @param listener A ProgressEventListener
	 */
	public void addOnProgressListener(final ProgressEventListener listener) {
		this.progressListener = listener;
	}
	
	/**
	 * Attach a listener to the on complete event of the worker.
	 * 
	 * @param listener A CompletedEventListener
	 */
	public void addOnCompleteListener(final CompletedEventListener listener) {
		this.completedListener = listener;
	}

	/**
	 * Raise progress event
	 */
	protected void progress(final ProgressEventArgs args) {
		if (progressListener != null) 
			progressListener.handle(args);
	}
	
	/**
	 * Raise completed event
	 */
	protected void completed(final CompletedEventArgs args) {
		if (completedListener != null) 
			completedListener.handle(args);
	}

}
