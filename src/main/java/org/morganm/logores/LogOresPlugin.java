/**
 * 
 */
package org.morganm.logores;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.java.JavaPlugin;
import org.morganm.logores.config.Config;
import org.morganm.logores.config.ConfigException;
import org.morganm.logores.config.ConfigFactory;
import org.morganm.logores.config.JavaConfigPlugin;
import org.morganm.logores.config.LogOresConfig;

/**
 * @author morganm
 *
 */
public class LogOresPlugin extends JavaPlugin implements JavaConfigPlugin {
	public static final Logger log = Logger.getLogger(LogOresPlugin.class.toString());
	
	private String logPrefix;
	private String pluginName;
	private Config config;
	private LogOresConfig logOresConfig;
	private LogQueue logQueue;
	private LogOresBlockListener blockListener;
	private LogOreLogger oreLogger;
	
	public void loadConfig() throws ConfigException, IOException {
		config = ConfigFactory.getInstance(ConfigFactory.Type.YAML, this, "plugins/"+pluginName+"/config.yml");
		config.load();
		
		logOresConfig = new LogOresConfig(this);
		logOresConfig.processConfig();
		
		blockListener.reloadConfig();
	}
	
	public void shutdownPlugin() {
		getServer().getPluginManager().disablePlugin(this);		
	}
	
	@Override
	public void onEnable() {
		boolean loadError = false;
		
    	pluginName = getDescription().getName();
    	logPrefix = "[" + pluginName + "]";
    	
		logQueue = new LogQueue();
    	blockListener = new LogOresBlockListener(this);
    	
    	try {
    		loadConfig();
    	}
    	catch(Exception e) {
    		loadError = true;
    		log.severe("Error loading plugin: "+pluginName);
    		e.printStackTrace();
    	}
    	
    	if( loadError ) {
    		log.severe("Error detected when loading plugin "+ pluginName +", plugin shutting down.");
    		shutdownPlugin();
    		return;
    	}
    	
        getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Monitor, this);
        
        oreLogger = new LogOreLogger(this);
        getServer().getScheduler().scheduleAsyncDelayedTask(this, oreLogger);
		
        log.info( logPrefix + " version [" + getDescription().getVersion() + "] loaded" );
	}

	@Override
	public void onDisable() {
		try {
			oreLogger.close();
		} catch(IOException e) { e.printStackTrace(); }
		
        log.info( logPrefix + " version [" + getDescription().getVersion() + "] unloaded" );
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		if( command.equals("lo") ) {
			if( args.length < 1 ) {
				sendMessage(sender, "Usage:\nlo reload - reload config file\n");
			}
			else if( args[0].equals("reload") || args[0].equals("rc") ) {	// reload config
				try {
					loadConfig();
					sendMessage(sender, "Config file reloaded.");
				} catch(Exception e) {
					sendMessage(sender, "Error loading config file, please check system log");
					e.printStackTrace();
				}
			}
			
			return true;
		}
		
		return false;
	}
	
	/** Write a mod message to the target, using our preferred color.
	 * 
	 * @param target
	 */
	public void sendMessage(CommandSender target, String message) {
		target.sendMessage("&a" + message);
	}
	
	public Config getConfig() { return config; }
	public LogOresConfig getLogOresConfig() { return logOresConfig; }
	public LogQueue getLogQueue() { return logQueue; }
	
	public File getJarFile() {
		return super.getFile();
	}
	
	@Override
	public String getLogPrefix() {
		return logPrefix;
	}

	@Override
	public Logger getLogger() {
		return log;
	}
}
