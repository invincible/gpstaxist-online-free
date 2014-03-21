package ru.ufalinux.tasp.jabberworks;

import org.jivesoftware.smack.packet.Packet;

import ru.ufalinux.tasp.dataworks.MainConfig;


public class SendingPacket extends Packet {

	String to="";
	String from="";
	String body="";
	
	public SendingPacket(String to, String from,String body){
		this.to=to;
		this.from=from;
		this.body=body;
	}
	
	public SendingPacket(String body){
		this.to=MainConfig.jabber.disp;
		this.from=MainConfig.jabber.account;
		this.body=body;
	}
	
	public String toXML() {
		
		return "<message xml:lang=\"en\"  to=\""+to+"\" from=\""+from+"\" type=\"chat\">"+
		  "<body>"+body+"</body>"+"</message>";
	}

}
