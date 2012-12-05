package org.arkhntech.distdaemon;

import java.io.Serializable;
import java.util.Vector;

public class Street implements Serializable{

	private static final long serialVersionUID = 9191666329348753775L;
	public Integer id=0;
	public String name="";
	public Vector<Home> homes=null;
	public Integer midPoint=0;
	
	public Street(Integer id,String name,Integer midPoint){
		this.id=id;
		this.name=name;
		this.midPoint=midPoint;
	}
}
