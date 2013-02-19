package ru.ufalinux.tasp.jabberworks;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.MainConfig;

public class MainJabberThread implements Runnable {

	XMPPConnection connection;
	
	public void login(String host, String port, String userName, String password)
			throws XMPPException {
		ConnectionConfiguration config = new ConnectionConfiguration(host,
				Integer.parseInt(port), host);
		config.setSelfSignedCertificateEnabled(true);
		config.setReconnectionAllowed(true);
		connection = new XMPPConnection(config);

		connection.connect();
		
		System.out.println(connection);
		System.out.println(userName + " " + password);
		connection.login(userName, password);
		Data.lastJabberAct=30;
	}
	
	public void run() {
		try {
			System.err.println("jabber thread try to login");
			login(MainConfig.jabber.server, MainConfig.jabber.port,
					MainConfig.jabber.user, MainConfig.jabber.password);
			System.err.println("login succesfull");
			Data.jabberLogged=true;
			Thread jabberListenerThread=new Thread(new JabberListenerThread(connection));
			jabberListenerThread.start();
			Thread jabberSenderThread=new Thread(new JabberSenderThread(connection));
			jabberSenderThread.start();
			//Data.tryLogin();
		} catch (XMPPException e) {
			e.printStackTrace();
//			this.stopSelf();
		}
	}

}
