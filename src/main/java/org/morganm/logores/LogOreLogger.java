/**
 * 
 */
package org.morganm.logores;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;

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
	private final HashMap<String, Integer> flaggedViolations;
	
	private FileWriter writer;
	private boolean running = false;
	
	private int minDistance;
	private int minBlocks;
	private int flagRatio;
	private int maxCaveBlocks;
	private int maxFlagTime;
	private int maxFlagDistance;
	private int verticalVariance;
	private int horizontalVariance;
	private int flagsBeforeNotify;
	private boolean notifyOnFlag = false;
	private boolean notifyInCaves = false;
	private String logFile;
	
	public LogOreLogger(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.queue = plugin.getLogQueue();
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
		
		lastOre = new HashMap<String, PrevOre>();
		flaggedViolations = new HashMap<String, Integer>();
	}
	
	public void reloadConfig() {
		minDistance = plugin.getConfig().getInt("minDistance", 5);
		minBlocks = plugin.getConfig().getInt("minBlocks", 10);
		flagRatio = plugin.getConfig().getInt("flagging.ratio", 250);
		maxCaveBlocks = plugin.getConfig().getInt("maxCaveBlocks", 0);
		
		// ways to tune out false positives on flagging, all of this defaults to off unless turned
		// on in the config file
		maxFlagTime = plugin.getConfig().getInt("flagging.maxTime", 0);
		maxFlagDistance = plugin.getConfig().getInt("flagging.maxDistance", 0);
		verticalVariance = plugin.getConfig().getInt("flagging.allowedVariance.vertical", 0);
		horizontalVariance = plugin.getConfig().getInt("flagging.allowedVariance.horizontal", 0);
		
		notifyOnFlag = plugin.getConfig().getBoolean("flagging.notify", false);
		notifyInCaves = plugin.getConfig().getBoolean("flagging.notifyInCaves", false);
		flagsBeforeNotify = plugin.getConfig().getInt("flagging.flagsBeforeNotify", 3);
		
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
		
		formatter.format("%-11s broken by %-12s at (x=%6d, y=%4d, z=%6d, world=%s)",
				event.bs.getData().getItemType().toString(),		// MATERIAL name
				event.playerName,
				event.bs.getX(),
				event.bs.getY(),
				event.bs.getZ(),
				event.bs.getWorld().getName()
				);
		
		boolean isFlagged = false;
		boolean probablyCave = false;
		
		if( prevOre != null ) {
			double distance = event.bs.getBlock().getLocation().distance(prevOre.bs.getBlock().getLocation());
			
			if( distance > minDistance && event.nonOreCounter > minBlocks ) {
				long time = event.time - prevOre.time;
				if( time != 0 )
					time = time / 1000;
				
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
					{
						// make sure we are within flaggable configuration limits
						if( (maxFlagTime == 0 || time < maxFlagTime) &&
						    (maxFlagDistance == 0 || distance < maxFlagDistance) )
						{
							// if we are here, this is a flagged entry, unless the variance checks below change that fact
							isFlagged = true;
							
							// if allowed variances are defined, check those too
							if( verticalVariance != 0 || horizontalVariance != 0 ) {
								boolean inHorizontalVariance = false;
								boolean inVerticalVariance = false;
								
								Location blockLocation = event.bs.getBlock().getLocation();
								Location prevBlockLocation = prevOre.bs.getBlock().getLocation();
								
								int varianceX = blockLocation.getBlockX() - prevBlockLocation.getBlockX();
								int varianceZ = blockLocation.getBlockZ() - prevBlockLocation.getBlockZ();
								
								if( Math.abs(varianceX) <= horizontalVariance || Math.abs(varianceZ) <= horizontalVariance )
									inHorizontalVariance = true;
								
								int varianceY = blockLocation.getBlockY() - prevBlockLocation.getBlockY();
								if( Math.abs(varianceY) <= verticalVariance )
									inVerticalVariance = true;
								
//								System.out.println("varianceX = "+varianceX+", varianceZ = "+varianceZ+", varianceY = "+varianceY);
								// are we in within variance limits?  if so, don't flag this entry
								if( inHorizontalVariance && inVerticalVariance )
									isFlagged = false;
							}
							
							if( isFlagged )
								sb.append(" [flagged]");
							else
								sb.append(" [in variance]");
						}
					}
					
					// if we've mined less blocks than the distance, it probably indicates we're in a cave with
					// lots of open space between visible ores.  We use adjusted distance to make sure we don't
					// accidentally flag a cave when it's really just adjacent/nearby ore blocks
					if( event.nonOreCounter < (distance-minDistance) && (maxCaveBlocks == 0 || event.nonOreCounter < maxCaveBlocks) ) {
						probablyCave = true;
						sb.append(" [cave?]");
					}
				}
			}
		}
		
		sb.append("\n");

		String logMessage = sb.toString();
		
		// if it's flagged, notify if we're supposed to, unless it's a cave and we're not supposed to
		if( (notifyOnFlag && isFlagged) && (notifyInCaves || !probablyCave) ) {
			Player oreBreaker = plugin.getServer().getPlayer(event.playerName);
			
			if( oreBreaker == null || !isIgnoredNotify(oreBreaker) )	// skip if player is on ignored notification list
			{
				Integer flagCount = flaggedViolations.get(event.playerName);
				if( flagCount == null )
					flagCount = Integer.valueOf(0);
				
				flagCount = Integer.valueOf(flagCount+1);
				flaggedViolations.put(event.playerName, flagCount);
				
				if( flagCount >= flagsBeforeNotify ) {
					List<Player> notifyPlayers = getNotifyPlayers();
					for(Player p : notifyPlayers)
						plugin.sendMessage(p, logMessage + " [flagCount: "+flagCount+"]");
				}
			}
		}
		
		if( writer == null )
			openLogFile();

		String dateStamp = "[" + new Date().toString() + "] ";
		try {
			writer.write(dateStamp + logMessage);
		}
		catch(IOException e) {
			if( e.getMessage().contains("Stream closed") ) {	// ugh terrible code based on implementation message, I know
				openLogFile();
				writer.write(dateStamp + logMessage);
			}
		}
	}
	
	private boolean isIgnoredNotify(Player p) {
		return plugin.hasPermission(p, "logores.notify.ignore");
	}
	
	/** Return the list of players to be notified on a flagged event.
	 * 
	 * @return
	 */
	private List<Player> getNotifyPlayers() {
		List<Player> players = new ArrayList<Player>();
		
		Player[] onlinePlayers = plugin.getServer().getOnlinePlayers();
		for(int i=0; i < onlinePlayers.length; i++) {
			if( plugin.hasPermission(onlinePlayers[i], "logores.notify") )
				players.add(onlinePlayers[i]);
		}
		
		return players;
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
