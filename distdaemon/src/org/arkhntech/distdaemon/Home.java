package org.arkhntech.distdaemon;

import java.io.Serializable;

public class Home implements Serializable{

	private static final long serialVersionUID = 4693669868620624662L;
	public String num="";
	public Integer wp=0;
	public Integer id=0;
	
	public Home(Integer id, String num, Integer neighbour){
		this.num=num;
		this.wp=neighbour;
		this.id=id;
	}
}
