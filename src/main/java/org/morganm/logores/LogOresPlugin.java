/**
 * 
 */
package org.morganm.logores;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.morganm.logores.config.Config;
import org.morganm.logores.config.ConfigException;
import org.morganm.logores.config.ConfigFactory;
import org.morganm.logores.config.JavaConfigPlugin;
import org.morganm.logores.config.LogOresConfig;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;

/**
 * @author morganm
 *
 */
public class LogOresPlugin extends JavaPlugin implements JavaConfigPlugin {
	public static final Logger log = Logger.getLogger(LogOresPlugin.class.toString());
	
	// yellow
    private static final String MOD_COLOR = "\u00A7e";
    
	/* Map to keep track of player non-ore block hits in between block hits.
	 * 
	 */
	public Map<String, Counter> playerNonOreCount;
	
	private String logPrefix;
	private String pluginName;
	private Config config;
	private LogOresConfig logOresConfig;
	private LogQueue logQueue;
	private LogOresBlockListener blockListener;
	private LogEventProcessor oreProcessor;
    private PermissionHandler permissionHandler;
	
	public void loadConfig() throws ConfigException, IOException {
		boolean firstTime = true;
		if( config != null )
			firstTime = false;
		
		config = ConfigFactory.getInstance(ConfigFactory.Type.YAML, this, "plugins/"+pluginName+"/config.yml");
		config.load();
		
		logOresConfig = new LogOresConfig(this);
		logOresConfig.processConfig();
		
		if( blockListener != null )
			blockListener.reloadConfig();
		if( oreProcessor != null )
			oreProcessor.reloadConfig();
		
		if( !firstTime )
			log.info(logPrefix + " config live reload complete");
	}
	
	public void shutdownPlugin() {
		getServer().getPluginManager().disablePlugin(this);		
	}
	
	@Override
	public void onEnable() {
		boolean loadError = false;
		
		playerNonOreCount = new HashMap<String, Counter>(); 
		
    	pluginName = getDescription().getName();
    	logPrefix = "[" + pluginName + "]";
    	
		logQueue = new LogQueue();
    	blockListener = new LogOresBlockListener(this);
    	
		initPermissions();
		
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
        
        oreProcessor = new LogEventProcessor(this);
		oreProcessor.reloadConfig();
		
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, oreProcessor, 200, 100);
		
        log.info( logPrefix + " version [" + getDescription().getVersion() + "] loaded" );
	}

	@Override
	public void onDisable() {
        log.info( logPrefix + " version [" + getDescription().getVersion() + "] unloading" );
		getServer().getScheduler().cancelTasks(this);
		oreProcessor.close();
        log.info( logPrefix + " version [" + getDescription().getVersion() + "] unloaded" );
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
	{
		String commandName = command.getName().toLowerCase();
		
		if( commandName.equals("lo") ) {
			if( !hasPermission(sender, "logores.admin") )
				return false;
			
			if( args.length < 1 ) {
				sendMessage(sender, "Usage:");
				sendMessage(sender, "  /lo reload - reload config file\n");
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
	
    /** Initialize permission system.
     * 
     */
    private void initPermissions() {
        Plugin permissionsPlugin = getServer().getPluginManager().getPlugin("Permissions");
        if( permissionsPlugin != null )
        	permissionHandler = ((Permissions) permissionsPlugin).getHandler();
        else
	    	log.warning(logPrefix+" Permissions system not enabled, using isOP instead.");
    }
    
    public boolean hasPermission(CommandSender sender, String permissionNode) {
		if( sender instanceof ConsoleCommandSender )
			return true;
		
		if( sender instanceof Player ) {
	    	if( permissionHandler != null ) 
	    		return permissionHandler.has((Player) sender, permissionNode);
	    	else
	    		return sender.isOp();
		}
		
		return false;
    }
	
	/** Write a mod message to the target, using our preferred color.
	 * 
	 * @param target
	 */
	public void sendMessage(CommandSender target, String message) {
		target.sendMessage(MOD_COLOR + message);
	}
	
	public Config getConfig() { return config; }
	public LogOresConfig getLogOresConfig() { return logOresConfig; }
	public LogQueue getLogQueue() { return logQueue; }
	public LogEventProcessor getEventProcessor() { return oreProcessor; }
	
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
