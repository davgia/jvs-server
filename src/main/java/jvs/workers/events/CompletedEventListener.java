package jvs.workers.events;

/**
 * The interface of the event listener that handles completed event.
 */
public interface CompletedEventListener {
	
	/**
	 * Handle the raised event.
	 * 
	 * @param args An object containing all the relevant informations about the event.
	 */
	void handle(CompletedEventArgs args);
}
