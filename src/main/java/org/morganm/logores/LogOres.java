/**
 * 
 */
package org.morganm.logores;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.morganm.logores.util.JarUtils;
import org.morganm.logores.util.PermissionSystem;

/**
 * @author morganm
 *
 */
public class LogOres extends JavaPlugin {
	public static Logger log;
	
	// yellow
    private static final String MOD_COLOR = "\u00A7e";
    
	/* Map to keep track of player non-ore block hits in between block hits.
	 */
	public Map<String, Counter> playerNonOreCount;
	
	/* Map to keep track of recent player block breaks in an efficient manner, for
	 * aid in detecting caves.
	 */
	public Map<String, RecentBlocks> playerRecentBlocks; 
	
	private String logPrefix;
	private String pluginName;
//	private FileConfiguration config;
	private LogOresConfig logOresConfig;
	private LogQueue logQueue;
	private LogOresBlockListener blockListener;
	private LogEventProcessor oreProcessor;
	private PermissionSystem perm;
	private JarUtils jarUtil;
	private int buildNumber = -1;
    private boolean configLoaded = false;
	
    public void loadConfig() {
    	// copy default config.yml into place if it's not there
		File file = new File(getDataFolder(), "config.yml");
		if( !file.exists() ) {
			jarUtil.copyConfigFromJar("config.yml", file);
		}
		
    	if( !configLoaded ) {
	    	super.getConfig();
	    	configLoaded = true;
    	}
    	else
    		super.reloadConfig();
    	
		logOresConfig = new LogOresConfig(this);
		logOresConfig.processConfig();

		if( blockListener != null )
			blockListener.reloadConfig();
		if( oreProcessor != null )
			oreProcessor.reloadConfig();
    }
    
    /*
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
	*/
	
	public void shutdownPlugin() {
		getServer().getPluginManager().disablePlugin(this);
	}
	
	@Override
	public void onEnable() {
		log = this.getLogger();
		boolean loadError = false;
		jarUtil = new JarUtils(this, getFile(), log, logPrefix);
		buildNumber = jarUtil.getBuildNumber();
		
		playerNonOreCount = new HashMap<String, Counter>(); 
		playerRecentBlocks = new HashMap<String, RecentBlocks>();
		
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
    	
    	getServer().getPluginManager().registerEvents(blockListener, this);
//        getServer().getPluginManager().registerEvent(Event.Type.BLOCK_BREAK, blockListener, Priority.Monitor, this);
        
        oreProcessor = new LogEventProcessor(this);
		oreProcessor.reloadConfig();
		
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, oreProcessor, 200, 100);
		
		log.info(logPrefix + "version "+getDescription().getVersion()+", build "+buildNumber+" is enabled");
	}

	@Override
	public void onDisable() {
		getServer().getScheduler().cancelTasks(this);
		oreProcessor.close();
		log.info(logPrefix + "version "+getDescription().getVersion()+", build "+buildNumber+" is disabled");
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
		perm = new PermissionSystem(this, log, logPrefix);
		perm.setupPermissions();
    }
    
    public boolean hasPermission(CommandSender sender, String permissionNode) {
    	return perm.has(sender, permissionNode);
    }
	
	/** Write a mod message to the target, using our preferred color.
	 * 
	 * @param target
	 */
	public void sendMessage(CommandSender target, String message) {
		target.sendMessage(MOD_COLOR + message);
	}
	
	public void broadcast(String message, String permission) {
		Bukkit.broadcast(MOD_COLOR + message, permission);
	}
	
//	public Config getLOConfig() { return config; }
	public LogOresConfig getLogOresConfig() { return logOresConfig; }
	public LogQueue getLogQueue() { return logQueue; }
	public LogEventProcessor getEventProcessor() { return oreProcessor; }
	
	public File getJarFile() {
		return super.getFile();
	}
	
	public String getLogPrefix() {
		return logPrefix;
	}

}
