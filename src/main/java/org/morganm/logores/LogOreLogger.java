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
	
	private int minDistance;
	private int minBlocks;
	private int flagRatio;
	private String logFile;
	
	public LogOreLogger(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.queue = plugin.getLogQueue();
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
		
		lastOre = new HashMap<String, PrevOre>();
	}
	
	public void reloadConfig() {
		minDistance = plugin.getConfig().getInt("minDistance", 5);
		minBlocks = plugin.getConfig().getInt("minBlocks", 10);
		flagRatio = plugin.getConfig().getInt("flagRatio", 250);
		
		String oldLogPath = logFile;
		// if the logFile changed, close the old file and open the new one
		if( (logFile = plugin.getConfig().getString("logFile", "plugins/LogOres/oreLog.txt")).equals(oldLogPath) ) {
			if( writer != null ) {
				try {
					writer.close();
				} catch(IOException e) { e.printStackTrace(); }
			}
			
			openLogFile();
		}
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
			
			if( distance > minDistance && event.nonOreCounter > minBlocks ) {
				long time = event.time - prevOre.time;
				if( time != 0 )
					time = time / 1000;
				
				boolean probablyCave = false;
				// if we've mined less blocks than the distance, it probably indicates we're in a cave with
				// lots of open space between visible ores.  We use adjusted distance to make sure we don't
				// accidentally flag a cave when it's really just adjacent/nearby ore blocks
				if( event.nonOreCounter < (distance-minDistance) )
					probablyCave = true;
				
				// if nonOreCounter is 0, consider it as "1" so we don't divide by zero below - basically
				// just sets the final divisor to whatever the distance is.
				double divisor = event.nonOreCounter;
				if( divisor == 0 )
					divisor = 1;
				
				divisor = distance / divisor;
				
				double ratio = 0;
				if( divisor != 0 && time != 0 )
					ratio = time / divisor;
	
				if( ratio > 0 ) {
					formatter.format(" [t=%4dsec / (d=%3.0f / b=%4d) = r=%3.1f]",
							time,
							distance,
							event.nonOreCounter,
							ratio
							);
					
					if( ratio < flagRatio )
						sb.append(" [flagged]");
					
					if( probablyCave )
						sb.append(" [cave?]");
				}
			}
		}
		
		sb.append("\n");
		
		if( writer == null )
			openLogFile();

		try {
			writer.write(sb.toString());
		}
		catch(IOException e) {
			if( e.getMessage().contains("Stream closed") ) {	// ugh terrible code based on implementation message, I know
				openLogFile();
				writer.write(sb.toString());
			}
		}
	}
	
	/** This method will be called repeatedly every few seconds by the Bukkit Scheduler.
	 * It will read the block queue until empty and then exit.
	 * 
	 */
	@Override
	public void run() {
		// never let us run in two threads at once
		if( running ) 
			return;
		
		running = true;
		LogEvent event = null;

		if( writer == null && !openLogFile() ) {
			log.severe(logPrefix + " Error opening log file, logger shutting down!");
			plugin.shutdownPlugin();
			running = false;
			return;
		}
		
		int errors = 0;
		while( errors < MAX_ERRORS && !queue.isEmpty() ) {
			try {
				event = queue.pop();
				PrevOre prevOre = lastOre.get(event.playerName);

				logBlock(event, prevOre);

				if( prevOre == null ) {
					prevOre = new PrevOre();
					lastOre.put(event.playerName, prevOre);
				}

				// we save CPU by just re-using the same private class rather than creating
				// a new object every time.
				prevOre.bs = event.bs;
				prevOre.time = event.time;
			}
			catch(IOException e) {
				log.warning(logPrefix + " Error writing to log file");
				e.printStackTrace();
				errors++;
			}
			catch(InterruptedException e) {}

			// if the plugin has been shutdown and the queue is empty, time to exit
			if( !plugin.isEnabled() && queue.isEmpty() )
				break;
		}
		
		if( errors > 10 ) {
			log.severe(logPrefix + " LogOre logger gave up after "+MAX_ERRORS+" errors");
		}

		try {
			writer.flush();
		} catch(IOException e) { e.printStackTrace(); }
		
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