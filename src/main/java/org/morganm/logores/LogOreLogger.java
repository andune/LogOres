/**
 * 
 */
package org.morganm.logores;

import org.bukkit.block.BlockState;
import org.morganm.logores.config.Config;

/** This class is responsible for actually writing the logs.
 * 
 * @author morganm
 *
 */
public class LogOreLogger implements Runnable {

	private final LogOresPlugin plugin;
	private final LogQueue queue;
	private final Config config;
	private boolean running = false;
	
	public LogOreLogger(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.config = plugin.getConfig();
		this.queue = plugin.getLogQueue();
	}
	
	/** This method will be called once by Bukkit Scheduler.  It will try to read the Queue
	 * and block until data is ready.
	 * 
	 */
	@Override
	public void run() {
		running = true;
		BlockState bs = null;
		
		try {
			while( (bs = queue.pop()) != null ) {
				;
			}
		}
		catch(InterruptedException e) {
		}
	
		running = false;
	}
	
	public boolean isRunning() { return running; }
}
