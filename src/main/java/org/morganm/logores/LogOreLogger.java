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
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
	
	/* This contains the list of BlockFaces we check for light when lightLevel logging is enabled.  
	 */
	private final BlockFace[] lightFaces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
			BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
	
	private final LogOresPlugin plugin;
	private final LogQueue queue;
	private final HashMap<String, PrevOre> lastOre;
	private final HashMap<String, Integer> flaggedViolations;
	
	private FileWriter writer;
	private HashMap<String, FileWriter> writerPerWorld;
	private boolean running = false;
	private long zombieTime = 0; 
	
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
	private boolean logFilePerWorld = false;
	private boolean logLightLevel = false;
	private List<String> notifyIgnoredWorlds;
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
		minBlocks = plugin.getConfig().getInt("flagging.minBlocks", 10);
		flagRatio = plugin.getConfig().getInt("flagging.ratio", 250);
		maxCaveBlocks = plugin.getConfig().getInt("maxCaveBlocks", 0);
		logFilePerWorld = plugin.getConfig().getBoolean("logFilePerWorld", false);
		logLightLevel = plugin.getConfig().getBoolean("logLightLevel", false);
		
		// ways to tune out false positives on flagging, all of this defaults to off unless turned
		// on in the config file
		maxFlagTime = plugin.getConfig().getInt("flagging.maxTime", 0);
		maxFlagDistance = plugin.getConfig().getInt("flagging.maxDistance", 0);
		verticalVariance = plugin.getConfig().getInt("flagging.allowedVariance.vertical", 0);
		horizontalVariance = plugin.getConfig().getInt("flagging.allowedVariance.horizontal", 0);
		
		notifyOnFlag = plugin.getConfig().getBoolean("flagging.notify", false);
		notifyInCaves = plugin.getConfig().getBoolean("flagging.notifyInCaves", false);
		flagsBeforeNotify = plugin.getConfig().getInt("flagging.flagsBeforeNotify", 3);
		
		notifyIgnoredWorlds = plugin.getConfig().getStringList("flagging.notifyIgnoredWorlds", null);
		
		if( logFilePerWorld && writerPerWorld == null )
			writerPerWorld = new HashMap<String, FileWriter>();

		logFile = plugin.getConfig().getString("logFile", "plugins/LogOres/logOres");
		/*
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
		*/
	}
	
	/**
	 * 
	 * @return writer that was opened on success, null on failure
	 */
	private FileWriter openLogFile(String world) {
		FileWriter fileWriter = null;
		String fileName = null;

		int txtIndex;
		if( (txtIndex = logFile.indexOf(".txt")) != -1 ) {
			logFile = logFile.substring(0, txtIndex);
			log.info(logPrefix + " Changed base logFile path to (dropped .txt): "+logFile);
		}
		
		// if we're only logging one file for all worlds, then don't add world as part of filename
		if( !logFilePerWorld )
			fileName = logFile + ".txt";
		else
			fileName = logFile + "." + world + ".txt";
		
		try {
			// TODO: move to config setting
			File file = new File(fileName);
			if( !file.exists() )
				file.createNewFile();
			fileWriter = new FileWriter(file, true);		// append
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		// indicates error opening the writer
		if( fileWriter == null )
			return null;
		
		if( logFilePerWorld )
			writerPerWorld.put(world, fileWriter);
		else
			writer = fileWriter;
		
		return fileWriter;
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
		
		String eventWorld = event.bs.getWorld().getName();
		
		formatter.format("%-11s broken by %-12s at (x=%6d, y=%4d, z=%6d",
				event.bs.getData().getItemType().toString(),		// MATERIAL name
				event.playerName,
				event.bs.getX(),
				event.bs.getY(),
				event.bs.getZ()
				);
		
		if( logLightLevel ) {
			Block b = event.bs.getBlock();
			byte lightLevel = b.getLightLevel();

			/* In MineCraft, you have to calculate the lightLevel of a solid block (such as ore) by looking
			 * at the lightLevel of adjacent blocks (primarily air blocks, in the case of underground mining).
			 * So we look at the 6 adjacent blocks and just find the highest lightLevel we can of the six
			 * and consider that the visible light level of our ore block.
			 * 
			 * Note that because this method is run asynchronously on another thread, it's possible by the
			 * time we get called, that Bukkit has already replaced the ore block with an air block, thus
			 * the call above to set the lightLevel to that of the block. If we get a non-zero value for the
			 * block that was broken, we just use that and skip the adjacent block check.
			 */
			if( lightLevel == 0 ) {
				for(BlockFace bf : lightFaces) {
					byte ll = b.getFace(bf).getLightLevel();
					if( ll > lightLevel )
						lightLevel = ll;
				}
			}
			
			formatter.format(", l=%2d", lightLevel);
		}

		if( !logFilePerWorld ) {
			sb.append(", world=");
			sb.append(eventWorld);
		}
		
		sb.append(")");
		
		boolean isFlagged = false;
		boolean probablyCave = false;
		
		// if the previous ore was on another world, set it to null, we can't compare distances
		// across worlds, so we ignore this block for the distance/ratio checks
		if( prevOre != null && !eventWorld.equals(prevOre.bs.getWorld().getName()) )
			prevOre = null;
		
		if( prevOre != null ) {
			double distance = event.bs.getBlock().getLocation().distance(prevOre.bs.getBlock().getLocation());
			
			if( distance > minDistance ) {
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
						    (maxFlagDistance == 0 || distance < maxFlagDistance) &&
						    event.nonOreCounter > minBlocks )
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
			boolean ignoreWorld = false;
			
			Player oreBreaker = plugin.getServer().getPlayer(event.playerName);

			// check to see if we should ignore notifications from this world
			if( notifyIgnoredWorlds != null && !notifyIgnoredWorlds.isEmpty() ) {
				for(String world : notifyIgnoredWorlds) {
					if( world.equals(eventWorld) ) {
						ignoreWorld = true;
						break;
					}
				}
			}
			
			// skip notify if player is on ignored notification list
			if( (oreBreaker == null || !isIgnoredNotify(oreBreaker)) && !ignoreWorld )
			{
				Integer flagCount = flaggedViolations.get(event.playerName);
				if( flagCount == null )
					flagCount = Integer.valueOf(0);
				
				flagCount = Integer.valueOf(flagCount+1);
				flaggedViolations.put(event.playerName, flagCount);
				
				if( flagCount >= flagsBeforeNotify ) {
					String msg = logMessage + " [flagCount: "+flagCount+"]";
					if( logFilePerWorld )
						msg = "[world="+eventWorld+"] "+msg;
					
					List<Player> notifyPlayers = getNotifyPlayers();
					for(Player p : notifyPlayers)
						plugin.sendMessage(p, msg);
				}
			}
		}
		
		FileWriter fileWriter = null;
		
		if( logFilePerWorld )
			fileWriter = writerPerWorld.get(eventWorld);
		else
			fileWriter = writer;
		
		if( fileWriter == null )
			fileWriter = openLogFile(eventWorld);

		String dateStamp = "[" + new Date().toString() + "] ";
		try {
			fileWriter.write(dateStamp + logMessage);
		}
		catch(IOException e) {
			if( e.getMessage().contains("Stream closed") ) {	// ugh terrible code based on implementation message, I know
				openLogFile(eventWorld);
				fileWriter.write(dateStamp + logMessage);
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

		/*
		if( writer == null && !openLogFile() ) {
			log.severe(logPrefix + " Error opening log file, logger shutting down!");
			plugin.shutdownPlugin();
			running = false;
			return;
		}
		*/
		
		int errors = 0;
		int flushCount = 0;
//		while( errors < MAX_ERRORS && !queue.isEmpty() ) {
		while( errors < MAX_ERRORS ) {
			flushCount++;
			
			try {
				zombieTime = 0;
				// block until we have a new event to process
				event = queue.pop();
				zombieTime = System.currentTimeMillis();
				
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

			// if the queue is empty, or we've logged more than a set number of messages since
			// our last flush, force a flush now.
			if( queue.isEmpty() || flushCount > 20 ) {
				flushWriters();
				flushCount = 0;
			}
			
			// if the plugin has been shutdown and the queue is empty, time to exit
			if( !plugin.isEnabled() && queue.isEmpty() )
				break;
		}
		
		if( errors >= MAX_ERRORS ) {
			log.severe(logPrefix + " LogOre logger gave up after "+MAX_ERRORS+" errors");
		}

		flushWriters();
		
		running = false;
	}
	
	private void flushWriters() {
		try {
			if( logFilePerWorld ) {
				for(FileWriter fileWriter : writerPerWorld.values())
					fileWriter.flush();
			}
			else
				writer.flush();
		} catch(IOException e) { e.printStackTrace(); }
	}
	
	public boolean isRunning() { return running; }
	
	/** Check to see if we are a zombie - ie. we died in the middle of a run unexpectedly.
	 * 
	 * @return
	 */
	public boolean isZombie() {
		if( !running )
			return false;
		
		if( zombieTime != 0 && (System.currentTimeMillis() - zombieTime) > 5000 )
			return true;
		else
			return false;
	}
	
	public void reset() {
		running = false;
		zombieTime = 0;
	}
	
	/** Private class for keeping track of the previous ore found by a given player.
	 */
	private class PrevOre {
		BlockState bs;
		long time;
	}
}
