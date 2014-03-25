package org.arkhntech.taxixmppclasses;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import javax.net.ssl.SSLSocketFactory;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

public class MainClass {

	XMPPConnection connection;
	PacketFilter filter = null;
	Vector<String> validUsers = new Vector<String>();

	public void login(String host, String port, String userName, String password)
			throws XMPPException {
		ConnectionConfiguration config = new ConnectionConfiguration(host,
				Integer.parseInt(port), host);
//		config.setSelfSignedCertificateEnabled(true);
		config.setSelfSignedCertificateEnabled(true);
		
//		SASLAuthentication.supportSASLMechanism("PLAIN",0);
//		SASLAuthentication.supportSASLMechanism("PLAIN",0);
//		config.setSASLAuthenticationEnabled(false);
//		config.setSASLAuthenticationEnabled(false);

//		config.setSecurityMode(ConnectionConfiguration.SecurityMode.enabled);
//      config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
		//config.setSocketFactory(SSLSocketFactory.getDefault());

		connection = new XMPPConnection(config);
		connection.connect();
		System.out.println(connection);
		System.out.println(userName+" "+password);
		connection.login(userName, password);
	}

	public void fillBuddyList() {
		Roster roster = connection.getRoster();
		 try {
		 roster.createEntry("arkhnchul@jabber.ru", "arkhnchul@jabber.ru", null);
		 } catch (XMPPException e) {
		 e.printStackTrace();
		 }
		Collection<RosterEntry> entries = roster.getEntries();

		System.out.println("" + entries.size() + " buddy(ies):");
		for (RosterEntry r : entries) {
			System.out.println(r.getUser());
			validUsers.add(r.getUser());
		}
	}

	public void disconnect() {
		connection.disconnect();
	}

	public static void main(String[] args) throws XMPPException, IOException {
		final MainClass c = new MainClass();
		
		Data data = new Data(c.connection);
		File cfile=new File("config.ini");
		if(!data.cfg.read(cfile)){
			System.err.println("unable to read config from "+cfile.getAbsolutePath());
			System.exit(1);
		}
		data.readStates();
		//new TariffWorks(data.cfg);
		
		// turn on the enhanced debugger
//		XMPPConnection.DEBUG_ENABLED = true;
		// Enter your login information here
		c.login(data.cfg.jabberHost,data.cfg.jabberPort,data.cfg.jabberName, data.cfg.jabberPass);

		data.connection=c.connection;
		data.checkConnection();
		c.fillBuddyList();

		c.filter = new PacketFilter() {
			public boolean accept(Packet packet) {
				// System.out.println(packet.getFrom());
				if (!packet.getClass().equals(Message.class))
					return false;
				// String from = packet.getFrom();
				// for (int i = 0; i < c.validUsers.size(); i++)
				// if (from.contains(c.validUsers.get(i)))
				// return true;
				// return false;
				return true;
			}
		};
		

		PacketCollector collector = c.connection
				.createPacketCollector(c.filter);

		Packet currPack;

			while ((currPack = collector.nextResult()) != null) {

				String messBody = ((Message) currPack).getBody();

				if (messBody != null) {
					if (!data.processPacket(currPack)){
						data.writeStates();
						data.dbtimer.cancel();
						c.disconnect();
						System.exit(0);
						break;
					}
				}
			}
		
		c.disconnect();
		System.exit(0);
	
	}

}
