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
	public static final int RATIO_FLAG = 0x01;
	public static final int NO_LIGHT_FLAG = 0x02;
	public static final int PARANOID_DIAMOND_FLAG = 0x04;
	
	// the original logEvent which contains important info to be logged
	public LogEvent logEvent;
	
	// the world the event occured on
	public String eventWorld;
	
	// if lightLevel was logged (0-15), it's here.  Note that -1 will indicate
	// that no lightLevel was logged, in the event lightLevel logging is turned off.
	public int lightLevel = -1;
	
	// the total number of flagged events we've accumulated this session (session referring
	// to the last time since the plugin was reloaded, usually the last server reboot)
	public int flagCount;
	
	// the amount of time (in seconds) that have elapsed since the previous ore break
	public long time;
	
	// whether or not this event is considered part of a new ore cluster, compared
	// to the previous ore
	public boolean isNewOreCluster = false;
	
	// if this is a new ore cluster, a ratio is calculated and stored here
	public double ratio;
	
	// if this is a new ore cluster, the distance from the previous ore cluster
	// is stored here
	public double distance;

	// bitmask of flag reasons for this event, if any
	public int flagReasons = 0;
	
	// whether or not this event has been determined to be in a cave
	public boolean isInCave = false;
	
	// if variance limits are defined and this event falls within them, this will
	// be set to true.
	public boolean isInVariance = false;
	
	public ProcessedEvent(LogEvent logEvent) {
		this.logEvent = logEvent;
	}
	
	public boolean isFlagged() {
		return flagReasons != 0;
	}
	
	public boolean isRatioFlagged() {
		return (flagReasons & RATIO_FLAG) != 0;
	}
	
	public boolean isLightFlagged() {
		return (flagReasons & NO_LIGHT_FLAG) != 0;
	}
}
