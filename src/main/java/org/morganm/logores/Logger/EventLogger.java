/**
 * 
 */
package org.morganm.logores.Logger;

import org.morganm.logores.ProcessedEvent;

/**
 * @author morganm
 *
 */
public interface EventLogger {
	/** Called to give the logger an opportunity to initialize itself.  Return object must be
	 * this logging object.
	 * 
	 * @return
	 */
	public EventLogger init();
	
	/** Called to flush the logger at regular intervals. Implementation can choose to respond to this
	 * or ignore it based on implementation details.
	 * 
	 */
	public void flush();
	
	/** Called to close the logger, freeing any resources.  flush() is guaranteed to be called immediately
	 * before a close().
	 * 
	 */
	public void close() throws Exception;
	
	public void logEvent(ProcessedEvent pe) throws Exception;
}
