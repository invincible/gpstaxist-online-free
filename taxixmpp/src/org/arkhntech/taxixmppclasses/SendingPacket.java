package org.arkhntech.taxixmppclasses;

import org.jivesoftware.smack.packet.Packet;


public class SendingPacket extends Packet {

	String to="";
	String from="";
	String body="";
	
	public SendingPacket(String to, String from,String body){
		this.to=to;
		this.from=from;
		this.body=body;
	}
	
	public String toXML() {
		
		return "<message xml:lang=\"en\"  to=\""+to+"\" from=\""+from+"\" type=\"chat\">"+
		  "<body>"+body+"</body>"+"</message>";
	}

}
