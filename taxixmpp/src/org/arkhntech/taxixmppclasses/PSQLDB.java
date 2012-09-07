package org.arkhntech.taxixmppclasses;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class PSQLDB extends TaxixmppDatabase {
	
	public PSQLDB(MainConfig cfg){
		this.cfg=cfg;
	}
	
	public boolean connect() {
		Properties properties = new Properties();
		properties.setProperty("user", cfg.username_psql);
		properties.setProperty("password", cfg.password_psql);

		try {
			Class.forName("org.postgresql.Driver");
			if(conn!=null){
				conn.close();
			}
			this.conn = DriverManager.getConnection("jdbc:postgresql://"
					+ cfg.host_psql + "/" + cfg.database_psql, properties);
			System.out
					.println("Connection to PostreSQL successful.\n");
			stat = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		System.gc();
		return true;
	}

	public int execute(String sqlcomm) {
		try {
			int cnt=stat.executeUpdate(sqlcomm);
			return cnt;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public boolean query(String sqlcomm) {
		try {
			res=stat.executeQuery(sqlcomm);
			if(res.next())
				return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int executePrep() {
		try {
			int cnt=prep.executeUpdate();
			return cnt;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public boolean queryPrep() {
		try {
			res=prep.executeQuery();
			if(res.next())
				return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean isConnected(){
		try{
			query("select 1");
			return true;
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
	}
}
