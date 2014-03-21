package ru.ufalinux.tasp.dataworks;

public class Order {
	public Long id=-1l;
	public String addressfrom="";
	public String addressto="";
	public Float xcoord=0f;
	public Float ycoord=0f;
	public String time="";
	
	public String toString(){
		return addressfrom;
	}
}
