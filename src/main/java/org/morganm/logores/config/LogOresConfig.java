/**
 * 
 */
package org.morganm.logores.config;

import org.morganm.logores.LogOresPlugin;

/** Config object with some logic specific to LogOresPlugin which does some processing
 * of the config file to generate specific data we need to operate the plugin.
 * 
 * @author morganm
 *
 */
public class LogOresConfig {
	private LogOresPlugin plugin;
	
	public LogOresConfig(LogOresPlugin plugin) {
		this.plugin = plugin;
	}
	
	/** Should be called after the config file is loaded, it will do the processing of
	 * config parameters into the data we need.
	 */
	public void processConfig() {
		Config config = plugin.getConfig();
		
//		config.getString(path, def)
	}
}
