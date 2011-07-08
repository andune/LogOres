/**
 * 
 */
package org.morganm.logores;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.block.BlockState;

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
	private final HashMap<String, PrevOre> lastOre;
	
	private FileWriter writer;
	private boolean running = false;
	
	public LogOreLogger(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.queue = plugin.getLogQueue();
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
		
		lastOre = new HashMap<String, PrevOre>();
	}
	
	/**
	 * 
	 * @return true if logfile opened OK, false on error
	 */
	private boolean openLogFile() {
		boolean error = true;
		
		try {
			// TODO: move to config setting
			File file = new File("plugins/LogOres/oreLog.txt");
			if( !file.exists() )
				file.createNewFile();
			writer = new FileWriter(file, true);		// append
			error = false;
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		return !error;
	}
	
	public void close() throws IOException {
		if( writer != null )
			writer.close();
	}
	
	/** Write a given block out to the logfile.
	 * 
	 * @param bs
	 */
	private void logBlock(LogEvent event, PrevOre prevOre) throws IOException {
		StringBuffer sb = new StringBuffer();
		Formatter formatter = new Formatter(sb, Locale.US);
		
		formatter.format("[%s] %-11s broken by %-12s at (x=%6d, y=%4d, z=%6d, world=%s)",
				new Date().toString(),
				event.bs.getData().getItemType().toString(),		// MATERIAL name
				event.playerName,
				event.bs.getX(),
				event.bs.getY(),
				event.bs.getZ(),
				event.bs.getWorld().getName()
				);
		
		if( prevOre != null ) {
			double distance = event.bs.getBlock().getLocation().distance(prevOre.bs.getBlock().getLocation());
			long time = System.currentTimeMillis() - prevOre.time;
			if( time != 0 )
				time = time / 1000;
			
			// we adjust the distance by subtracting 3 from the last block, basically setting the
			// ratio to 0 anytime the last block was within 3 of this block, in effect ignoring
			// blocks that are close to each other.
			double adjustedDistance = distance - 3;
			if( adjustedDistance < 0 )
				adjustedDistance = 0;
			
			double ratio = 0;
			if( adjustedDistance != 0 && time != 0 )
				ratio = time / distance;

			if( ratio > 0 )
				formatter.format(" [d=%3.0f, t=%4dsec, r=%3.1f]",
						distance,
						time,
						ratio
						);
		}
		
		sb.append("\n");
		
		/*
		sb.append("[");
		sb.append(new Date().toString());
		sb.append("] ");
		sb.append(event.bs.getData().getItemType().toString());
		sb.append(" broken by ");
		sb.append(event.playerName);
		sb.append(" at (x=");
		sb.append(event.bs.getX());
		sb.append(", y=");
		sb.append(event.bs.getY());
		sb.append(", z=");
		sb.append(event.bs.getZ());
		sb.append(", world=");
		sb.append(event.bs.getWorld().getName());
		sb.append(")\n");
		*/
		
		if( writer == null )
			openLogFile();

//		System.out.println("Writing :"+sb.toString());
		writer.write(sb.toString());
		writer.flush();
	}
	
	/** This method will be called once by Bukkit Scheduler.  It will try to read the Queue
	 * and block until data is ready.
	 * 
	 */
	@Override
	public void run() {
		running = true;
		LogEvent event = null;

//		System.out.println("LogOreLogger running");
		
		if( !openLogFile() ) {
			log.severe(logPrefix + " Error opening log file, logger shutting down!");
			plugin.shutdownPlugin();
			running = false;
			return;
		}
		
		int errors = 0;
		try {
//			System.out.println("blocking waiting for event");
			while( errors < MAX_ERRORS && (event = queue.pop()) != null ) {
				try {
//					System.out.println("got event");
					PrevOre prevOre = lastOre.get(event.playerName);
					
					logBlock(event, prevOre);
					
					if( prevOre == null ) {
						prevOre = new PrevOre();
						lastOre.put(event.playerName, prevOre);
					}
					
					// we save CPU by just re-using the same private class rather than creating
					// a new object every time.
					prevOre.bs = event.bs;
					prevOre.time = System.currentTimeMillis();
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
	
	/** Private class for keeping track of the previous ore found by a given player.
	 */
	private class PrevOre {
		BlockState bs;
		long time;
	}
}
