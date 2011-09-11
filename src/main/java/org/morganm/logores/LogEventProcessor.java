/**
 * 
 */
package org.morganm.logores;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.morganm.logores.Logger.EventLogger;
import org.morganm.logores.Logger.ExcelFileLogger;
import org.morganm.logores.Logger.FileLogger;

/** Class to process LogEvents and turn them into a ProcessedEvent, which will
 * then be logged to whatever log systems the admin has configured.
 * 
 * @author morganm
 *
 */
public class LogEventProcessor implements Runnable {
	private final int MAX_ERRORS = 10;
	private final Block[] emptyBlockList = new Block[] {};
	private final Logger log;
	private final String logPrefix;

	/* This contains the list of BlockFaces we check for light when lightLevel logging is enabled.  
	 */
	private static final BlockFace[] lightFaces = {BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
			BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST};
	private static final HashMap<BlockFace, BlockFace> oppositeFaces;
	
	static {
		oppositeFaces = new HashMap<BlockFace, BlockFace>(6);
		oppositeFaces.put(BlockFace.UP, BlockFace.DOWN);
		oppositeFaces.put(BlockFace.DOWN, BlockFace.UP);
		oppositeFaces.put(BlockFace.EAST, BlockFace.WEST);
		oppositeFaces.put(BlockFace.WEST, BlockFace.EAST);
		oppositeFaces.put(BlockFace.NORTH, BlockFace.SOUTH);
		oppositeFaces.put(BlockFace.SOUTH, BlockFace.NORTH);
	}
	
	private LogOresPlugin plugin;
	private final LogQueue queue;
	private final HashMap<String, PrevOre> lastOre;
	private final HashMap<String, PrevOre> lastDiamond;
	private final HashMap<String, Integer> flaggedViolations;
	
	private List<EventLogger> loggers;

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
	private boolean logLightLevel = false;
	private boolean flagWhenZeroLight = false;
	private boolean isParanoidDiamonds = false;
	private List<String> notifyIgnoredWorlds;	
	
	public LogEventProcessor(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.queue = plugin.getLogQueue();
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
		
		lastOre = new HashMap<String, PrevOre>();
		lastDiamond = new HashMap<String, PrevOre>();
		flaggedViolations = new HashMap<String, Integer>();
	}
	
	public void reloadConfig() {
		minDistance = plugin.getConfig().getInt("minDistance", 5);
		minBlocks = plugin.getConfig().getInt("flagging.minBlocks", 10);
		flagRatio = plugin.getConfig().getInt("flagging.ratio", 250);
		maxCaveBlocks = plugin.getConfig().getInt("maxCaveBlocks", 0);
		logLightLevel = plugin.getConfig().getBoolean("logLightLevel", true);
		isParanoidDiamonds = plugin.getConfig().getBoolean("paranoidDiamonds", true);
		
		// ways to tune out false positives on flagging, all of this defaults to off unless turned
		// on in the config file
		maxFlagTime = plugin.getConfig().getInt("flagging.maxTime", 0);
		maxFlagDistance = plugin.getConfig().getInt("flagging.maxDistance", 0);
		verticalVariance = plugin.getConfig().getInt("flagging.allowedVariance.vertical", 0);
		horizontalVariance = plugin.getConfig().getInt("flagging.allowedVariance.horizontal", 0);
		
		notifyOnFlag = plugin.getConfig().getBoolean("flagging.notify", false);
		notifyInCaves = plugin.getConfig().getBoolean("flagging.notifyInCaves", false);
		flagsBeforeNotify = plugin.getConfig().getInt("flagging.flagsBeforeNotify", 3);
		flagWhenZeroLight = plugin.getConfig().getBoolean("flagging.flagWhenZeroLight", true);
		
		notifyIgnoredWorlds = plugin.getConfig().getStringList("flagging.notifyIgnoredWorlds", null);
		
		// TODO: need to flush existing loggers before we load new ones
		
		loggers = new ArrayList<EventLogger>();
		List<String> logTypes = plugin.getConfig().getStringList("logTypes", null);
		for(String logType : logTypes) {
			if( logType.equals("file") ) {
				loggers.add(new FileLogger(plugin).init());
			}
			else if( logType.equals("excel") ) {
				loggers.add(new ExcelFileLogger(plugin).init());
			}
			/* TODO: write logger
			else if( logType.equals("database") ) {
			}
			*/
			else {
				log.warning(logPrefix + " Ignoring invalid log type "+logType);
			}
		}
		
		if( logTypes.isEmpty() ) {
			log.warning(logPrefix + " WARNING: No loggers defined! Using default file logger.");
			loggers.add(new FileLogger(plugin).init());
		}
	}
	
