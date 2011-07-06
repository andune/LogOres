/**
 * 
 */
package org.morganm.logores;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

/** This class is responsible for actually writing the logs.
 * 
 * @author morganm
 *
 */
public class LogOreLogger implements Runnable {
	private final int MAX_ERRORS = 10;
	private final Logger log;
	private final String logPrefix;
	
	private final LogOresPlugin plugin;
	private final LogQueue queue;
	
	private FileWriter writer;
	private boolean running = false;
	
	public LogOreLogger(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.queue = plugin.getLogQueue();
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
	}
	
	private boolean openLogFile() {
		boolean error = false;
		
		try {
			writer = new FileWriter("plugin/LogOres/oreLog.txt");
		}
		catch(Exception e) {
			error = true;
			e.printStackTrace();
		}
		
		return error;
	}
	
	/** Write a given block out to the logfile.
	 * 
	 * @param bs
	 */
	private void logBlock(LogEvent event) throws IOException {
		StringBuffer sb = new StringBuffer();
		sb.append("[");
		sb.append(new Date().toString());
		sb.append("] ");
		sb.append(event.bs.getData().toString());
		sb.append(" broken by ");
		sb.append(event.playerName);
		sb.append(" at (x=");
		sb.append(event.bs.getX());
		sb.append(",y=");
		sb.append(event.bs.getY());
		sb.append(",z=");
		sb.append(event.bs.getZ());
		sb.append(",world=");
		sb.append(event.bs.getWorld().getName());
		sb.append(")");
		
		writer.write(sb.toString());
	}
	
	/** This method will be called once by Bukkit Scheduler.  It will try to read the Queue
	 * and block until data is ready.
	 * 
	 */
	@Override
	public void run() {
		running = true;
		LogEvent event = null;

		if( !openLogFile() ) {
			log.severe(logPrefix + " Error opening log file, logger shutting down!");
			plugin.shutdownPlugin();
			running = false;
			return;
		}
		
		int errors = 0;
		try {
			while( errors < MAX_ERRORS && (event = queue.pop()) != null ) {
				try {
					logBlock(event);
				} catch(IOException e) {
					log.warning(logPrefix + " Error writing to log file");
					e.printStackTrace();
					errors++;
				}
				
				// if the plugin has been shutdown and the queue is empty, time to exit
				if( !plugin.isEnabled() && queue.isEmpty() )
					break;
			}
		}
		catch(InterruptedException e) {}
		
		if( errors > 10 ) {
			log.severe(logPrefix + " LogOre logger exited after "+MAX_ERRORS+" errors");
		}
	
		try {
			writer.close();
		}
		catch(IOException e) { e.printStackTrace(); }
		
		running = false;
	}
	
	public boolean isRunning() { return running; }
}
