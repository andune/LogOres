/**
 * 
 */
package org.morganm.logores.config;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.morganm.logores.LogOresPlugin;

/** Config object with some logic specific to LogOresPlugin which does some processing
 * of the config file to generate specific data we need to operate the plugin.
 * 
 * @author morganm
 *
 */
public class LogOresConfig {
	private final Logger log;
	private final String logPrefix;
	private final LogOresPlugin plugin;
	
	private int logIds[];
	
	public LogOresConfig(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
	}
	
	/** Should be called after the config file is loaded, it will do the processing of
	 * config parameters into the data we need.
	 */
	public void processConfig() throws ConfigException {
		Config config = plugin.getConfig();
		
		List<Integer> ids = new ArrayList<Integer>();
		List<String> loggedOres = config.getStringList("loggedOres", null);
		for(String s : loggedOres) {
			try {
				Integer i = Integer.parseInt(s);
				ids.add(i);
			}
			catch(NumberFormatException e) {
				log.warning(logPrefix + " Invalid number in loggedOres list: "+s);
			}
		}
		
		if( ids.isEmpty() )
			throw new ConfigException("No ores defined, logIds is empty");
		
		logIds = new int[ids.size()];
		for(int i=0; i < ids.size(); i++) {
			logIds[i] = ids.get(i).intValue();
		}
	}
	
	public int[] getLogIds() { return logIds; }
}
