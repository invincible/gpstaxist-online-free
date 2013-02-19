package org.arkhntech.taxixmppclasses;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public abstract class TaxixmppDatabase {
	
	protected Statement stat;
	protected Connection conn;
	protected ResultSet res;
	protected MainConfig cfg;
	
	public abstract boolean connect();
	
	public boolean isConnected(){
		try {
			return conn.isValid(3);
		} catch (Exception e) {
			return false;
		}
//		try {
//			
//			prepare("select now();");
//			return queryPrep();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return false;
//		}
	}
	
	//public abstract int execute(String sqlcomm);
	public abstract int executePrep();
	//public abstract boolean query(String sqlcomm);
	public abstract boolean queryPrep();
	
	public boolean next(){
		try {
			return res.next();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected java.sql.PreparedStatement prep;
	
	public Boolean getBoolean(String column){
		try {
			return res.getBoolean(column);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Float getFloat(String column){
		try {
			return res.getFloat(column);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public String getString(String column){
		try {
			return res.getString(column);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Integer getInt(String column){
		try {
			return res.getInt(column);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Long getLong(String column){
		try {
			return res.getLong(column);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public boolean prepare(String stmtText){
		try {
			if(prep!=null)
				this.prep.close();
			this.prep=conn.prepareStatement(stmtText);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean setString(int num,String str){
		try {
			prep.setString(num, str);
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean setLong(int num, long data){
		try {
			prep.setLong(num, data);
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean setLong(int num, String data){
		try {
			prep.setLong(num, Long.parseLong(data));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
}
