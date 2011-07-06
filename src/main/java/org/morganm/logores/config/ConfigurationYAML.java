/**
 * 
 */
package org.morganm.logores.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import org.bukkit.util.config.Configuration;


/**
 * @author morganm
 *
 */
public class ConfigurationYAML extends Configuration implements Config {
	private final Logger log;
	private final String logPrefix; 
	
	private File file;
	private JavaConfigPlugin plugin;
	
	public ConfigurationYAML(File file, JavaConfigPlugin plugin) {
		super(file);
		this.file = file;
		this.plugin = plugin;
		
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
	}

	@Override
    public void load() {
		// if no config exists, copy the default one out of the JAR file
		if( !file.exists() )
			copyConfigFromJar("config.yml");
		
		super.load();
    }

	/** Right now we don't allow updates in-game, so we don't do anything, because if we
	 * let it save, all the comments are lost.  In the future, I may allow in-game updates
	 * to the config file and this will just call super.save();
	 */
	@Override
	public boolean save() {
		return true;
	}
	
	/** Code adapted from Puckerpluck's MultiInv plugin.
	 * 
	 * @param string
	 * @return
	 */
    private void copyConfigFromJar(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        
        if (!file.canRead()) {
            try {
            	JarFile jar = new JarFile(plugin.getJarFile());
            	
                file.getParentFile().mkdirs();
                JarEntry entry = jar.getJarEntry(fileName);
                InputStream is = jar.getInputStream(entry);
                FileOutputStream os = new FileOutputStream(file);
                byte[] buf = new byte[(int) entry.getSize()];
                is.read(buf, 0, (int) entry.getSize());
                os.write(buf);
                os.close();
            } catch (Exception e) {
                log.warning(logPrefix + " Could not copy config file "+fileName+" to default location");
            }
        }
    }
}
