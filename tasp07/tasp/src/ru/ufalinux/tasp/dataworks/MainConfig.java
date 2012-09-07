package ru.ufalinux.tasp.dataworks;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Vector;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import android.content.SharedPreferences;

public class MainConfig {
	public static boolean parsed=false;
	public static JabberConfig jabber;
	public static String mainDir="";
	public static Vector<Tariff> tariffs;
	public static int tariffCount;
	public static Integer overSpeed1 = 0;
	public static Integer overSpeed2 = 0;
	public static String firmName="";
	public static Vector<String>orderTimes;
	public static int coordsPeriod=60;
	public static int ordersPeriod=60;
	public static float orderDistAlert=(float) 0.0;
	public static boolean ordInfoOnWait=true;
	public static boolean floorSum=true;
	public static Vector <Call>calls;
	
	public MainConfig(SharedPreferences prefs){
		System.out.println("mainConfig");
		tariffs=new Vector<Tariff>();
		calls=new Vector<Call>();
		orderTimes=new Vector<String>();
		jabber=new JabberConfig();
		if(jabber.parsePrefs(prefs)){
			System.out.println("jabber config parsed");
			parsed=true;
		}
		else
			parsed=false;
	}
	
	public static boolean getBool(String s){
//		String lc=s.toLowerCase();
//		System.out.println(s+":"+s.equalsIgnoreCase("1"));
		if(s.equalsIgnoreCase("yes")||s.equalsIgnoreCase("true")||s.equalsIgnoreCase("on")||s.equals("1"))
			return true;
		else
			return false;
	}
	
	public static boolean parse(String input){
		try {
			InputStream is=new ByteArrayInputStream(input.getBytes("UTF-8"));
			Ini ini=new Ini(is);
			Section settings=ini.get("SETTINGS");
			if(settings!=null){
				orderTimes.clear();
				for(int i=0;i<10;i++){
					if(settings.get("OrderTime"+i)!=null)
						orderTimes.add(settings.get("OrderTime"+i));
				}
				if(settings.containsKey("CoordsPeriod"))
					coordsPeriod=Integer.parseInt(settings.get("CoordsPeriod"));
				if(settings.containsKey("OrdersPeriod"))
					ordersPeriod=Integer.parseInt(settings.get("OrdersPeriod"));
				if(settings.containsKey("FirmName"))
					firmName=settings.get("FirmName");
				if(settings.containsKey("OverSpeed1"))
					overSpeed1=Integer.parseInt(settings.get("OverSpeed1"));
				if(settings.containsKey("OverSpeed2"))
					overSpeed2=Integer.parseInt(settings.get("OverSpeed2"));
				if(settings.containsKey("OrderDistAlert"))
					orderDistAlert=Float.parseFloat(settings.get("OrderDistAlert"));
				if(settings.containsKey("OrdInfoOnWait"))
					ordInfoOnWait=getBool(settings.get("OrdInfoOnWait"));
			}
			Section tariffsSection=ini.get("TARIFF");
			if(tariffsSection!=null){
				tariffs.clear();
				int cnt=tariffsSection.getAll("Name").size();
				for(int i=0;i<cnt;i++){
					Tariff tmp=new Tariff();
					tmp.name=tariffsSection.getAll("Name").get(i);
					tmp.minimalKm=Float.parseFloat(tariffsSection.getAll("MinimalKm").get(i));
					tmp.minimalPrice=Float.parseFloat(tariffsSection.getAll("MinimalPrice").get(i));
					tmp.priceKm=Float.parseFloat(tariffsSection.getAll("PriceKm").get(i));
					tmp.priceMinute=Float.parseFloat(tariffsSection.getAll("PriceMinute").get(i));
					tmp.waitMinutes=Float.parseFloat(tariffsSection.getAll("WaitMinutes").get(i));
					tmp.kmInMinutes=getBool(tariffsSection.getAll("KmInMinutes").get(i));
					tmp.waitMinutesContinue=getBool(tariffsSection.getAll("WaitMinutes").get(i));
					tmp.autoMinutes=getBool(tariffsSection.getAll("AutoMinutes").get(i));
					tmp.autoKm=getBool(tariffsSection.getAll("AutoKm").get(i));
					tmp.autoKmSpeed=Float.parseFloat(tariffsSection.getAll("AutoKmSpeed").get(i));
					tmp.autoKmTime=Float.parseFloat(tariffsSection.getAll("AutoKmTime").get(i));
					tmp.autoMinutesSpeed=Float.parseFloat(tariffsSection.getAll("AutoMinutesSpeed").get(i));
					tmp.autoMinutesTime=Float.parseFloat(tariffsSection.getAll("AutoMinutesTime").get(i));
					tariffs.add(tmp);
				}
				System.err.println("tariffs:"+tariffs.size());
				tariffCount=tariffs.size();
				System.err.println(tariffs.get(0).name);	
			}
			
			Section callsSection=ini.get("CALL");		
			if(callsSection!=null){
				calls.clear();
				int cnt=callsSection.getAll("Name").size();
				for(int i=0;i<cnt;i++){
					Call tmpCall=new Call();
					tmpCall.name=callsSection.getAll("Name").get(i);
					tmpCall.cost=Float.parseFloat(callsSection.getAll("Price").get(i));
					calls.add(tmpCall);
				}
			}
			System.out.println(firmName);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

}
