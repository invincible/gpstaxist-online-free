package ru.ufalinux.tasp.dataworks;

import java.net.ContentHandler;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import ru.ufalinux.tasp.MainActivity;
import ru.ufalinux.tasp.jabberworks.Command;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;

public class Data {

	public static final String TAG = "TASP07";
	public static String devID;
	public static String sign;
	public static String password;
	public static String car;
	public static boolean isLogged;
	public static Types waiting;
	public static int ttl = 0;
	public static FIFO incoming;
	public static FIFO outcoming;
	public static Tariff currTariff;
	public static Vector<Order> orders;
	public static Order currOrder;
	public static Types currState;
	public static Float totalKm = (float) 0;
	public static Float totalMin = (float) 0;
	public static Float totalCost = (float) 0;
	public static Vector<DrivePiece> driveInfo;
	public static DrivePiece currPiece;
	public static String alert = "";
	//public static String clientPhone = ""; // телефон клиента текущего заказа 
	//public static String dispPhone = "";   // телефон диспетчера
	public static LocationManager locMan;
	public static MainActivity mainAct;
	public static NotificationManager nManager;
	public static Location gpsLastLoc = new Location(
			LocationManager.GPS_PROVIDER);
	public static int msgID = 0;
	public static String version = "0.5.5";
	public static long lastJabberAct = 70;
	public static boolean jabberLogged = false;
	public static Vector<Driverstate> driverstates;
	public static Vector<Driverstops> driverstops;
	public static boolean ordersChanged=false;
	
	public static String[] dispdiliphones = {"+79173487655","+79177959377","+79177504337","+79191577377"}; // телефоны диспетчера дилижанс
	public static int dispcurrphone =  0; // какой номер используем, перебираем по очереди 
	public static String[] dispcp01phones = {"+79646698958"}; // телефоны диспетчера череповец
	public static String[] dispcp02phones = {"88202201515"}; // телефоны диспетчера череповец, звонок клиенту
	
	public Data() {
		incoming = new FIFO();
		outcoming = new FIFO();
		orders = new Vector<Order>();
		isLogged = false;
		waiting = Types.NONE;
		currState = Types.NONE;
		// debug
		devID = "devID";
		sign = "7";
		password = "123";
		car = "7";
		driverstates=new Vector<Driverstate>();
		driverstops=new Vector<Driverstops>();
	}

	public static void changeTariff(Tariff newTariff){
		currTariff=newTariff;
	}
	
	public static void addCall(Call newCall){
		totalCost+=newCall.cost;
	}
	
	public static void initDrive() {
		driveInfo = new Vector<DrivePiece>();
		Thread taxthread = new Thread(new TaxometerThread());
		totalKm = 0f;
		totalMin = 0f;
		totalCost = currTariff.minimalPrice;
		currState=Types.A_ORDER_ONDRIVE;
		taxthread.start();
	}

	public static void showNotify(int icon, String message) {
		// int icon = R.drawable.alert_icon;
		CharSequence tickerText = message;
		long when = System.currentTimeMillis();

		Notification notification = new Notification(icon, tickerText, when);

		Context context = mainAct.getApplicationContext();
		CharSequence contentTitle = message;
		CharSequence contentText = "ТАСП-07";
		Intent notificationIntent;
		try {
			notificationIntent = mainAct.getIntent();
		} catch (Exception e) {
			e.printStackTrace();
			notificationIntent = new Intent(mainAct, MainActivity.class);
		}
		PendingIntent contentIntent = PendingIntent.getActivity(mainAct, 0,
				notificationIntent, 0);

		notification.setLatestEventInfo(context, contentTitle, contentText,
				contentIntent);
		notification.defaults |= Notification.DEFAULT_SOUND;
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
		nManager.notify(msgID, notification);
		msgID++;
	}

	public static synchronized void tryLogin() {
		Command comm = new Command();
		comm.type = "R_OPEN";
		comm.body.put("pass", password);
		comm.body.put("num", car);
		comm.body.put("version", Data.version);
		System.err.println(comm + " " + outcoming);
		try {
			outcoming.push(comm);

		} catch (Exception e) {
			outcoming = new FIFO();
			outcoming.push(comm);
		}
		waiting = Types.R_OPEN;
		ttl = 100;
		synchronized (outcoming) {
			outcoming.notify();
		}
	}

	public static void requestConfig() {
		Command comm = new Command();
		comm.type = "R_CONFIG";
		waiting = Types.R_CONFIG;
		ttl = 100;
		synchronized (outcoming) {
			outcoming.push(comm);
			outcoming.notify();
		}
	}

	public static void requestOrders() {
		Command comm = new Command();
		comm.type = "R_FREERUN";
		outcoming.push(comm);
		waiting = Types.R_FREERUN;
		orders.clear();
		Order tmpOrder=new Order();
		tmpOrder.addressfrom="Запрос списка";
		tmpOrder.id=-1l;
		DateFormat dateFormat = new SimpleDateFormat("HH:mm");
		Date date = new Date();
		tmpOrder.time=dateFormat.format(date);
		orders.add(tmpOrder);
	}

