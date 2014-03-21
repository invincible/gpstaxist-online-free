package ru.ufalinux.tasp.dataworks;

import android.location.Location;
import android.location.LocationManager;

public class GPSData {

	public static Boolean available=false;
	public static int availableSatellites=0;
	public static Location lastLoc=new Location(LocationManager.GPS_PROVIDER);
	
}
