package org.morganm.logores.Logger;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseLogger {
	
	private Statement stmt;
	private Connection conn;

	DatabaseLogger(String host, String database, String user, String password) {
		try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://"+host+"/"+database+"?user="+user+"&password="+password);
            
        } catch (Exception ex) {
            System.out.println("SQLException: " + ex.getMessage());
        }        
        System.out.println("[Logores] MySQL-Connection established");
		try {
			stmt = conn.createStatement();
			stmt.execute("CREATE TABLE IF NOT EXISTS `logores_log` (`id` int(10) unsigned NOT NULL AUTO_INCREMENT,`date` varchar(255) NOT NULL,`username` varchar(50) NOT NULL,`ore` varchar(50) NOT NULL,`x` int(10) unsigned NOT NULL,`y` int(10) unsigned NOT NULL,`z` int(10) unsigned NOT NULL,`light` int(10) NOT NULL,`world` varchar(50) NOT NULL,`t` varchar(50) NOT NULL,`d` int(10) unsigned NOT NULL,`b` int(10) unsigned NOT NULL,`ratio` varchar(50) NOT NULL,`flagged` varchar(255) NOT NULL,PRIMARY KEY (`id`)) ENGINE=MyISAM DEFAULT CHARSET=utf8;");		
			} catch (SQLException e) {
			// TODO Auto-generated catch block
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
	
	public void newLogEvent(String date,String username,String ore,int x,int y,int z,int light,String world,String t,int d,int b,String ratio,String flagged) {
		try {
			stmt = conn.createStatement();
			stmt.execute("INSERT INTO logores_log (date, username, ore, x, y, z, light, world, t, d, b, ratio, flagged) VALUES ('"+date+"','"+username+"','"+ore+"',"+x+","+y+","+z+","+light+",'"+world+"','"+t+"',"+d+","+b+",'"+ratio+"','"+flagged+"')");		
			} catch (SQLException e) {
			// TODO Auto-generated catch block
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
	
	public void connClose() {
		try {
			conn.close();
		} catch (SQLException e) {
			// ignore 
		}	
	}
}