	public static void requestOrderTake(Long id, String info) {
		Command comm = new Command();
		comm.type = "A_ORDER";
		comm.body.put("id", id.toString());
		comm.body.put("state", "CONFIRM");
		comm.body.put("info", info);
		waiting = Types.A_ORDER_CONFIRM;
		outcoming.push(comm);
		for (int i = 0; i < orders.size(); i++)
			if (orders.get(i).id.equals(id)) {
				currOrder = orders.get(i);
				break;
			}
	}

	public static void requestOrderMap(Long id, String info) {
		
		//String SS = Data.orders.get((int) selectedId).addressfrom + Data.orders.get((int) selectedId).addressto;
	//										Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
	//											 Uri.parse("http://maps.google.com/maps?saddr="+SS));
	//											startActivity(intent);
		
	//	Command comm = new Command();
	//	comm.type = "A_ORDER";
	//	comm.body.put("id", id.toString());
	//	comm.body.put("state", "CONFIRM");
	//	comm.body.put("info", info);
	//	waiting = Types.A_ORDER_CONFIRM;
	//	outcoming.push(comm);
		for (int i = 0; i < orders.size(); i++) 
		 if (orders.get(i).id.equals(id)) 
		    {	currOrder = orders.get(i);					
		    String SS = "?saddr=Уфа "+currOrder.addressfrom+"&daddr=Уфа "+currOrder.addressto;
			Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
						 Uri.parse("http://maps.google.com/maps"+SS));
	//		startActivity(intent); 
			break;}
			
	}

	
//	public static void requestStates(){
//		Command comm=new Command();
//		comm.type="R_DRVSTATEID";
//		outcoming.push(comm);
//	}
	
	
	public static void requestOnPlace(Long id) {
		Command comm = new Command();
		comm.type = "A_ORDER";
		comm.body.put("id", id.toString());
		comm.body.put("state", "ONPLACE");
		waiting = Types.A_ORDER_WAITING;
		outcoming.push(comm);
	}

	public static void requestOndrive(Long id) {
		Command comm = new Command();
		comm.type = "A_ORDER";
		comm.body.put("id", id.toString());
		comm.body.put("state", "ONDRIVE");
		Data.totalCost = Data.currTariff.minimalPrice + Data.currTariff.call;
		comm.body.put("info", Data.totalCost.toString());
		waiting = Types.A_ORDER_ONDRIVE;
		outcoming.push(comm);
	}
	
	public static void requestReject(Long id) {
		Command comm = new Command();
		comm.type = "A_ORDER";
		comm.body.put("id", id.toString());
		comm.body.put("state", "REJECT");
		//Data.totalCost = Data.currTariff.minimalPrice + Data.currTariff.call;
		//comm.body.put("info", Data.totalCost.toString());
		//waiting = Types.A_ORDER_ONDRIVE;
		outcoming.push(comm);
	}

	public static void sendOnDrive() {
		Command comm = new Command();
		comm.type = "A_ORDER";
		if (currOrder == null) {
			currOrder = new Order();
			currOrder.id = -1l;
		}
		if (currOrder.id != 0) {
			comm.body.put("id", currOrder.id.toString());
			comm.body.put("state", "ONDRIVE");
			comm.body.put("info", Data.totalCost.toString());
			outcoming.push(comm);
		}
		if (currOrder.id < 0)
			currOrder.id = 0l;

	}

	public static void requestOrderComplete(Long id) {
		Command comm = new Command();
		comm.type = "A_ORDER";
		comm.body.put("id", id.toString());
		comm.body.put("state", "COMPLETED");
		comm.body.put("info", totalCost.toString());
		comm.body.put("price", totalCost.toString());
		waiting = Types.A_ORDER_COMPLETE;
		outcoming.push(comm);
	}

	public static void requestFreeOrder() {

	}

	public static void requestClose() {
		Command comm = new Command();
		comm.type = "R_CLOSE";
		waiting = Types.R_CLOSE;
		try {
			outcoming.push(comm);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void sendCoords() {
		Command comm = new Command();
		if (Data.gpsLastLoc == null) {
			Data.gpsLastLoc = new Location(LocationManager.GPS_PROVIDER);
		}
		// System.err.println("coords");
		comm.type = "A_COORDS";
		comm.body.put("ycoord", "" + Data.gpsLastLoc.getLongitude());
		comm.body.put("xcoord", "" + Data.gpsLastLoc.getLatitude());
		outcoming.push(comm);
	}

	public static void requestStatus(int id){ //меняем статус
		Command comm=new Command();
		comm.type="A_DRVSTATE";
		comm.body.put("id", ""+id);
		outcoming.push(comm);
	}

	public static void requestStop(int id){ //ставимся на стоянку
		Command comm=new Command();
		comm.type="A_DRVSTOP";
		comm.body.put("id", ""+id);
		outcoming.push(comm);
	}

	
	public static void requestDrvstates(){
		Command comm=new Command();
		comm.type="R_DRVSTATEID";
		outcoming.push(comm);
//		waiting=Types.A_DRVSTATEID;
	}
	
	public static void requestStops(){
		Command comm=new Command();
		comm.type="R_DRVSTOPSID";
		outcoming.push(comm);
	}
	
}
