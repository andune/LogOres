package org.morganm.logores.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import org.morganm.logores.LogOresPlugin;
import org.morganm.logores.ProcessedEvent;

/** Class originally written by Sancta. Refactored by morganm to remove
 * dependencies on FileLogger and decouple the two implementations.
 * 
 * @author sancta, morganm
 *
 */
public class DatabaseLogger implements EventLogger {
	private final Logger log;
	private final String logPrefix;
	
	private LogOresPlugin plugin;
	private Statement stmt;
	private Connection conn;
	private boolean status = true;
	
	public DatabaseLogger(LogOresPlugin plugin) {
		this.plugin = plugin;
		this.log = plugin.getLogger();
		this.logPrefix = plugin.getLogPrefix();
	}
	
	private void connect() {
        String host = plugin.getConfig().getString("mysql.host");
		String database = plugin.getConfig().getString("mysql.database");
		String user = plugin.getConfig().getString("mysql.user");
		String password = plugin.getConfig().getString("mysql.password");
		

        try {
			conn = DriverManager.getConnection("jdbc:mysql://"+host+"/"+database+"?user="+user+"&password="+password);
		} catch (SQLException ex) {
        	status = false;
        	log.info(logPrefix + " Cannot connect to mysql database");
        	ex.printStackTrace();
        }

	}

	public boolean getStatus() {
		return status;
	}
		
	@Override
	public EventLogger init() {
		try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            connect();
            if(conn.isValid(20)){
			try {
				System.out.println("[Logores] MySQL-Connection established");
				stmt = conn.createStatement();
				stmt.execute("CREATE TABLE IF NOT EXISTS `logores_log` (" +
						"`id` INT unsigned NOT NULL AUTO_INCREMENT" +
						",`date` DATETIME NOT NULL" +
						",`username` varchar(50) NOT NULL" +
						",`ore` varchar(50) NOT NULL" +
						",`x` INT NOT NULL" +
						",`y` INT NOT NULL" +
						",`z` INT NOT NULL" +
						",`light` TINYINT NOT NULL" +
						",`world` varchar(50) NOT NULL" +
						",`t` MEDIUMINT NOT NULL" +
						",`d` MEDIUMINT unsigned NOT NULL" +
						",`b` MEDIUMINT unsigned NOT NULL" +
						",`ratio` SMALLINT NOT NULL" +
						",`flagged` varchar(255) NOT NULL" +
						",PRIMARY KEY (`id`)) ENGINE=MyISAM DEFAULT CHARSET=utf8;"
						);		
				} catch (SQLException e) {
					log.severe(logPrefix + " 'CREATE TABLE `logores_log`' failed ");
					e.printStackTrace();
					status  = false;
				} finally {
					    if (stmt != null) {
					        try {
					            stmt.close();
					        } catch (SQLException sqlEx) { 
					         
					        }
				        stmt = null;
					    }
				}
            }
        } catch (Exception ex) {
        	status = false;
        	log.info(logPrefix + " MySQL Error");
        	ex.printStackTrace();
        }
		
		return this;
	}

	@Override
	public void flush() {
		// we don't do anything with this for MySQL since each row is fully committed
		// at the time of the logEvent() call.
	}

	@Override
	public void close() throws Exception {
		conn.close();
	}

	@Override
	public void logEvent(ProcessedEvent pe) throws Exception {
		// morganm: DRY violation, this code exists in FileLogger as well, I should probably
		// refactor it into a base class or utility class. For now, laziness wins.
		String flagged = "";
		if( pe.isFlagged() ) {
			flagged = "[flagged x"+pe.flagCount+";";
			if( (pe.flagReasons & ProcessedEvent.RATIO_FLAG) != 0 )
				flagged = flagged+" ratio";
			//if( (pe.flagReasons & ProcessedEvent.NO_LIGHT_FLAG) != 0 )
			if(pe.lightLevel == 0)
				flagged = flagged+" nolight";
			if( (pe.flagReasons & ProcessedEvent.PARANOID_DIAMOND_FLAG) != 0 )
				flagged = flagged+" paranoidDiamonds";
			flagged = flagged+"]";
		}
		
		if( pe.isInCave )
			flagged = flagged+" [cave]";
		
		try {
			// Reconnect 
			if(!conn.isValid(20)) {
				connect();	
			}
			// TODO: this probably should be updated to use a preparedStatement as the performance will
			// be much better.
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO logores_log (date, username, ore, x, y, z, light, world, t, d, b, ratio, flagged) "
					+ "VALUES ("
						+ "NOW()"
						+ ",'"+pe.logEvent.playerName
						+ "','"+pe.logEvent.bs.getData().getItemType().toString()
						+ "',"+pe.logEvent.bs.getX()+","+pe.logEvent.bs.getY()+","+pe.logEvent.bs.getZ()
						+ ","+pe.lightLevel+",'"+pe.eventWorld+"'"
						+ ",'"+pe.time+"'"
						+ ","+(int)pe.distance+","+pe.logEvent.nonOreCounter+",'"+(int)pe.ratio+"'"
						+ ",'"+flagged+"'"
					+ ")"
				);
			stmt.close();
			} catch (SQLException e) {
			// TODO Auto-generated catch block
				log.info(logPrefix + " MySQL: Insert Failed");
				e.printStackTrace();
			} finally {
				    if (stmt != null) {
				        try {
				            stmt.close();
				        } catch (SQLException sqlEx) { 
				        // ignore 
				        }
			        stmt = null;
				    }
			}
	}
}