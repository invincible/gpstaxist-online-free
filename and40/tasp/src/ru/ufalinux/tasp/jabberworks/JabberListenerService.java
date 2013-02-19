package ru.ufalinux.tasp.jabberworks;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.MainConfig;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.IBinder;

public class JabberListenerService extends Service {
	
	XMPPConnection connection;
	
	@Override
	public IBinder onBind(Intent arg0) {
		System.err.println("onBind jabber");
		return null;
	}
	
	public void login(String host, String port, String userName, String password)
			throws XMPPException {
		ConnectionConfiguration config = new ConnectionConfiguration(host,
				Integer.parseInt(port), host);
		config.setSelfSignedCertificateEnabled(true);
		connection = new XMPPConnection(config);

		connection.connect();
		System.out.println(connection);
		System.out.println(userName + " " + password);
		connection.login(userName, password);
	}
	
	public int onStartCommand(Intent intent, int flags, int startId){
		try {
			ConnectivityManager connMan=(ConnectivityManager) this.getBaseContext().getSystemService(Context.CONNECTIVITY_SERVICE);
			connMan.startUsingNetworkFeature(ConnectivityManager.TYPE_MOBILE, "enableSUPL");
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
			System.err.println("try to login");
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
			this.stopSelf();
		}
		
		return 0;
	}
	
	public void onDestroy(){
		System.err.println("destroy jabber service");
		connection.disconnect();
	}

}