	/** Given the input parameters, calculate the resulting ratio that goes into the log.
	 * 
	 * @param seconds
	 * @param numBlocks
	 * @param distance
	 * @return
	 */
	private double calculateRatio(long seconds, int numBlocks, double distance) {
		double ratio = 0;
		
		// if numBlocks is 0, consider it as "1" so we don't divide by zero below - basically
		// just sets the final divisor to whatever the distance is.
		double divisor = numBlocks;
		if( divisor == 0 )
			divisor = 1;
		
		divisor = distance / divisor;
		
		if( divisor != 0 && seconds != 0 )
			ratio = seconds / divisor;
		
		return ratio;
	}

	/** Increment and return the current flagCounter for a given player.
	 * 
	 * @param playerName
	 * @return
	 */
	private int incrementFlagCount(String playerName) {
		Integer flagCount = flaggedViolations.get(playerName);
		if( flagCount == null )
			flagCount = Integer.valueOf(0);
		
		flagCount = Integer.valueOf(flagCount+1);
		flaggedViolations.put(playerName, flagCount);
		
		return flagCount;
	}
	
	private ProcessedEvent processEvent(LogEvent event, PrevOre prevOre, PrevOre prevDiamond) {
		ProcessedEvent pe = new ProcessedEvent(event);
		
		pe.eventWorld = event.bs.getWorld().getName();
		
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
			 * the call above to get the lightLevel of the block. If we get a non-zero value for the
			 * block that was broken, we just use that and skip the adjacent block check.
			 */
			if( lightLevel == 0 ) {
				for(BlockFace bf : lightFaces) {
					byte ll = b.getRelative(bf).getLightLevel();
					if( ll > lightLevel )
						lightLevel = ll;
				}
			}
			
			pe.lightLevel = lightLevel;
		}
		else
			pe.lightLevel = -1;

		pe.isInCave = false;
		
		// if the previous ore was on another world, set it to null, we can't compare distances
		// across worlds, so we ignore this block for the distance/ratio checks
		if( prevOre != null && !pe.eventWorld.equals(prevOre.bs.getWorld().getName()) )
			prevOre = null;
		if( prevDiamond != null && !pe.eventWorld.equals(prevDiamond.bs.getWorld().getName()) )
			prevDiamond = null;
		
