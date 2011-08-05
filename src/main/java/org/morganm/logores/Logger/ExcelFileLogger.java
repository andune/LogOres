/**
 * 
 */
package org.morganm.logores.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import org.morganm.logores.LogOresPlugin;
import org.morganm.logores.ProcessedEvent;

/**
 * @author morganm
 * 
 */
public class ExcelFileLogger extends FileLogger implements EventLogger {
	private static final String formatString =
		"%s|%s|%s|" +			// date, material, player
		"%s|%d|%d|%d|%d|" + 	// world, x, y, z, lightLevel
		"%d|%1.0f|%d|%1.1f|" +		// time, distance, block, ratio
		"%s|%d|%s\n";				// flagged string, flag count, additional info
		
	public ExcelFileLogger(LogOresPlugin plugin) {
		super(plugin);
	}
	
	@Override
	public void logEvent(ProcessedEvent pe) throws Exception {
		FileWriter fileWriter = getFileWriter(pe.eventWorld);
		String logMessage = getExcelLogString(pe);

		try {
			fileWriter.write(logMessage);
		} catch (IOException e) {
			 // ugh terrible code based on implementation message in exception, I know.
			if (e.getMessage().contains("Stream closed")) {
				fileWriter = openLogFile(pe.eventWorld);
				fileWriter.write(logMessage);
			}
		}
	}
	
	
	protected void createNewFile(File file) throws IOException {
		super.createNewFile(file);
		
		FileWriter writer = new FileWriter(file, true);
		writer.append("# File format:");
		writer.append("# Date|player|ore|world|x|y|z|lightLevel|"+
				"time|distance|blocks|ratio|"+
				"flagged|flagCount|extra info"
				);
	}
	
	protected String getLogFileName(String world) {
		String logFile = super.getLogFileName(world);
		
		int txtIndex;
		if( (txtIndex = logFile.indexOf(".txt")) != -1 )
			logFile = logFile.substring(0, txtIndex) + ".data";
		
		return logFile;
	}
	
	private String getExcelLogString(ProcessedEvent pe) {
		StringBuffer sb = new StringBuffer();
		Formatter formatter = new Formatter(sb, Locale.US);

		StringBuffer flagString = new StringBuffer();
		if( pe.isFlagged() ) {
			flagString.append("flagged;");
			
			if( pe.isRatioFlagged() )
				flagString.append(" ratio");
			if( pe.isLightFlagged() )
				flagString.append(" nolight");
		}

		formatter.format(formatString,
				new Date().toString(),
				pe.logEvent.playerName,
				pe.logEvent.bs.getData().getItemType().toString(), // MATERIAL name
				
				pe.eventWorld, pe.logEvent.bs.getX(), pe.logEvent.bs.getY(), pe.logEvent.bs.getZ(),
				pe.lightLevel,
				
				pe.time, pe.distance, pe.logEvent.nonOreCounter, pe.ratio,
				
				flagString.toString(), pe.flagCount,
				(pe.isInCave ? "[cave?]" : "") +
				(pe.isInVariance ? "[in variance]" : "")
				);

		return sb.toString();
	}

}
