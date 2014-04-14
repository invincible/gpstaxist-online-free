package org.arkhntech.taxixmppclasses;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;
import org.ini4j.InvalidFileFormatException;

public class MainConfig {

	protected String database_mysql = "";
	protected String username_mysql = "";
	protected String password_mysql = "";
	protected String host_mysql = "";

	protected String database_psql = "";
	protected String username_psql = "";
	protected String password_psql = "";
	protected String host_psql = "";

	protected String jabberName;
	protected String jabberHost;
	protected String jabberPass;
	protected String jabberPort;
	protected String jabberAcc;

	protected boolean callAsMinimal = false;
	protected boolean speedLimit = false;
	protected boolean autoUpdate = false;
	protected boolean confUpdate = false;
	protected String currVersion;
	protected String updateUrl;
	protected Integer cityout = 0;
	protected Integer pretime = 45;
	protected String updateConfigs;
	protected String urlOwn;
	protected String autoMinutes = "Yes";
	protected Integer autoMinutesTime = 10;
	protected Integer autoMinutesSpeed = 5;
	protected String autoKm = "Yes";
	protected Integer autoKmSpeed = 3;
	protected Integer autoKmTime = 5;
	protected String autoMinutesContinue = "No";
	protected String tmplocation = "/tmp/";
	protected boolean zone_tariffs = false;
	protected boolean tariff_recalc = false;
	protected boolean send_cost = false;
	protected boolean only_default_car = false;
	protected boolean alarm_disp = false;
	protected boolean alarm_drivers = false;
	protected Integer shift_report = 0;
	protected Integer daily_report = 0;
	protected Integer balance_request = 0;
	protected boolean disable_busy_state = false;
	protected Integer cityout_tariff = 1;
	protected boolean show_destination = false;
	protected boolean only_disp = false;
	protected boolean autocomplete = true;
	protected boolean compute_stops = false;
	protected Integer freerun_delay = 0;
	protected String freerun_denied_prefix = "";
	protected Long minimal_balance = (long) 0;
	protected Integer ttl = 1000;
	protected Integer orders_coords_count = 10;
	protected boolean zone_stops = false;
	protected String order_vendor = null;
	protected String town = null;
	protected boolean dcards = false;
	protected boolean dual_cost = false;
	protected Integer border_state = 10;
	protected Integer balance_sign = 99999;
	protected String configBody = "";
	protected int force_tariff=0;
	protected boolean ord_info_on_wait=false; 
	protected boolean address_stops=false;
	protected boolean sendCENA=false; // добавлятьли цена в список заказов
	protected String config_string="";
	protected boolean set_cost=false;
	protected int border_endtask=10;
	protected boolean idle_minutes_on_fixed=false;
	protected String cost_column="ordersum"; 
	protected int channel=0; 
	
	
	
