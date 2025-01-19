package jvs.workers.events;

/**
 * The interface of the event listener that handles progress event.
 */
public interface ProgressEventListener {
	
	/**
	 * Handle the raised event.
	 * 
	 * @param args An object containing all the relevant informations about the event.
	 */
	void handle(ProgressEventArgs args);
}
