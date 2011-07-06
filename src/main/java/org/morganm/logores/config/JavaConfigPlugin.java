/**
 * 
 */
package org.morganm.logores.config;

import java.io.File;
import java.util.logging.Logger;

/** Interface that the JavaPlugin object implements that these config packages then
 * utilize.  This makes the config objects portable between projects without being
 * tied to a specific plugin class.
 * 
 * @author morganm
 *
 */
public interface JavaConfigPlugin {
	/** Return File location of the JAR file for this plugin.
	 * 
	 * @return
	 */
	public File getJarFile();
	
	/** Return File location of the data folder for this plugin (usually "plugins/PluginName").
	 * 
	 * @return
	 */
	public File getDataFolder();
	
	/** Get string to be used as log prefix for any log messages printed out.
	 * 
	 * @return
	 */
	public String getLogPrefix();
	
	/** Get Logger object to be used to send any log messages to.
	 * 
	 * @return
	 */
	public Logger getLogger();
}
