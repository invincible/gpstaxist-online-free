package org.arkhntech.taxixmppclasses;

import java.util.HashMap;
import java.util.Vector;

public class TariffInfo {
	public String name="";
	public float MinimalKM=0;
	public float MinimalPrice=0;
	public float PriceKm=0;
	public float PriceMinute=0;
	public float WaitMinutes=0;
	public String AutoMinutes="no";
	public float AutoMinutesSpeed=0;
	public float AutoMinutesTime=0;
	public String AutoKm="yes";
	public String AutoKmSpeed="5";
	public float AutoKmTime=5;
	public Vector<HashMap<String,Float>>  minutesStteps=new Vector<HashMap<String,Float>>();
	public String MinutesType="STEPONE";
	public float call=0;
	public float OverSpeed1=0;
	public float OverSpeed2=0;
}
