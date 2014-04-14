package ru.ufalinux.tasp.dataworks;

public class Order {
	public Long id=-1l;
	public String addressfrom="";
	public String addressto="";
	public Float xcoord=0f;
	public Float ycoord=0f;
	public String time="";
	public String clientPhone = "";
	public String townfrom ="Уфа";
	public String streetfrom ="Шафиева";
	public String housefrom ="44";
	public String townto ="Уфа";
	public String streetto ="Адмирала Макарова";
	public String houseto ="5/1";
	public String toString(){
		return addressfrom;
	}
}
