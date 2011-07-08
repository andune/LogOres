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
	
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;
		
		int blockType = event.getBlock().getTypeId();
		
		// check to see if this is an ore we should log
		for(int i=0; i < logOres.length; i++) {
//			System.out.println("Checking blockType "+blockType+" against "+logOres[i]);
			if( blockType == logOres[i] ) {
				logQueue.push(new LogEvent(event.getPlayer().getName(), event.getBlock().getState()));
				break;
			}
		}
	}
	
	public void reloadConfig() {
		this.logOres = plugin.getLogOresConfig().getLogIds();
	}
}
