/**
 * 
 */
package org.morganm.logores;

/** This class represents a LogEvent that has been processed through our configs
 * and is basically in a ready-to-write state.  This is then used by different
 * Loggers to write them in the appropriate format: to a logfile, to a database,
 * to an excel-friendly file, etc.
 * 
 * I'm generally not a fan of public members and prefer to use accessor methods,
 * however since the intent is for this to class to be used simply as a private
 * data container, it's more efficient for us to expose the members directly
 * rather than forcing every Logger implementation to use a bunch of (slower)
 * method calls to get at the raw data.
 * 
 * @author morganm
 *
 */
public class ProcessedEvent {
	// the original logEvent which contains important info to be logged
	public LogEvent logEvent;
	
	// if lightLevel was logged (0-15), it's here.  Note that -1 will indicate
	// that no lightLevel was logged, in the event lightLevel logging is turned off.
	public int lightLevel;
	
	// whether or not this event is considered part of a new ore cluster, compared
	// to the previous ore
	public boolean isNewOreCluster;
	
	// if this is a new ore cluster, a ratio is calculated and stored here
	public double ratio;
	
	// if this is a new ore cluster, the distance from the previous ore cluster
	// is stored here
	public double distance;
	
	// whether or not this event has tripped the defined flag ratio
	public boolean isFlagged;
	
	// whether or not this event has been determined to be in a cave
	public boolean isInCave;
	
	// if variance limits are defined and this event falls within them, this will
	// be set to true.
	public boolean isInVariance;
	
	public ProcessedEvent(LogEvent logEvent) {
		this.logEvent = logEvent;
	}
	
	
}
