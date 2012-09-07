package ru.ufalinux.tasp.dataworks;

import android.os.Bundle;

public class NMEA {
	
	public Bundle parse(String input) {
		Bundle nmea = new Bundle();
		input.replace("$", "");
		String[] fields = input.split(",", -1);
		switch (NMEATypes.valueOf(fields[0])) {
		case GPGGA:
			nmea.putString("type", "GPGSA");
			nmea.putInt("hour", Integer.parseInt(fields[1].substring(0, 1)));
			nmea.putInt("minute", Integer.parseInt(fields[1].substring(2, 3)));
			nmea.putInt("second", Integer.parseInt(fields[1].substring(4, 5)));
			float lat=0.0f;
			if(fields[2].length()>0)
				lat=Float.parseFloat(fields[2].substring(0,1))+Float.parseFloat(fields[2].substring(2))/60;
			nmea.putFloat("lat", lat);
			float lon=0.0f;
			if(fields[2].length()>0)
				lat=Float.parseFloat(fields[2].substring(0,1))+Float.parseFloat(fields[2].substring(2))/60;
			nmea.putFloat("lon", lon);
			break;
		default:
			break;
		}
		return nmea;
	}

}
