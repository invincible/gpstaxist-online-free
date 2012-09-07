package ru.ufalinux.tasp.dataworks;

public class Driverstate implements Comparable<Driverstate>{
	public int id=0;
	public String name="";
	
	public Driverstate(int id, String name){
		this.id=id;
		this.name=name;
	}

	public int compareTo(Driverstate arg0) {
		return this.id-arg0.id;
	}
}
