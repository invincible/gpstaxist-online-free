package ru.ufalinux.tasp.jabberworks;

import java.util.StringTokenizer;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import ru.ufalinux.tasp.dataworks.Data;

public class JabberListenerThread implements Runnable {

	XMPPConnection connection;
	PacketFilter filter = null;

	public JabberListenerThread(XMPPConnection conn){
		this.connection=conn;
	}
	

	public void run() {
		System.out.println("listener started");
		PacketFilter filter = new PacketFilter() {
			public boolean accept(Packet packet) {
				boolean ret=false;
				if (packet.getClass().equals(Message.class)){
					System.out.println("message");
					ret=true;
				}else
					System.out.println("not message");
				return ret;
			}
		};

		PacketCollector collector = connection.createPacketCollector(filter);

		Packet currPack;

		while ((currPack = collector.nextResult()) != null) {
			try {
				String messBody = ((Message) currPack).getBody();
				if ((messBody != null)&&(messBody.length()>0)) {
					System.out.println(processPacket(currPack));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try{
		connection.disconnect();
		}catch(Exception e){
			e.printStackTrace();
		}

	}
	
	public boolean processPacket(Packet pack){
		Message mess=(Message)pack;
		String body=mess.getBody();
		if(body.equals("bye")){
			connection.disconnect();
			return false;
		}

		String id = "";
		String sign = "";
		String commBody = "";
		String strType = "A_ERR";
		System.out.println("comm body: "+body);
		Command comm;
		StringTokenizer strt = new StringTokenizer(body, "\n");
		try {
			id = strt.nextToken();
			sign = strt.nextToken();
			if(!(id.equals("0")&&sign.equals("0"))){
				System.out.println("isnt disp message");
				return false;
			}
			strType = strt.nextToken();
			try {
				while(strt.hasMoreTokens())
					commBody = commBody+strt.nextToken()+"\n";
				comm=new Command(strType,commBody);
				synchronized (Data.incoming) {
					Data.incoming.push(comm);
					Data.incoming.notifyAll();
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			System.err.println("packet parsing error");
			e.printStackTrace();
		}
		
		//connection.sendPacket(new SendingPacket(from, MainConfig.jabber.account, outBody));

		return true;
	}

}
