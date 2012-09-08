/**
 * 
 */
package org.morganm.logores;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;
import org.morganm.logores.Logger.DatabaseLogger;

/** Config object with some logic specific to LogOresPlugin which does some processing
 * of the config file to generate specific data we need to operate the plugin.
 * 
 * @author morganm
 *
 */
public class LogOresConfig {
	private final Logger log;
	private final LogOres plugin;
	
	private int logIds[];
	
	public LogOresConfig(LogOres plugin) {
		this.plugin = plugin;
		this.log = plugin.getLogger();
	}
	
	/** Should be called after the config file is loaded, it will do the processing of
	 * config parameters into the data we need.
	 */
	public void processConfig() {
		FileConfiguration config = plugin.getConfig();
		
		List<Integer> ids = new ArrayList<Integer>();
		List<String> loggedOres = config.getStringList("loggedOres");
		for(String s : loggedOres) {
			try {
				Integer i = Integer.parseInt(s);
				ids.add(i);
			}
			catch(NumberFormatException e) {
				log.warning(" Invalid number in loggedOres list: "+s);
			}
		}
		
		logIds = new int[ids.size()];
		if( ids.isEmpty() )
			log.severe(" No ores defined, logIds is empty");
		
		for(int i=0; i < ids.size(); i++) {
			logIds[i] = ids.get(i).intValue();
		}
	}
	
	public boolean isDatabaseEnabled() {
	    boolean ret = false;
	    
        List<String> logTypes = plugin.getConfig().getStringList("logTypes");
        for(String logType : logTypes) {
            if( logType.equals("mysql") ) {
                ret = true;
                break;
            }
        }
        
        return ret;
	}
	
	public int[] getLogIds() { return logIds; }
}
