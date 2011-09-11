/**
 * 
 */
package org.morganm.logores;

import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import org.morganm.logores.RecentBlocks.RecentBlock;

/**
 * @author morganm
 *
 */
public class LogOresBlockListener extends BlockListener {
	private static final int MIN_BLOCK_ID = 13;
	
	private LogOresPlugin plugin;
	private LogQueue logQueue;
	private int[] logOres;
	
	public LogOresBlockListener(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.logQueue = plugin.getLogQueue();
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;
		
		boolean foundOre = false;
		
		String playerName = event.getPlayer().getName();
		
		// get or create the current nonOreCounter object for this player
		Counter nonOreCounter = plugin.playerNonOreCount.get(playerName);
		if( nonOreCounter == null ) {
			nonOreCounter = new Counter();
			plugin.playerNonOreCount.put(playerName, nonOreCounter);
		}
		
		RecentBlocks recentBlocks = plugin.playerRecentBlocks.get(playerName);
		if( recentBlocks == null ) {
			recentBlocks = new RecentBlocks();
			plugin.playerRecentBlocks.put(playerName, recentBlocks);
		}
		
		Block b = event.getBlock();
		int blockType = b.getTypeId();
		
		// store the recent block mined.  This is TERRIBLE OOP, but I'm doing this
		// for efficiency since this is called on every block break, so that we have
		// minimal overhead on the main Bukkit processing thread, by minimizing
		// function calls.
		RecentBlock rb = recentBlocks.recentBlocks[recentBlocks.head];
		if( rb == null )
			rb = recentBlocks.newRecentBlock(recentBlocks.head);
		
		recentBlocks.head++;
		if( recentBlocks.head >= RecentBlocks.MAX_BLOCKS )
			recentBlocks.head = 0;
		rb.worldName = b.getWorld().getName();
		rb.x = b.getX();
		rb.y = b.getY();
		rb.z = b.getZ();
		rb.blockType = blockType;
		rb.uniqueString = null;

		// MIN_BLOCK_ID check is a performance boost; the lowest valuable gem is
		// GOLD at ID 14, so with a single if check we can eliminate any additional
		// checks for the majority of blocks broken when mining (dirt, stone, grass
		// cobble, sand, gravel).
		if( blockType > MIN_BLOCK_ID )
		{
			// check to see if this is an ore we should log
			for(int i=0; i < logOres.length; i++) {
	//			System.out.println("Checking blockType "+blockType+" against "+logOres[i]);
				if( blockType == logOres[i] ) {
					logQueue.push(new LogEvent(playerName, event.getBlock().getState(),
							System.currentTimeMillis(), nonOreCounter.counter, nonOreCounter.nonDiamondCounter));
					nonOreCounter.counter = 0;	// reset non-ore counter
					
					// we have a separate counter for diamonds, used when paranoidDiamonds is true
					if( blockType == 56 )
						nonOreCounter.nonDiamondCounter = 0;
					
					foundOre = true;
					break;
				}
			}
		}
		
		if( !foundOre ) {
			nonOreCounter.counter++;
			nonOreCounter.nonDiamondCounter++;
		}
	}

	public void reloadConfig() {
		this.logOres = plugin.getLogOresConfig().getLogIds();
	}
}
