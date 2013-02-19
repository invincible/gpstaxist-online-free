package org.arkhntech.taxixmppclasses;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class MySQLDB extends TaxixmppDatabase {
	
	public MySQLDB(MainConfig cfg){
		this.cfg=cfg;
	}
	
	@Override
	public boolean connect() {
		Properties properties = new Properties();
		properties.setProperty("user", cfg.username_mysql);
		properties.setProperty("password", cfg.password_mysql);
		properties.setProperty("useUnicode", "true");
		properties.setProperty("characterEncoding", "CP1251");
		try {
			Class.forName("com.mysql.jdbc.Driver");
			if(conn!=null){
				conn.close();
			}
			conn = DriverManager.getConnection("jdbc:mysql://" + cfg.host_mysql
					+ "/" + cfg.database_mysql, properties);
			System.out.println(conn.getMetaData().getDatabaseProductName()
					+ ": success");
			stat = conn.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public boolean queryPrep() {
		try {
			if(res!=null)
				res.close();
			res=prep.executeQuery();
			if(res.first()){
				res.beforeFirst();
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
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

	public int rowCount(){
		int cnt=0;
		try {
			res.last();
			cnt=res.getRow();
			res.beforeFirst();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return cnt;
	}
	
}
