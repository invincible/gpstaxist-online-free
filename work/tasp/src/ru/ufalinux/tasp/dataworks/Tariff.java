package ru.ufalinux.tasp.dataworks;

public class Tariff {
	public String name;
	public Integer id;
	public Float minimalKm=(float) 0;
	public Float minimalPrice=(float) 0; 
	public Float priceKm=(float) 0;
	public Float priceMinute=(float) 0;
	public Float waitMinutes=(float) 0;
	public boolean kmInMinutes;
	public boolean waitMinutesContinue;
	public boolean autoMinutes;
	public Float autoMinutesSpeed=(float) 0;
	public Float autoMinutesTime=(float) 0;
	public Float autoKmSpeed=(float) 0;
	public Float autoKmTime=(float) 0;
	public boolean autoKm;
	public Float call=(float) 0;
	public boolean recalc=false;
}
// round totalcost