/**
 * 
 */
package org.morganm.logores.config;

import java.io.File;
import java.io.IOException;


/**
 * @author morganm
 *
 */
public class ConfigFactory {
	public static enum Type
	{
		YAML
	}

	/** Get a Config instance of the specified type.
	 * 
	 * @param storageType type Type of config object to load
	 * @param plugin the calling plugin, passed to config objects, might be used to get database handle
	 * @param arg1 additional argument to be passed, perhaps filename or table name
	 * @return
	 * @throws StorageException
	 * @throws IOException
	 */
	public static Config getInstance(Type storageType, JavaConfigPlugin plugin, Object arg1)
	throws ConfigException, IOException
{
	if ( storageType == Type.YAML ) {
		File file = null; 
		if( arg1 instanceof File )
			file = (File) arg1;
		else if( arg1 instanceof String )
			file = new File((String) arg1);
		else
			throw new ConfigException("Unable to create Config interface: invalid YAML config file argument");
		
		return new ConfigurationYAML(file, plugin);
//		return new ConfigYAML(file);
	}
	else {
		throw new ConfigException("Unable to create Config interface.");
	}
}
}
