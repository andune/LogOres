/**
 * 
 */
package org.morganm.logores;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;

/**
 * @author morganm
 *
 */
public class LogOresBlockListener extends BlockListener {
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
		
		int blockType = event.getBlock().getTypeId();
		
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
		
		if( !foundOre ) {
			nonOreCounter.counter++;
			nonOreCounter.nonDiamondCounter++;
		}
	}

	public void reloadConfig() {
		this.logOres = plugin.getLogOresConfig().getLogIds();
	}
}
