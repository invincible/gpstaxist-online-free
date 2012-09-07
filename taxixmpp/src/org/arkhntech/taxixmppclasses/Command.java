package org.arkhntech.taxixmppclasses;

import java.util.HashMap;
import java.util.StringTokenizer;

public class Command {

	public HashMap<String, String> body;
	public String devID;
	public String sign;
	public String from;
	
	public Command() {
		this.body = new HashMap<String, String>();
		this.devID = "";
		this.sign = "";
		this.from="";
	}

	public Command(String from, String devID, String sign, String body) {
		this.devID = devID;
		this.sign = sign;
		this.body=new HashMap<String, String>();
		this.from=from;
//		System.out.println(body);
		StringTokenizer strt = new StringTokenizer(body, "|:");
//		System.out.println(strt.countTokens());
		try {
			while (strt.hasMoreTokens()) {
				this.body.put(strt.nextToken(), strt.nextToken());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
}