		if( prevOre != null ) {
			pe.distance = event.bs.getBlock().getLocation().distance(prevOre.bs.getBlock().getLocation());
			
			if( pe.distance > minDistance ) {		
				pe.time = event.time - prevOre.time;
				if( pe.time != 0 )
					pe.time = pe.time / 1000;
				
				pe.ratio = calculateRatio(pe.time, event.nonOreCounter, pe.distance);
	
				if( pe.ratio > 0 ) {
					pe.isNewOreCluster = true;
					
					if( pe.ratio < flagRatio )
					{
						// make sure we are within flaggable configuration limits
						if( (maxFlagTime == 0 || pe.time < maxFlagTime) &&
						    (maxFlagDistance == 0 || pe.distance < maxFlagDistance) &&
						    event.nonOreCounter > minBlocks )
						{
							boolean inVariance = false;
							
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
									inVariance = true;
							}
							
							if( !inVariance )
								pe.flagReasons |= ProcessedEvent.RATIO_FLAG;
						}
						
						if( flagWhenZeroLight && logLightLevel ) {
							if( pe.lightLevel == 0 ) {
								pe.flagReasons |= ProcessedEvent.NO_LIGHT_FLAG;
							}
						}
					}
					
					if( pe.isFlagged() )
						pe.flagCount = incrementFlagCount(pe.logEvent.playerName);;
					
					pe.isInCave = isInCave(event);
					
					/* OLD CHECK
					// if we've mined less blocks than the distance, it probably indicates we're in a cave with
					// lots of open space between visible ores.  We use adjusted distance to make sure we don't
					// accidentally flag a cave when it's really just adjacent/nearby ore blocks
					if( event.nonOreCounter < (pe.distance-minDistance) && (maxCaveBlocks == 0 || event.nonOreCounter < maxCaveBlocks) ) {
						pe.isInCave = true;
					}
					*/
				}
			}
		}
		
		
		/* These things must be true in order to enter paranoid diamond checks:
		 * 
		 *   - paranoidDiamond flag must be on
		 *   - we must have had a previous diamond to compare to
		 *   - the current block being checked must be a diamond
		 *   - we must not already be flagged "normally" (this is to help distinguish between events
		 *     that are normally flagged vs. those that are paranoid diamond flagged)
		 */
		if( isParanoidDiamonds && prevDiamond != null && event.bs.getTypeId() == 56 && !pe.isFlagged() ) {
			double distance = event.bs.getBlock().getLocation().distance(prevDiamond.bs.getBlock().getLocation());
			
			if( distance > 3 ) {
				long time = event.time - prevDiamond.time;
				if( time != 0 )
					time = time / 1000;
				
				double ratio = calculateRatio(time, event.nonDiamondCounter, distance);
	
				if( ratio < flagRatio ) {
					pe.flagReasons |= ProcessedEvent.RATIO_FLAG;
				
					// if we get flagged for paranoidDiamonds, then the logged ratio is defined as
					// the ratio between the two diamonds
					pe.ratio = ratio;
				}
				
				if( flagWhenZeroLight && logLightLevel ) {
					if( pe.lightLevel == 0 ) {
						pe.flagReasons |= ProcessedEvent.NO_LIGHT_FLAG;
					}
				}
				
				// if we're a flagged entry, then add that the reason we're flagged is because of
				// the paranoidDiamond setting.
				if( pe.isFlagged() ) {
					pe.flagCount = incrementFlagCount(pe.logEvent.playerName);;
					pe.flagReasons |= ProcessedEvent.PARANOID_DIAMOND_FLAG;
					
					// paranoidDiamonds never have cave flag set
					pe.isInCave = false;
					
					// if it's a flagged paranoidDiamonds event, we need to set isNewOreCluster to
					// true so it will be logged.
					pe.isNewOreCluster = true;
				}
			}
		}
		
		doNotify(pe);
		
		return pe;
	}
	
	/** Check to determine if the given event mined was in a cave or not.  This started as
	 * a simplistic block count, but that turned out to be too simple and got lots of
	 * false positives.  So now this algorithm leverages the new recentBlocks that are
	 * tracked to search for air blocks around the mined block and see if they were mined
	 * by the player or not.  If they were not, then it's a cave (since they didn't mine
	 * it).
	 * 
	 * This happens asynchronously from the main Bukkit thread, that means we can't necessarily
	 * count on the state being accurate when we check it (ie. the player may have mined another
	 * 2 or 3 blocks by the time we get called).  That's OK: worst case we wrongly think that
	 * the player is NOT in a cave, in which case we get might get a false positive alert, which
	 * is no worse off than we were before this check was written.  Best case, we accurately
	 * detect a cave and avoid what would otherwise be a false positive.
	 * 
	 * @param event
	 * @return
	 */
	private boolean isInCave(LogEvent event) {
		BlockState bs = event.bs;
		Block[] blocks = getNearAirBlocks(bs.getBlock());
//		log.info("airBlocks.length = "+blocks.length);
		
		RecentBlocks rbs = plugin.playerRecentBlocks.get(event.playerName);
		// if no recent blocks, then we don't do any cave checking
		if( rbs == null )
			return false;
		
		int playerMined = 0;
		
		// loop through all air blocks found and check the players recent blocks to
		// determine how many of them are player mined blocks.  We try to make this
		// as efficient as possible by comparing the blocks as "uniqueStrings", this
		// means the check is reduced to a simple, efficient string comparison
		// for every loop past the first one (when the strings are first built),
		// although efficiency is less important anyway as this is all happening on
		// a separate thread from the main Bukkit thread.
		for(int i = 0; i < blocks.length; i++) {
			String uniqueString = RecentBlocks.uniqueString(blocks[i].getWorld().getName(), blocks[i].getX(),
					blocks[i].getY(), blocks[i].getZ());
			
			// record head locally as it could change concurrently while we are looping
			int head = rbs.head;
			
			// these next two checks shouldn't ever happen and should be unnecessary, but they
			// defensively protect us from an infinite loop as a result of bad code elsewhere
			// in the system.
			if( head < 0 )
				head = 0;
			if( head > RecentBlocks.MAX_BLOCKS-1 )
				head = RecentBlocks.MAX_BLOCKS-1;
			
			int j = head-1;
			if( j < 0 )
				j = RecentBlocks.MAX_BLOCKS-1;
			do {
				if( rbs.recentBlocks[j] != null && rbs.recentBlocks[j].worldName != null ) {
					String blockUniqueString = rbs.recentBlocks[j].getUniqueString();
//					log.info("checking "+uniqueString+" against "+rbs.recentBlocks[j].getUniqueString());
					if( uniqueString.equals(blockUniqueString) ) {
//						log.info("MATCH!\n");
						playerMined++;
						break;
					}
				}
				
				if( --j < 0 )
					j = RecentBlocks.MAX_BLOCKS-1;
			} while(j != head);
		}

		int percentage = 100;	// fail-open: ie. default to assuming this is NOT a cave 
		
		// calculate playerMined blocks as a percentage of the airblocks found
		if( blocks.length != 0 )
			percentage = (playerMined * 100) / blocks.length;

		boolean isInCave = false;
		// if greater than 50% of the air blocks were player mined, then we consider
		// this to NOT have been a cave.
		if( percentage < 50 )
			isInCave = true;
		
//		log.info("isInCave()="+isInCave+", percentage = "+percentage+", block.length = "+blocks.length+", playerMined = "+playerMined);
		
		return isInCave;
	}
	
	/** Given a BlockState, return the nearby air blocks.  The interface is intentionally not
	 * specific as to how many air blocks are returned, it is up to the implementation as to
	 * how many blocks are checked and therefore returned.
	 * 
	 * @param bs
	 * @return
	 */
	private Block[] getNearAirBlocks(Block b) {
		HashMap<Block, Boolean> list = new HashMap<Block, Boolean>(10);
		getNearAirBlocksRecursive(list, b, 0, null);
		return list.keySet().toArray(emptyBlockList);
	}

	/** This method is called internally to recursive fill the list with nearby Air blocks,
	 * basically by recursively checking each of the 6 accessible adjacent blocks.
	 * 
	 * @param list
	 * @param b
	 * @param count
	 */
	private void getNearAirBlocksRecursive(HashMap<Block, Boolean> list, Block b, int count, BlockFace from) {
		if( ++count > 10 )
			return;
		
		for(BlockFace bf : lightFaces) {
			// don't backtrack the way we just came
			if( bf == from )
				continue;
			
			Block nearBlock = b.getRelative(bf);
			
			// don't ever add the same block twice
			if( list.get(nearBlock) != null )
				continue;
			
//			log.info("aircheck block "+bf.toString()+" is type "+nearBlock.getTypeId());
			if( nearBlock.getTypeId() == 0 ) {
				list.put(nearBlock, Boolean.TRUE);
				getNearAirBlocksRecursive(list, nearBlock, count+1, oppositeFaces.get(bf));
			}
		}
	}
	
	private void doNotify(ProcessedEvent pe) {
		// if it's flagged, notify if we're supposed to, unless it's a cave and we're not supposed to
		if( (notifyOnFlag && pe.isFlagged()) && (notifyInCaves || !pe.isInCave) ) {
			boolean ignoreWorld = false;
			
			Player oreBreaker = plugin.getServer().getPlayer(pe.logEvent.playerName);

			// check to see if we should ignore notifications from this world
			if( notifyIgnoredWorlds != null && !notifyIgnoredWorlds.isEmpty() ) {
				for(String world : notifyIgnoredWorlds) {
					if( world.equals(pe.eventWorld) ) {
						ignoreWorld = true;
						break;
					}
				}
			}
			
			// skip notify if player is on ignored notification list
			if( (oreBreaker == null || !isIgnoredNotify(oreBreaker)) && !ignoreWorld )
			{
				if( pe.flagCount >= flagsBeforeNotify ) {
					String msg = FileLogger.getLogString(pe, true);
					
					List<Player> notifyPlayers = getNotifyPlayers();
					for(Player p : notifyPlayers)
						plugin.sendMessage(p, msg);
				}
			}
		}		
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
		
		int errors = 0;
		while( errors < MAX_ERRORS && !queue.isEmpty() ) {
			try {
				event = queue.pop();
				
				PrevOre prevOre = lastOre.get(event.playerName);
				PrevOre prevDiamond = lastDiamond.get(event.playerName);

				ProcessedEvent pe = processEvent(event, prevOre, prevDiamond);

				if( prevOre == null ) {
					prevOre = new PrevOre();
					lastOre.put(event.playerName, prevOre);
				}
				
				// we save CPU by just re-using the same private class rather than creating
				// a new object every time.
				prevOre.bs = event.bs;
				prevOre.time = event.time;
				
				// is paranoidDiamond flag on and was it a diamond?
				if( isParanoidDiamonds && event.bs.getTypeId() == 56 ) {
					if( prevDiamond == null ) {
						prevDiamond = new PrevOre();
						lastDiamond.put(event.playerName, prevDiamond);
					}
					
					prevDiamond.bs = event.bs;
					prevDiamond.time = event.time;
				}

				logEvent(pe);
			}
			catch(InterruptedException e) {}

			// if the plugin has been shutdown and the queue is empty, time to exit
			if( !plugin.isEnabled() && queue.isEmpty() )
				break;
		}
		
		if( errors >= MAX_ERRORS ) {
			log.severe(logPrefix + " LogOre logger gave up after "+MAX_ERRORS+" errors");
		}
		
		flushLoggers();

		running = false;
	}
	
	private void logEvent(ProcessedEvent pe) {
		for(EventLogger logger : loggers) {
			try {
				logger.logEvent(pe);
			}
			catch(Exception e) {
				log.warning(logPrefix + " error logging event");
				e.printStackTrace();
			}
		}
	}
	
	private void flushLoggers() {
		for(EventLogger logger : loggers) {
			logger.flush();
		}
	}
	
	public void close() {
		for(EventLogger logger : loggers) {
			try {
				logger.close();
			}
			catch(Exception e) {
				log.warning(logPrefix + " error closing log object");
				e.printStackTrace();
			}
		}
	}
	
	private boolean isIgnoredNotify(Player p) {
		return plugin.hasPermission(p, "logores.notify.ignore");
	}
	
	/** Private class for keeping track of the previous ore found by a given player.
	 */
	private class PrevOre {
		BlockState bs;
		long time;
	}
}
