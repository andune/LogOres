/**
 * 
 */
package org.morganm.logores.config;

/**
 * @author morganm
 *
 */
public interface ConfigOptions {
	public static final String STORAGE_TYPE = "core.storage";
	
	public static final String DEFAULT_WORLD = "spawn.defaultWorld";
	public static final String COMMAND_TOGGLE_BASE = "disabledCommands.";
	
	public static final String ENABLE_HOME_BEDS = "home.bedsethome";
	public static final String ENABLE_GROUP_SPAWN = "spawn.groups";

	public static final String VALUE_JOIN_DEFAULT = "default";
	public static final String VALUE_JOIN_HOME = "home";
	public static final String VALUE_JOIN_GLOBAL = "global";
	public static final String VALUE_JOIN_GROUP = "group";
	public static final String VALUE_JOIN_WORLD = "world";
	
	public static final String SETTING_JOIN_BEHAVIOR = "spawn.onjoin";
	public static final String SETTING_DEATH_BEHAVIOR = "spawn.ondeath";
	public static final String SETTING_SPAWN_BEHAVIOR = "spawn.oncommand";

	public static final String VALUE_DEFAULT = "default";
	public static final String VALUE_HOME = "home";
	public static final String VALUE_MULTIHOME = "multihome";
	public static final String VALUE_GROUP = "group";
	public static final String VALUE_WORLD = "world";
	public static final String VALUE_GLOBAL = "global";
	
	public static final String SETTING_WORLD_OVERRIDE = "spawn.override_world";
	
	public static final String COOLDOWN_BASE = "cooldown.";
}
