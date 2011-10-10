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
	}
	
	public void newLogEvent(String date,String username,String ore,int x,int y,int z,int light,String world,String t,int d,int b,String ratio,String flagged) {
		try {
			stmt = conn.createStatement();
			//System.out.println("INSERT INTO logores_log (date, username, ore, x, y, z, light, world, t, d, b, ratio, flagged) VALUES ('"+date+"','"+username+"','"+ore+"',"+x+","+y+","+z+","+light+",'"+world+"','"+t+"',"+d+","+b+",'"+ratio+"','"+flagged+"')");
			stmt.execute("INSERT INTO logores_log (date, username, ore, x, y, z, light, world, t, d, b, ratio, flagged) VALUES ('"+date+"','"+username+"','"+ore+"',"+x+","+y+","+z+","+light+",'"+world+"','"+t+"',"+d+","+b+",'"+ratio+"','"+flagged+"')");		
		//	stmt.("INSERT INTO logores_log (username`) VALUES ("+username+")");		
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
        
