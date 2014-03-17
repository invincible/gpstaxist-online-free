package ru.ufalinux.tasp.dataworks;

import java.util.Collections;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import ru.ufalinux.tasp.MainActivity;
import ru.ufalinux.tasp.R;
import ru.ufalinux.tasp.jabberworks.Command;

public class ProcessingThread implements Runnable {

	public void run() {
		try {
			while (true) {
				Thread.sleep(1000);
//				System.err.println("processing tick");
				while (!Data.incoming.isEmpty()) {
					Data.lastJabberAct = 70;
//					System.err.println("incoming: " + Data.incoming.toString());
					Command comm = Data.incoming.poll();
//					System.err.println(comm.type);
					try {

						switch (Types.valueOf(comm.type)) {
						case A_OK:
							processA_OK(comm);
							break;
						case A_ERR:
							processA_ERR(comm);
							break;
						case A_CONFIG:
							processA_CONFIG(comm);
							break;
						case A_TARIF:
							processA_TARIF(comm);
							break;
						case R_ORDER:
							processR_ORDER(comm);
							break;
						case R_ORDERID:
							processR_ORDERID(comm);
							break;
						case A_INFO:
							processA_INFO(comm);
							break;
						case R_ANDROIDUPDATE:
							processR_ANDROIDUPDATE(comm);
							break;
						case A_DRVSTATEID:
							Log.d(Data.TAG, "comm STATE="+comm.toString()); 
							processA_DRVSTATEID(comm);
							break;
						case A_DRVSTOPSID:
							Log.d(Data.TAG, "comm STOPS="+comm.toString()); 
							processA_DRVSTOPSID(comm);
							break;
						default:
							System.out.println("Invalid command " + comm.type);
							break;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void processR_ANDROIDUPDATE(Command comm) {
		String url = "http://" + comm.body.get("url");
		Data.showNotify(R.drawable.icon_info,
				"Доступно обновление ТАСП-07, дождитесь загрузки");
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		Data.mainAct.startActivity(i);
		Data.requestClose();
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Data.mainAct.stopService(MainActivity.jabber);
		Data.mainAct.stopService(MainActivity.processing);
		System.exit(0);

	}

	public void processR_ORDER(Command comm) {
		Order curr = new Order();
		curr.id = Long.parseLong(comm.body.get("id"));
		curr.addressfrom = comm.body.get("address");
		curr.xcoord = Float.parseFloat(comm.body.get("xcoord"));
		curr.ycoord = Float.parseFloat(comm.body.get("ycoord"));
		curr.time = comm.body.get("time");
		curr.time = curr.time.replace("\n", "");
		curr.addressto = comm.body.get("address2");
		System.out.println(curr.addressfrom);
		if (Data.waiting.equals(Types.R_FREERUN)){
			if(!Data.orders.isEmpty()&&Data.orders.get(0).id==-1l){
				Data.orders.clear();
			}
			Data.orders.add(curr);
		}
		System.out.println("order passed");
		Data.ordersChanged = true;
		synchronized (Data.orders) {
			Data.orders.notifyAll();
		}
	}

	public void processA_TARIF(Command comm) {
		System.out.println("processing tariff");
		Tariff curr = new Tariff();
		curr.minimalKm = Float.parseFloat(comm.body.get("MinimalKm"));
		curr.minimalPrice = Float.parseFloat(comm.body.get("MinimalPrice"));
		curr.priceKm = Float.parseFloat(comm.body.get("PriceKm"));
		curr.priceMinute = Float.parseFloat(comm.body.get("PriceMinute"));
		curr.waitMinutes = Float.parseFloat(comm.body.get("WaitMinutes"));
		curr.autoMinutes = MainConfig.getBool(comm.body.get("AutoMinutes"));
		curr.autoKm = MainConfig.getBool(comm.body.get("AutoKm"));
		curr.autoMinutesSpeed = Float.parseFloat(comm.body
				.get("AutoMinutesSpeed"));
		curr.autoMinutesTime = Float.parseFloat(comm.body
				.get("AutoMinutesTime"));
		curr.autoKmSpeed = Float.parseFloat(comm.body.get("AutoKmSpeed"));
		curr.autoKmTime = Float.parseFloat(comm.body.get("AutoKmTime"));
		curr.call = Float.parseFloat(comm.body.get("call"));
		curr.waitMinutesContinue = MainConfig.getBool(comm.body
				.get("WaitMinutesContinue"));

		String recalc = comm.body.get("recalc");
		// System.out.println("got recalc: "+recalc+":");
		curr.recalc = MainConfig.getBool(recalc);
		//MainConfig.tariffs.add(curr);
		Data.currTariff = curr;
		System.out.println("recalc: " + curr.recalc);
		if (curr.recalc) {
			float tmpkm = Data.totalKm - curr.minimalKm;
			if (tmpkm < 0)
				tmpkm = 0;
			float tmpmin = Data.totalMin - curr.waitMinutes;
			if (tmpmin < 0)
				tmpmin = 0;
			Data.totalCost = curr.minimalPrice + curr.priceKm * tmpkm
					+ curr.priceMinute * tmpmin;
			// System.out.println("newcost: "+Data.totalCost);
		}
		System.err.println("tariff proceed");
	}

	public void processA_CONFIG(Command comm) {
		if (Data.waiting == Types.R_CONFIG)
			Data.waiting = Types.NONE;
		MainConfig.parse(comm.body.get("content"));
		if (Data.currTariff == null && !MainConfig.tariffs.isEmpty())
			Data.currTariff = MainConfig.tariffs.get(0);
	}

	public void processA_OK(Command comm) {
		System.err.println("OK processing");
		switch (Data.waiting) {
		case R_OPEN:
			Data.waiting = Types.NONE;
			Data.isLogged = true;
			System.out.println("login success");
			Data.requestConfig();
			Data.requestDrvstates();
			Data.requestOrders();
			
			Data.requestStops();
			break;
		case R_FREERUN:
			Data.waiting = Types.NONE;
			if(Data.orders.get(0).id==-1l){
				Data.orders.clear();
			}
			// System.out.println("FREERUN gained");
			break;
		case A_ORDER_CONFIRM:
			System.out.println("confirmed1");
			Data.waiting = Types.NONE;
			Data.currState = Types.A_ORDER_CONFIRM;
			synchronized (Data.currState) {
				Data.currState.notify();
			}
			Data.orders.clear();
			System.out.println("confirmed");
			break;
		case A_ORDER_WAITING:
			Data.currState = Types.A_ORDER_WAITING;
			synchronized (Data.currState) {
				Data.currState.notify();
			}
			Data.waiting = Types.NONE;
			System.out.println("awaiting confirmed");
			break;
		case A_ORDER_ONDRIVE:
			Data.currState = Types.A_ORDER_ONDRIVE;
			Data.waiting = Types.NONE;
			// Data.initDrive();
			break;
		case A_ORDER_COMPLETE:
			Data.currState = Types.NONE;
			Data.waiting = Types.NONE;
			break;
		case NONE:
		case R_CONFIG:
		default:
			System.out.println("A_OK is garbage");
			break;
		}
	}

	public void processA_DRVSTATEID(Command comm) {
		// System.out.println(comm.body.size()+" values");
		// for(int i=2; i<comm.body.size();i+=2){
		// Data.driverstates.add(new
		// Driverstate(Integer.parseInt(comm.body.get("id"+i)),
		// comm.body.get("state"+i+1)));
		// System.err.println(comm.body.get("id"+i));
		// }
		Log.d(Data.TAG, "==== A_DRVSTATEID ===== "+ comm.body.toString());
		for (String num : comm.body.keySet()) {
			System.err.println("num:" + num);
			Log.d(Data.TAG,"A_DRVSTATEID" + num.toString()+ comm.body.get(num));
			Driverstate curr = new Driverstate(Integer.parseInt(num),
					comm.body.get(num));
			Data.driverstates.add(curr);
		}

		Log.d(Data.TAG, "A_DRVSTATEID");
		//for (String num : comm.body.keySet()) {
		//	System.err.println("num:" + num);
		//	Driverstops curr = new Driverstops(Integer.parseInt(num),
		//			comm.body.get(num));
		//	Data.driverstops.add(curr);
		//}

		System.err.println(Data.driverstates.size() + " states");
		//System.err.println(Data.driverstops.size() + " states");
		Collections.sort(Data.driverstates);
		//Collections.sort(Data.driverstops);
	}
	public void processA_DRVSTOPSID(Command comm) {
		

		Log.d(Data.TAG, "==== A_DRVSTOPSID ===== "+ comm.body.toString());
		for (String num : comm.body.keySet()) {
			System.err.println("num:" + num);
			Log.d(Data.TAG,"33A_DRVSTOPSID" + num.toString()+ comm.body.get(num));
			Driverstops curr = new Driverstops(Integer.parseInt(num),
					comm.body.get(num));
			Data.driverstops.add(curr);
		}

		//System.err.println(Data.driverstates.size() + " states");
		System.err.println(Data.driverstops.size() + " states");
		//Collections.sort(Data.driverstates);
		Collections.sort(Data.driverstops);
	}
	

	public void processA_INFO(Command comm) {
		Data.alert = "Сообщение:\n" + comm.body.get("info");
		synchronized (Data.alert) {
			Data.alert.notifyAll();
		}
		Data.showNotify(R.drawable.icon_info, Data.alert);
	}

	public void processA_ERR(Command comm) {
		switch (Data.waiting) {
		case R_OPEN:
			System.out.println("Login unsuccesful");
			Data.waiting = Types.NONE;
			Data.alert = "Ошибка:\n" + comm.body.get("info");
			// synchronized (Data.alert) {
			// Data.alert.notifyAll();
			// }
			// Data.showNotify(Data.alert);
			break;
		case A_ORDER_CONFIRM:
		case A_ORDER_WAITING:
		case A_ORDER_ONDRIVE:
			Data.currOrder = null;
			Data.waiting = Types.NONE;
			Data.currState = Types.NONE;
			Data.alert = "Ошибка:\n" + comm.body.get("info");
			// synchronized (Data.alert) {
			// Data.alert.notifyAll();
			// }
			Data.showNotify(ru.ufalinux.tasp.R.drawable.alert_icon, Data.alert);
			break;
		default:
			Data.waiting = Types.NONE;
			Data.currState = Types.NONE;
			Data.alert = "Ошибка:\n" + comm.body.get("info");
			// synchronized (Data.alert) {
			// Data.alert.notifyAll();
			// }
			Data.showNotify(ru.ufalinux.tasp.R.drawable.alert_icon, Data.alert);
			break;
		}
	}

	public void processR_ORDERID(Command comm) {
		Data.currOrder = new Order();
		long id = Long.parseLong(comm.body.get("id"));
		Data.currOrder.id = id;
	}

}
