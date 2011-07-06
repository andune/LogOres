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
	
	public LogOresBlockListener(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.logQueue = plugin.getLogQueue();
	}
	
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled())
			return;
		
		// if this is an ore we should log
		if( true ) {
			logQueue.push(event.getBlock().getState());
		}
	}
}
