/**
 * 
 */
package org.morganm.logores;

/**
 * @author morganm
 *
 */
public class WatchDog implements Runnable {
	private LogOresPlugin plugin;
	
	public WatchDog(LogOresPlugin plugin) {
		this.plugin = plugin;
	}
	
	/** Called at regular intervals by Bukkit scheduler.
	 * 
	 */
	public void run() {
		if( !plugin.isEnabled() )
			return;
		
		LogOreLogger oreLogger = plugin.getOreLogger();
		if( oreLogger.isZombie() ) {
			oreLogger.reset();
			plugin.getServer().getScheduler().scheduleAsyncDelayedTask(plugin, oreLogger);
			plugin.getLogger().warning(plugin.getLogPrefix() + " Watchdog restarted dead OreLogger");
		}
	}
}
