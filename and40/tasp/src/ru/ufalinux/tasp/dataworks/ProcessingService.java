package ru.ufalinux.tasp.dataworks;

import ru.ufalinux.tasp.jabberworks.MainJabberThread;
import android.app.Service;
import android.content.Intent;
import android.location.LocationManager;
import android.os.IBinder;

public class ProcessingService extends Service {

	public IBinder onBind(Intent intent) {
		return null;
	}

	public static void restartJabber(){
		Data.jabberLogged=false;
		try {
			Thread.sleep(2000);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Thread jabberThread=new Thread(new MainJabberThread());
		jabberThread.start();
	}
	
	public int onStartCommand(Intent intent, int flags, int startId){
		try {
			new Data();
			System.out.println("Data created");
			new GPSData();
//			System.out.println(Data.outcoming.isEmpty());
			Data.locMan=(LocationManager) this.getBaseContext().getSystemService(LOCATION_SERVICE);
//			Data.locMan.addNmeaListener(new TaspNMEAListener());
			Data.locMan.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, new TaspLocationListener());
			
			Thread jabberThread=new Thread(new MainJabberThread());
			jabberThread.start();
			
			Thread procThread=new Thread(new ProcessingThread());
			procThread.start();
			Thread timedThread=new Thread(new TimedThread());
			timedThread.start();
			//Data.tryLogin();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	public void onDestroy(){
		Data.jabberLogged=false;
	}
	
}
