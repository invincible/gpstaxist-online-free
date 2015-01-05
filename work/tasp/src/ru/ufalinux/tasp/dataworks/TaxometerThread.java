package ru.ufalinux.tasp.dataworks;

import android.location.Location;
import android.location.LocationManager;

public class TaxometerThread implements Runnable {

	public void run() {
		System.err.println("taxtread started");
		long lastmill = System.currentTimeMillis();
		long millis = 0;
		while (Data.currState == Types.A_ORDER_ONDRIVE
				|| Data.currState == Types.A_ORDER_PAUSED) {
			Location currLoc = Data.locMan
					.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			if (currLoc == null)
				currLoc = new Location(LocationManager.GPS_PROVIDER);
			millis += System.currentTimeMillis() - lastmill;
			float interval = 0;
			if (millis > 60000) {
				interval = 1f * millis / 60000f;
				millis = 0;
			}
			// System.err.println("millis "+millis+" interval: "+interval+
			// " min "+Data.totalMin);
			if (Data.currState == Types.A_ORDER_ONDRIVE) {
				//if (currLoc.getSpeed() < Data.currTariff.autoMinutesSpeed) {
				if (currLoc.getSpeed() < Data.currTariff.autoMinutesSpeed) {
					Data.totalMin += interval;
					float tmpmin = (Data.totalMin - Data.currTariff.waitMinutes);
				// Не считаем простой 	
				//	if (tmpmin > 0 || Data.totalKm > 0)
				//		Data.totalCost += Data.currTariff.priceMinute
				//				* interval;
				}

				if (Data.gpsLastLoc == null) { // no previous location
					Data.gpsLastLoc = currLoc;
					continue;
				}

				if (currLoc.hasAccuracy() && (currLoc.getAccuracy() < 100)) {
					float dist = currLoc.distanceTo(Data.gpsLastLoc);
					if (dist < 100) {
						Data.totalKm += dist / 1000;
						float kmnow = Data.totalKm - Data.currTariff.minimalKm;
						if (kmnow > 0) {
							Data.totalCost += Data.currTariff.priceKm
									* (dist / 1000);
						}
						if (!Data.currTariff.waitMinutesContinue) {
							Data.currTariff.waitMinutes = 0f;
						}
					}
					Data.gpsLastLoc = currLoc;
				}
			}
			lastmill = System.currentTimeMillis();
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		System.err.println("taxtread stopped");
	}

}
