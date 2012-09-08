/**
 * 
 */
package org.morganm.logores.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.logging.Logger;

import org.morganm.logores.LogOres;
import org.morganm.logores.ProcessedEvent;

/**
 * @author morganm
 * 
 */
public class FileLogger implements EventLogger {
	private final Logger log;
	private final String logPrefix;

	private LogOres plugin;
	
	protected boolean logFilePerWorld = false;
	private String logFile;
	
	private FileWriter writer;
	private HashMap<String, FileWriter> writerPerWorld;
	
	public FileLogger(LogOres plugin) {
		this.plugin = plugin;
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
	}
	
	@Override
	public EventLogger init() {
		logFile = plugin.getConfig().getString("logFile", "plugins/LogOres/logOres");
		
		logFilePerWorld = plugin.getConfig().getBoolean("logFilePerWorld", false);
		if( logFilePerWorld )
			writerPerWorld = new HashMap<String, FileWriter>();
		
		return this;
	}
	
	@Override
	public void logEvent(ProcessedEvent pe) throws Exception {
		FileWriter fileWriter = getFileWriter(pe.eventWorld);

		String logMessage = FileLogger.getLogString(pe, logFilePerWorld);

		String dateStamp = "[" + new Date().toString() + "] ";
		try {
			fileWriter.write(dateStamp + logMessage);
		} catch (IOException e) {
			 // ugh terrible code based on implementation message in exception, I know.
			if (e.getMessage().contains("Stream closed")) {
				fileWriter = openLogFile(pe.eventWorld);
				fileWriter.write(dateStamp + logMessage);
			}
		}
	}

	/** Return the FileWriter object to be used, given the world the event is in.
	 * 
	 * @param world
	 * @return
	 */
	protected FileWriter getFileWriter(String world) {
		FileWriter fileWriter = null;
		
		if (logFilePerWorld)
			fileWriter = writerPerWorld.get(world);
		else
			fileWriter = writer;

		if (fileWriter == null)
			fileWriter = openLogFile(world);
		
		return fileWriter;
	}
	
	/** Return the logString for a given ProcessedEvent, minus the [datestamp] prefix.  This is used both internal
	 * to this class for file logging as well as external for sending in-game notifications.
	 * 
	 * @param pe
	 * @param includeWorldTag
	 * @return
	 * @throws Exception
	 */
	static public String getLogString(ProcessedEvent pe, boolean includeWorldTag) {
		StringBuffer sb = new StringBuffer();
		Formatter formatter = new Formatter(sb, Locale.US);

		String eventWorld = pe.eventWorld;

		formatter.format(
				"%-11s broken by %-12s at (x=%6d, y=%4d, z=%6d, l=%2d",
				pe.logEvent.bs.getData().getItemType().toString(), // MATERIAL
																	// name
				pe.logEvent.playerName, pe.logEvent.bs.getX(),
				pe.logEvent.bs.getY(), pe.logEvent.bs.getZ(),
				pe.lightLevel
				);

		if (!includeWorldTag) {
			sb.append(", world=");
			sb.append(eventWorld);
		}

		sb.append(")");

		if( pe.isNewOreCluster ) {
			formatter.format(
					" [t=%4dsec / (d=%3.0f / b=%4d) = r=%3.1f]", pe.time,
					pe.distance, pe.logEvent.nonOreCounter, pe.ratio);

			if( pe.isFlagged() ) {
				sb.append(" [flagged x");
				sb.append(pe.flagCount);
				sb.append(";");
				if( (pe.flagReasons & ProcessedEvent.RATIO_FLAG) != 0 )
					sb.append(" ratio");
				if( (pe.flagReasons & ProcessedEvent.NO_LIGHT_FLAG) != 0 )
					sb.append(" nolight");
				if( (pe.flagReasons & ProcessedEvent.PARANOID_DIAMOND_FLAG) != 0 )
					sb.append(" paranoidDiamonds");
				sb.append("]");
			}
			else if( pe.isInVariance )
				sb.append(" [in variance]");

			if( pe.isInCave )
				sb.append(" [cave]");
		}

		sb.append("\n");
		
		return sb.toString();
	}
	/** Called when a logfile doesn't exist and a new one needs to be created. Exists so it
	 * can be overridden by a subclass.
	 * 
	 * @param file
	 */
	protected void createNewFile(File file) throws IOException {
		file.createNewFile();
	}
	
	protected String getLogFileName(String world) {
		String fileName = null;
		
		int txtIndex;
		if( (txtIndex = logFile.indexOf(".txt")) != -1 ) {
			logFile = logFile.substring(0, txtIndex);
			log.info(logPrefix + " Changed base logFile path to (dropped .txt): "+logFile);
		}
		
		// if we're only logging one file for all worlds, then don't add world as part of filename
		if( !logFilePerWorld )
			fileName = logFile + ".txt";
		else
			fileName = logFile + "." + world + ".txt";
		
		return fileName;
	}
	
	/**
	 * 
	 * @return writer that was opened on success, null on failure
	 */
	protected FileWriter openLogFile(String world) {
		FileWriter fileWriter = null;
		String fileName = getLogFileName(world);
		
		try {
			File file = new File(fileName);
			if( !file.exists() )
				createNewFile(file);
			fileWriter = new FileWriter(file, true);		// append
		}
		catch(Exception e) {
			e.printStackTrace();
		}

		// indicates error opening the writer
		if( fileWriter == null )
			return null;
		
		if( logFilePerWorld )
			writerPerWorld.put(world, fileWriter);
		else
			writer = fileWriter;
		
		return fileWriter;
	}
	
	@Override
	public void flush() {
		try {
			if( logFilePerWorld ) {
				for(FileWriter fileWriter : writerPerWorld.values()) {
					if( fileWriter != null )
						fileWriter.flush();
				}
			}
			else {
				if( writer != null )
					writer.flush();
			}
		} catch(IOException e) { e.printStackTrace(); }		
	}
	
	@Override
	public void close() throws IOException {
		try {
			if( logFilePerWorld ) {
				for(FileWriter fileWriter : writerPerWorld.values()) {
					if( fileWriter != null )
						fileWriter.close();
				}
			}
			else {
				if( writer != null )
					writer.close();
			}
		} catch(IOException e) { e.printStackTrace(); }		
	}
}
