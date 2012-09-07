package ru.ufalinux.tasp.dataworks;

import java.io.File;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

import android.content.SharedPreferences;


public class JabberConfig {
	public String server="";
	public String port="";
	public String user="";
	public String password="";
	public String disp="";
	public String account="";
	
	public boolean parsePrefs(SharedPreferences prefs){
		server=prefs.getString("server","");
		port=prefs.getString("port", "5222");
		user=prefs.getString("user", "");
		password=prefs.getString("password", "");
		disp=prefs.getString("disp", "");
		if(server.length()*port.length()*user.length()*password.length()*disp.length()==0)
			return false;
		account=user+"@"+server;
		if(disp.indexOf("@")==-1)
			disp=disp+"@"+server;
		return true;
	}
	
	public boolean parseConfig(String fname){
		try {
			//System.out.println("parsing ini file: "+fname);
			Ini ini=new Ini(new File(fname));
			Preferences prefs=new IniPreferences(ini);
			Preferences serverPrefs=prefs.node("server");
			server=serverPrefs.get("server", "");
			port=serverPrefs.get("port", "5222");
			Preferences accountPrefs=prefs.node("account");
			user=accountPrefs.get("user", "");
			password=accountPrefs.get("password", "");
			disp=accountPrefs.get("dispaccount", "");
			if(server.length()*port.length()*user.length()*password.length()*disp.length()==0)
				return false;
			account=user+"@"+server;
			
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("error parsing ini file "+fname);
			return false;
		}
		return true;
	}
	
}
