package ru.ufalinux.tasp.dataworks;

import android.location.Location;
import android.location.LocationManager;

public class TimedThread extends Thread {

	public void run() {
		long curr = System.currentTimeMillis();
		long currCoords = System.currentTimeMillis();
		while (true) {
			// System.out.println("tick");
			// System.out.println(System.currentTimeMillis()-curr);
			System.err.println("waiting: " + Data.waiting + ", state:"
					+ Data.currState);
			// System.err.println("incoming " + Data.incoming.size()
			// + " outcoming " + Data.outcoming.size());
			// System.err.println("incoming: "+Data.incoming.toString());
			// System.err.println("outcoming: "+Data.outcoming.toString());
			System.err.println("cost: " + Data.totalCost);
			// System.out.println(Data.isLogged);
			// System.out.println(Data.currState);
			long now = System.currentTimeMillis();
			if ((now - curr >= MainConfig.ordersPeriod * 1000)
					&& (Data.waiting == Types.NONE) && (Data.isLogged)
					&& (Data.currState == Types.NONE)) {
				Data.requestOrders();
				Data.requestStops();
				Data.requestDrvstates();
				curr = System.currentTimeMillis();
			}

			if (Data.currState != Types.A_ORDER_ONDRIVE) {
				try {
					Location currLoc = Data.locMan
							.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					if (currLoc == null)
						currLoc = new Location(LocationManager.GPS_PROVIDER);
					if (Data.gpsLastLoc == null) { // no previous location
						Data.gpsLastLoc = currLoc;
					}
					if (currLoc.hasAccuracy() && (currLoc.getAccuracy() < 100)) {
						Data.gpsLastLoc = currLoc;
					}
				} catch (Exception e) {
					e.printStackTrace();
					Data.gpsLastLoc = new Location(
							LocationManager.NETWORK_PROVIDER);
				}
			}

			if ((now - currCoords >= MainConfig.coordsPeriod * 1000)
					&& (Data.isLogged)) {
				// System.err.println("coords");
				Data.sendCoords();
				currCoords = System.currentTimeMillis();
				if (Data.currState == Types.A_ORDER_ONDRIVE
						|| Data.currState == Types.A_ORDER_PAUSED)
					Data.sendOnDrive();
			}

			Data.lastJabberAct -= 10;
			if (Data.lastJabberAct < 0) {
				try {
					Data.jabberLogged = false;
					try {
						Thread.sleep(2000);
					} catch (Exception e) {
						// TODO: handle exception
					}
					System.err.println("try to restart jabber");
					// Data.mainAct.restartJabber();
					ProcessingService.restartJabber();
					// Data.lastJabberAct=70;

					// Data.waiting=Types.NONE;
					if (Data.currState == Types.NONE)
						Data.requestOrders();
					    Data.requestStops();
					    Data.requestDrvstates();
				} catch (Exception e) {
					e.printStackTrace();
				}

			}

			try {
				if (!Data.mainAct.isJabberServiceRunning()) {
					// Data.mainAct.restartJabber();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

}
