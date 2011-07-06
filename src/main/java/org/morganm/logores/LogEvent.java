/**
 * 
 */
package org.morganm.logores;

import org.bukkit.block.BlockState;

/** Just a container for log event data that we capture.  Note that this class exists since
 * we don't use the actual BlockBreakEvent object, because BlockBreakEvent doesn't guarantee
 * the integrity of it's data. ie. the Block might change by the time the Queue gets around
 * to it, or the player might have logged out, etc.  So we capture all that data at the
 * time of the event and store it in one of these objects for later processing.
 * 
 * @author morganm
 *
 */
public class LogEvent {
	public String playerName;
	public BlockState bs;
	
	public LogEvent(String playerName, BlockState bs) {
		this.playerName = playerName;
		this.bs = bs;
	}
}
