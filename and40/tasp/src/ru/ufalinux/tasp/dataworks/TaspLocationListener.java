package ru.ufalinux.tasp.dataworks;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationProvider;
import android.os.Bundle;

public class TaspLocationListener implements LocationListener {

	public void onLocationChanged(Location loc) {
//		System.out.println(loc.getLatitude()+","+loc.getLongitude()+","+loc.getSpeed());
//		if(Data.currState==Types.A_ORDER_ONDRIVE){
//			DrivePiece curPiece=Data.driveInfo.get(Data.driveInfo.size()-1);
//			float dist=loc.distanceTo(GPSData.lastLoc);
//			if(dist<500){
//				curPiece.km+=dist;
//			}
//		}
//		Data.gpsLastLoc=loc;
		System.out.println(loc.getLatitude());
	}

	public void onProviderDisabled(String provider) {
		GPSData.available=false;
		GPSData.availableSatellites=0;
	}

	public void onProviderEnabled(String provider) {

	}

	public void onStatusChanged(String provider, int status, Bundle extra) {
		if(status==LocationProvider.AVAILABLE){
			int satellites=extra.getInt("satellites");
			GPSData.availableSatellites=satellites;
			System.out.println("current satellites: "+satellites);
		}

	}

}