	public boolean read(File inifile) {
		try {
			Ini ini = new Ini(inifile);
			Preferences prefs = new IniPreferences(ini);

			Preferences global = prefs.node("global");
			sendCENA = Boolean.parseBoolean(global.get("sendCENA", "false"));
			callAsMinimal = Boolean.parseBoolean(global.get("call_as_minimal",
					"false"));
			speedLimit = Boolean.parseBoolean(global
					.get("speed_limit", "false"));
			cityout = Integer.parseInt(global.get("outcity", "0"));
			pretime = Integer.parseInt(global.get("pretime", "45"));
			tmplocation = global.get("tmplocation", "/tmp/");
			cityout_tariff = Integer
					.parseInt(global.get("cityout_tariff", "1"));
			zone_tariffs = Boolean.parseBoolean(global.get("zone_tariffs",
					"false"));
			tariff_recalc = Boolean.parseBoolean(global.get("tariff_recalc",
					"false"));
			send_cost = Boolean.parseBoolean(global.get("send_cost", "false"));
			alarm_disp = Boolean
					.parseBoolean(global.get("alarm_disp", "false"));
			alarm_drivers = Boolean.parseBoolean(global.get("alarm_drivers",
					"false"));
			only_default_car = Boolean.parseBoolean(global.get(
					"only_default_car", "false"));
			disable_busy_state = Boolean.parseBoolean(global.get(
					"disable_busy_state", "false"));
			show_destination = Boolean.parseBoolean(global.get(
					"show_destination", "false"));
			only_disp = Boolean.parseBoolean(global.get("only_disp", "false"));
			autocomplete = Boolean.parseBoolean(global.get("autocomplete",
					"true"));
			compute_stops = Boolean.parseBoolean(global.get("compute_stops",
					"false"));
			zone_stops = Boolean
					.parseBoolean(global.get("zone_stops", "false"));
			dcards = Boolean.parseBoolean(global.get("dcards", "false"));
			dual_cost = Boolean.parseBoolean(global.get("dual_cost", "false"));
			shift_report = Integer.parseInt(global.get("shift_report", "0"));
			daily_report = Integer.parseInt(global.get("daily_report", "0"));
			balance_request = Integer.parseInt(global.get("balance_request",
					"0"));
			freerun_delay = Integer.parseInt(global.get("freerun_delay", "0"));
			freerun_denied_prefix = global.get("freerun_denied_prefix", "");
			minimal_balance = Long.parseLong(global.get("minimal_balance","0"));
			ttl = Integer.parseInt(global.get("ttl", "1000"));
			balance_sign = Integer.parseInt(global.get("balance_sign", "99999"));
			orders_coords_count = Integer.parseInt(global.get(
					"orders_coords_count", "10"));
			order_vendor = global.get("order_vendor", null);
			border_state = Integer.parseInt(global.get("border_state", "10"));
			force_tariff=Integer.parseInt(global.get("force_tariff", "0"));
			ord_info_on_wait=Boolean.parseBoolean(global.get("ord_info_on_wait", "false"));
			address_stops=Boolean.parseBoolean(global.get("address_stops", "false"));
			config_string=global.get("config_string", "");
			border_endtask=Integer.parseInt(global.get("border_endtask", "10"));
			set_cost=Boolean.parseBoolean(global.get("set_cost", "false"));
			idle_minutes_on_fixed=Boolean.parseBoolean(global.get("idle_minutes_on_fixed", "false"));
			cost_column=global.get("cost_column", "ordersum");
			channel = Integer.parseInt(global.get("channel", "0"));
			
			Preferences psqlConfig = prefs.node("postgres");
			System.out.println(psqlConfig);
			database_psql = psqlConfig.get("database", "");
			username_psql = psqlConfig.get("user", "");
			password_psql = psqlConfig.get("pass", "");
			host_psql = psqlConfig.get("host", "");

			Preferences mysqlConfig = prefs.node("mysql");
			System.out.println(mysqlConfig);
			database_mysql = mysqlConfig.get("database", "");
			username_mysql = mysqlConfig.get("user", "");
			password_mysql = mysqlConfig.get("pass", "");
			host_mysql = mysqlConfig.get("host", "");

			System.out.println("mybase:" + database_mysql + " "
					+ username_mysql);

			Preferences jabberConfig = prefs.node("jabber");
			jabberName = jabberConfig.get("user", "taxitest");
			jabberPass = jabberConfig.get("pass", "t2xip2ss");
			jabberHost = jabberConfig.get("server", "jabber.org");
			jabberPort = jabberConfig.get("port", "5222");
			jabberAcc = jabberConfig.get("account", "taxitest@jabber.org");

			Preferences defTime = prefs.node("driveopts");
			autoMinutes = defTime.get("auto_minutes", "Yes");
			autoMinutesTime = Integer.parseInt(defTime.get("auto_minutes_time",
					"10"));
			autoMinutesSpeed = Integer.parseInt(defTime.get(
					"auto_minutes_speed", "3"));
			autoKm = defTime.get("auto_km", "Yes");
			autoKmTime = Integer.parseInt(defTime.get("auto_km_time", "10"));
			autoKmSpeed = Integer.parseInt(defTime.get("auto_km_speed", "5"));
			autoMinutesContinue = defTime.get("auto_minutes_continue", "Yes");

			Preferences versionConfig = prefs.node("update");
			autoUpdate = Boolean.parseBoolean(versionConfig.get("auto_update",
					"false"));
			currVersion = versionConfig.get("version", "");
			updateUrl = versionConfig.get("url", "");

			Preferences config = prefs.node("confupdate");
			confUpdate = Boolean.parseBoolean(config
					.get("conf_update", "false"));
			updateConfigs = config.get("url", "");
			urlOwn = config.get("urlOwn", "");
			if (currVersion.length() < 2 || updateUrl.length() < 2
					|| updateConfigs.length() < 2 || urlOwn.length() < 2)
				return false;

			FileInputStream fstream = new FileInputStream("gtconfig.ini");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				configBody += strLine+"\n";
			}
			in.close();

		} catch (InvalidFileFormatException e) {
			e.printStackTrace();
			System.err.println(inifile.getAbsolutePath()
					+ ": Invalid file format");
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("Error reading file "
					+ inifile.getAbsolutePath());
			return false;
		}
		return true;

	}

}
