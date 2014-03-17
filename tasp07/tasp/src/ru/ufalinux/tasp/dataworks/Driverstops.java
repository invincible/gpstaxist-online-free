package ru.ufalinux.tasp.dataworks;

public class Driverstops implements Comparable<Driverstops> {

public int id=0;
public String name="";

public Driverstops(int id, String name){
	this.id=id;
	this.name=name;
}
	
	public int compareTo(Driverstops arg0) {
		// TODO Auto-generated method stub
		return this.id-arg0.id;
	}

}
