package org.arkhntech.taxixmppclasses;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

public class Data extends TimerTask {

	enum types {
		A_OK, R_OPEN, R_STOPSID, A_STOPSID, R_ORDER, A_ORDER, R_DRVSTATEID, A_DRVSTATEID, A_DRVSTATE, A_INFO, R_FREERUN, R_TARIF, A_TARIF, R_CALL, A_CALL, A_COORDS, A_ERR, R_CLOSE, R_ALARM, R_PARKINGID, R_NOALARM, R_CONFIG, R_PARKING, R_SETTINGS
	}

	XMPPConnection connection;
	HashMap<String, DriverInfo> accounts = new HashMap<String, DriverInfo>();

	Vector<OrderTrackInfo> trackinfo;
	Vector<OrderTrackInfo> currorders;

	Vector<String> confUpdated = new Vector<String>();

	public Timer dbtimer;

	Calendar cal = Calendar.getInstance();

	PSQLDB psql;
	MySQLDB mysql;

	String stateFileName = "states.st";
	String accFileName = "accounts.st";
	String confFileName = "configs.st";
	int gcTTL = 100;

	MainConfig cfg;

	MapWork map;

	TariffWorks tw;

	public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
	SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);

	public Vector<String> blockedSigns = new Vector<String>();
	public Vector<String> blockedDevices = new Vector<String>();
	public Vector<String> blockedJids = new Vector<String>();

	boolean end = false;

	public boolean readStates() {
		try {
			File stateFile = new File(stateFileName);
			FileInputStream tmpStates = new FileInputStream(stateFile);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					tmpStates));
			trackinfo = new Vector<OrderTrackInfo>();
			String tmp;
			while ((tmp = in.readLine()) != null) {
				try {
					StringTokenizer strt = new StringTokenizer(tmp, "\t ");
					OrderTrackInfo tmpinfo = new OrderTrackInfo();
					tmpinfo.id = Integer.parseInt(strt.nextToken());
					tmpinfo.status = Integer.parseInt(strt.nextToken());
					tmpinfo.driver = strt.nextToken();
					tmpinfo.autoflag = Integer.parseInt(strt.nextToken());
					tmpinfo.seen = Integer.parseInt(strt.nextToken());
					tmpinfo.seenNew = Integer.parseInt(strt.nextToken());
					tmpinfo.tariff = Integer.parseInt(strt.nextToken());
					tmpinfo.computedCost = Boolean.parseBoolean(strt
							.nextToken());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			tmpStates.close();

			File accFile = new File(accFileName);
			FileInputStream tmpAcc = new FileInputStream(accFile);
			in = new BufferedReader(new InputStreamReader(tmpAcc));
			accounts = new HashMap<String, DriverInfo>();
			while ((tmp = in.readLine()) != null) {
				try {
					StringTokenizer strt = new StringTokenizer(tmp, "\t ");
					DriverInfo tmpdrv = new DriverInfo();
					tmpdrv.sign = strt.nextToken();
					tmpdrv.jid = strt.nextToken();
					accounts.put(tmpdrv.sign, tmpdrv);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			tmpAcc.close();

			File confFile = new File(confFileName);
			FileInputStream tmpConf = new FileInputStream(confFile);
			in = new BufferedReader(new InputStreamReader(tmpConf));
			while ((tmp = in.readLine()) != null) {
				try {
					confUpdated.add(tmp);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			tmpConf.close();

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean writeStates() {
		try {
			// System.out.println("write start");
			File accFile = new File(accFileName);
			FileOutputStream tmp = new FileOutputStream(accFile);
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(tmp));
			Vector<String> signs = new Vector<String>(accounts.keySet());
			for (int i = 0; i < signs.size(); i++) {
				out.write(signs.get(i) + "\t" + accounts.get(signs.get(i)).jid
						+ "\n");
			}
			out.close();
			// System.out.println("wrote accounts");
			File stateFile = new File(stateFileName);
			tmp = new FileOutputStream(stateFile);
			out = new BufferedWriter(new OutputStreamWriter(tmp));
			for (int i = 0; i < trackinfo.size(); i++) {
				OrderTrackInfo curr = trackinfo.get(i);
				out.write(curr.id + "\t" + curr.status + "\t" + curr.driver
						+ "\t" + curr.autoflag + "\t" + curr.seen + "\t"
						+ curr.seenNew + "\t" + curr.tariff + "\t"
						+ curr.computedCost + "\n");
			}
			out.close();
			// System.out.println("wrote states");

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public String getRoute(Long order) {
		String mess = "";
		mysql.prepare("select orders.route as route from orders where orders.num= ?");
		mysql.setLong(1, order);
		mysql.queryPrep();
		if (mysql.next()) {
			;
			mess = "Маршрут: ";
			String route = mysql.getString("route");
			String[] routearr = route.split("\\x0d\\x0a");
			// System.out.println(routearr);
			for (int i = 0; i < routearr.length - 1; i++) {
				String currRoute = routearr[i];
				// System.out.println(currRoute);
				String[] currRouteArr = currRoute.split("\t");
				mess += currRouteArr[0] + " " + currRouteArr[1] + " "
						+ currRouteArr[2] + " " + ";";
			}
		}
		return mess;
	}

	public Data(XMPPConnection conn) {
		Calendar.getInstance().setTimeZone(TimeZone.getTimeZone("GMT+3"));
		dbtimer = new Timer();
		dbtimer.schedule(this, 1000, 10000);

		trackinfo = new Vector<OrderTrackInfo>();

		this.connection = conn;
		cfg = new MainConfig();
		psql = new PSQLDB(cfg);
		mysql = new MySQLDB(cfg);
		map = new MapWork(psql);
	}

	public String processAErr(Command comm) {
		// for (int i = 0; i < trackinfo.size(); i++) {
		// if (trackinfo.get(i).driver.equals(comm.sign)) {
		//
		// }
		// }
		return "";
	}

	public String processROpen(Command comm) {
		try {
			System.out.println(sdf.format(Calendar.getInstance().getTime())
					+ " sign=" + comm.sign + " tried to open with car "
					+ comm.body.get("num"));
			mysql.prepare("select taxnum,channel from refdrivers where sign like ? and disabled=0");
			mysql.setString(1, comm.sign);
			if (!mysql.queryPrep()) {
				System.out.println("taxnum not found (driver " + comm.sign
						+ ")");
				return "0\n0\nA_ERR";
			}

			mysql.next();
			String tnum = mysql.getString("taxnum");
			String channel=mysql.getString("channel");
			if(channel==null){
				channel=""+cfg.channel;
			}
			if (!tnum.equals(comm.body.get("pass"))) {
				System.out.println(sdf.format(Calendar.getInstance().getTime())
						+ " incorrect password from " + comm.sign + " : "
						+ comm.body.get("pass"));
				return "0\n0\nA_ERR\ninfo:Неверный пароль";
			}

			if (!cfg.only_default_car) {
				mysql.prepare("select num from refcars where carnumber=?");
				mysql.setString(1, comm.body.get("num"));
			} else {
				mysql.prepare("select refcars.num from refcars,refdrivers where refcars.carnumber=? "
						+ "and refdrivers.car=refcars.carnumber and refdrivers.sign=?");
				mysql.setString(1, comm.body.get("num"));
				mysql.setString(2, comm.sign);
			}
			if (!mysql.queryPrep()) {
				System.out.println(sdf.format(Calendar.getInstance().getTime())
						+ " incorrect car from " + comm.sign + " : "
						+ comm.body.get("num"));
				return "0\n0\nA_ERR\ninfo:Неверный номер машины";
			}
			mysql.next();
			int carnum = mysql.getInt("num");

			Vector<String> signs = new Vector<String>(accounts.keySet());
			for (int i = 0; i < signs.size(); i++) {
				DriverInfo tmpdriver = accounts.get(signs.get(i));
				if (tmpdriver.jid.equals(comm.from)
						&& (!tmpdriver.sign.equals(comm.sign))) {
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " driver "
							+ comm.sign
							+ " opening with jid "
							+ comm.from
							+ " rejected: sign "
							+ tmpdriver.sign
							+ " already online on " + tmpdriver.jid);
					return "0\n0\nA_ERR\ninfo:Адрес уже занят другим водителем";
				}
			}
			System.out.println(cfg.minimal_balance);
			if (cfg.minimal_balance > 0) {
				if (Integer.parseInt(comm.sign) < cfg.balance_sign) {
					mysql.prepare("select balance from refdrivers where sign=?");
					mysql.setString(1, comm.sign);
					if (mysql.queryPrep()) {
						mysql.next();
						System.out.println(mysql.getInt("balance"));
						Integer balance = mysql.getInt("balance");
						if (balance < cfg.minimal_balance) {
							return "0\n0\nA_ERR\ninfo:Баланс недостаточен ("
									+ balance + ")";
						}
					}
				}
			}

			// if(comm.from.contains("navitest")){
			// return
			// "0\n0\nA_ERR\ninfo:Старая версия ТАСП-07$0\n0\nR_ANDROIDUPDATE\nurl:arkhnchul.devio.us/tasp07.apk";
			// }

			mysql.prepare("select num as cnt from drivershift where sign=? and (endtime is null) and complete=0");
			mysql.setString(1, comm.sign);
			mysql.queryPrep();
			if (!mysql.next()) {
				String sql = "insert into drivershift (driver,sign,channel,pager,car,carid,starttime,stoporder) select "
						+ "refdrivers.num,refdrivers.sign,?,refdrivers.pager,refcars.carnumber,refcars.num,now(),1 "
						+ " from refdrivers,refcars where refdrivers.sign=? and refcars.num=?";
				// System.out.println(sql);
				mysql.prepare(sql);
				mysql.setLong(1, channel);
				mysql.setString(2, comm.sign);
				mysql.setLong(3, carnum);

				mysql.executePrep();
			}
			System.out.println(comm.from);

			DriverInfo tmpdrv = new DriverInfo();
			tmpdrv.sign = comm.sign;
			tmpdrv.jid = comm.from;
			tmpdrv.version = comm.body.get("version");
			accounts.put(comm.sign, tmpdrv);
			return "0\n0\nA_OK";

		} catch (Exception e) {
			e.printStackTrace();
		}
		return "0\n0\nA_ERR";
		// return null;
	}

	public String processRClose(Command comm) {
		try {
			// System.out.println(comm.sign);
			accounts.remove(comm.sign);
			mysql.prepare("update drivershift set complete=1,endtime=now() where sign=? and complete=0 and endtime is null");
			mysql.setString(1, comm.sign);
			mysql.executePrep();
			System.out.println(sdf.format(Calendar.getInstance().getTime())
					+ " sign=" + comm.sign + " closes session");
			return "0\n0\nA_OK";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "0\n0\nA_ERR";
	}

	public String processAOrder(Command comm) {
		try {
			long order = Long.parseLong(comm.body.get("id"));
			if (comm.body.get("state").equals("CONFIRM")) {
				System.out.println(sdf.format(Calendar.getInstance().getTime())
						+ " sign=" + comm.sign + " tries to confirm order "
						+ order);
				// perform check
				mysql.prepare("select count(*) as cnt from orders where num=?");
				mysql.setLong(1, order);
				mysql.queryPrep();
				mysql.next();
				if (mysql.getLong("cnt") != 1) {
					return "0\n0\nA_ERR\ninfo:Заказ уже занят или отклонен";
				}
				// изменения вносил михаил 16.08.2012 //begin
				// проверка, что водитель не на другом заказе, почему ее раньше НЕ БЫЛО!!! луч поноса Севе 
				mysql.prepare("select count(*) as cnt from orders,drivershift where orders.drivershift=drivershift.num and sign=? and orders.num<>?");
				mysql.setString(1, comm.sign);
				mysql.setLong  (2, order);
				mysql.queryPrep();
				mysql.next();
				if (mysql.getLong("cnt") != 0) {
					return "0\n0\nA_ERR\ninfo:Вам назначен другой заказ";	
				}
				// изменения вносил михаил  // end

				try {
					mysql.prepare("select drivershift.sign as sign from orders,drivershift "
							+ "where orders.drivershift=drivershift.num and orders.num=? and orders.lock=0");
					mysql.setLong(1, order);
					if (mysql.queryPrep()) {
						mysql.next();
						String checkSign = mysql.getString("sign");
						if (!checkSign.equals(comm.sign)) {
							System.out.println(sdf.format(Calendar
									.getInstance().getTime())
									+ " sign="
									+ comm.sign
									+ " : getting order "
									+ order
									+ " rejected");
							return "0\n0\nA_ERR\ninfo:Заказ уже занят или отклонен";
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					return "0\n0\nA_ERR\ninfo:Ошибка обработки";
				}
				int roadside = 0;
				int paysum = 0;
				int type = 1;
				// int drivershift = 0;
				String time = "15 мин";
				if (comm.body.get("info") != null)
					time = comm.body.get("info") + " мин";

				time = "ПРИНЯЛ ЗАКАЗ " + time;
				mysql.prepare("update orders,refdrivers,drivershift set orders.driver=refdrivers.num, "
						+ "orders.drivershift=drivershift.num, orders.driverstart=now(),orders.orderstate=1,"
						+ "orders.mess=concat(?,orders.mess),drivershift.stopid=0,drivershift.stoporder=0 where orders.num=? "
						+ "and orders.orderstate=0 and refdrivers.sign=? and drivershift.sign=? and "
						+ "drivershift.endtime is null and	 orders.drivershift=0 and orders.lock=0");
				mysql.setString(1, time);
				mysql.setLong(2, order);
				mysql.setString(3, comm.sign);
				mysql.setString(4, comm.sign);

				int cnt = mysql.executePrep();
				if (cnt == 0) {
					mysql.prepare("update orders,drivershift set orders.orderstate=1,orders.driverstart=now(),orders.mess=concat(orders.mess,?) "
							+ "where orders.drivershift=drivershift.num and "
							+ "drivershift.sign=? and orders.orderstate=0 and orders.num=? and orders.lock=0");
					mysql.setString(1, time);
					mysql.setString(2, comm.sign);
					mysql.setLong(3, order);
					cnt = mysql.executePrep();
					if (cnt != 0)
						System.out.println(sdf.format(Calendar.getInstance()
								.getTime())
								+ " "
								+ comm.sign
								+ " already on order");
				}
				if (cnt == 0) {
					mysql.prepare("update orders,drivershift set orders.orderstate=orders.orderstate,"
							+ "orders.driverstart=now(),orders.mess=concat(orders.mess,?) where orders.drivershift="
							+ "drivershift.num and orders.num=? and drivershift.sign=? and orders.lock=0");
					mysql.setString(1, time);
					mysql.setLong(2, order);
					mysql.setString(3, comm.sign);
					cnt = mysql.executePrep();
					if (cnt != 0)
						System.out.println(sdf.format(Calendar.getInstance()
								.getTime())
								+ " sign="
								+ comm.sign
								+ " already on order with greater state");
				}

				if (cnt == 0) {
					return "0\n0\nA_ERR\ninfo:Заказ занят или обрабатывается";
				}

				// updating driverstate
				mysql.prepare("select stopid,stoporder from drivershift where complete=0 and sign=?");
				mysql.setString(1, comm.sign);
				mysql.queryPrep();
				mysql.next();
				long stopid = mysql.getLong("stopid");
				long stoporder = mysql.getLong("stoporder");
				mysql.prepare("update drivershift set stoporder=stoporder-1 where stopid=? and stoporder>?");
				mysql.setLong(1, stopid);
				mysql.setLong(2, stoporder);
				mysql.executePrep();
				mysql.prepare("update orders,drivershift set drivershift.drvstate=2,drivershift.stopid=0 where "
						+ "orders.drivershift=drivershift.num and orders.num=?");
				mysql.setLong(1, order);
				mysql.executePrep();

				// фиксированная цена, если есть
				if ((paysum > 0) && (cfg.send_cost))
					type = paysum;

				for (int i = 0; i < trackinfo.size(); i++)
					if (trackinfo.get(i).id == Integer.parseInt(comm.body
							.get("id"))) {
						trackinfo.get(i).autoflag = 1;
						if (paysum > 0)
							trackinfo.get(i).computedCost = true;
						System.out.println(sdf.format(Calendar.getInstance()
								.getTime())
								+ " order "
								+ trackinfo.get(i).id
								+ "found in trackinfo");
						return "0\n0\nA_OK" + "$" + tw.getTariff(order);
					}
				OrderTrackInfo curr = new OrderTrackInfo();
				curr.id = order;
				curr.driver = comm.sign;
				curr.status = 1;
				curr.autoflag = 1;
				curr.seenNew = 1;
				curr.tariff = type;
				if ((paysum > 0) && (cfg.send_cost))
					curr.computedCost = true;
				trackinfo.add(curr);
				int numericSign = 0;
				try {
					numericSign = Integer.parseInt(comm.sign);
				} catch (Exception e) {
					e.printStackTrace();
				}
				String outstring = "0\n0\nA_OK" + "$" + tw.getTariff(order);

				// route designating
				if (cfg.show_destination) {
					String mess = getRoute(order);
					if (mess.length() > 1)
						outstring += "$" + "0\n0\nA_INFO\ninfo:" + mess;
				}

				if (cfg.speedLimit) {
					if (type == cfg.cityout)
						roadside = 1;
					if (numericSign < 500)
						outstring += "$" + getSpeedLimit(roadside);
					else
						outstring += "$" + getSpeedLimit(1);
				}

				return outstring;

			} else if (comm.body.get("state").equals("ONDRIVE")
					|| comm.body.get("state").equals("ONDRIVESTART")) {
				if (comm.body.get("info") == null)
					comm.body.put("info", "0.0");
				String outString = "";
				if (order == -1) {
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " sign="
							+ comm.sign
							+ " ondrive on border");
					int tariff = 1;
					if (cfg.force_tariff != 0)
						tariff = cfg.force_tariff;

					mysql.prepare("insert into orders (phone,street,streetto,drivershift,paysum,ordersum,"
							+ "ordertime,orderstate,endtask,driver,ordertype) "
							+ " select 'бордюр','бордюр','бордюр',num,?,?,now(),"
							+ "4,"
							+ cfg.border_endtask
							+ ",driver,"
							+ tariff
							+ " from drivershift where sign=? "
							+ "and complete=0 order by num desc limit 1");
					mysql.setString(1, comm.body.get("price"));
					mysql.setString(2, comm.body.get("price"));
					mysql.setString(3, comm.sign);
					mysql.executePrep();

					mysql.prepare("select orders.num from orders,drivershift where "
							+ "orders.drivershift=drivershift.num and drivershift.sign=? and orders.orderstate=4 and orders.street='бордюр'");
					mysql.setString(1, comm.sign);
					mysql.queryPrep();
					mysql.next();
					order = mysql.getLong("num");
					outString = "$0\n0\nR_ORDERID\nid:" + order + "$"
							+ tw.getTariff(order);
				}
				System.out.println(sdf.format(Calendar.getInstance().getTime())
						+ " sign=" + comm.sign + " ondrive on order " + order
						+ " price " + comm.body.get("info"));
				mysql.prepare("update orders,drivershift set orders.driverload=now(),orders.orderstate=4,mess='В ПУТИ' where orders.num=? and "
						+ "orders.drivershift=drivershift.num and drivershift.sign=? and orders.orderstate!=4;");
				mysql.setLong(1, order);
				mysql.setString(2, comm.sign);
				int cnt = mysql.executePrep();

				if (cnt == 1) {
					for (int i = 0; i < trackinfo.size(); i++)
						if (trackinfo.get(i).id == order) {
							mysql.prepare("update orders,drivershift set drivershift.drvstate=2 where "
									+ "orders.drivershift=drivershift.num and orders.num=?");
							mysql.setLong(1, order);
							mysql.executePrep();
							trackinfo.get(i).autoflag = 1;
							System.out.println(sdf.format(Calendar
									.getInstance().getTime())
									+ " order "
									+ trackinfo.get(i).id
									+ " found (ondrive) in trackinfo");
							return "0\n0\nA_OK" + outString;
						}
					// if (cnt == 1)

				} else {
					mysql.prepare("update orders,drivershift set orders.orderstate=4,drivershift.drvstate=2 where orders.num=? and "
							+ "orders.drivershift=drivershift.num and drivershift.sign=?;");

					mysql.setLong(1, order);
					mysql.setString(2, comm.sign);
					cnt = mysql.executePrep();
					if (cfg.set_cost && cfg.cost_column.length() > 0) {
						mysql.prepare("update orders,drivershift set orders."
								+ cfg.cost_column
								+ "=? where orders.num=? and "
								+ "orders.drivershift=drivershift.num and drivershift.sign=? and (orders."
								+ cfg.cost_column + "<=? or " + "orders."
								+ cfg.cost_column + " is null)");
						mysql.setString(1, comm.body.get("info"));
						mysql.setLong(2, order);
						mysql.setString(3, comm.sign);
						mysql.setString(4, comm.body.get("info"));
						mysql.executePrep();
					}
					// if (cnt == 0)
					// return "0\n0\nA_ERR\ninfo:Заказ отклонен";
					return "0\n0\nA_OK" + outString;
				}
			} else if (comm.body.get("state").equals("ONPLACE")) {
				mysql.prepare("update orders,drivershift set orders.orderstate=2,orders.mess='ЖДУ КЛИЕНТА',orders.driverwait=now() "
						+ "where orders.num=? and orders.drivershift=drivershift.num and drivershift.sign=?");
				mysql.setLong(1, order);
				mysql.setString(2, comm.sign);
				int cnt = mysql.executePrep();
				String outstring = "0\n0\nA_OK";
				if (cfg.ord_info_on_wait)
					outstring += "\n$0\n0\nA_INFO\ninfo:" + getRoute(order);

				if (cnt == 1) {
					for (int i = 0; i < trackinfo.size(); i++)
						if (trackinfo.get(i).id == order) {
							trackinfo.get(i).autoflag = 1;
							trackinfo.get(i).seen = 0;
							System.out.println(sdf.format(Calendar
									.getInstance().getTime())
									+ " order "
									+ trackinfo.get(i).id
									+ " found (waiting) in trackinfo");
							return outstring;
						}
					return outstring;
				} else {
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime()) + " order " + order + " not found");
					return "0\n0\nA_ERR\ninfo:Заказ не найден в базе";
				}
			} else if (comm.body.get("state").equals("COMPLETED")) {
				System.out.println(sdf.format(Calendar.getInstance().getTime())
						+ " sign=" + comm.sign + " completes order " + order
						+ " price " + comm.body.get("price"));
				int endtask = 1;
				int cnt=0;
				mysql.prepare("select num from orders where num=? and street like '%бордюр%'");
				mysql.setLong(1, comm.body.get("id"));
				mysql.queryPrep();
				if (mysql.next()) {
					endtask = cfg.border_endtask;
				}
				if (order == -1) {
					mysql.prepare("insert into orders (phone,street,streetto,drivershift,paysum,ordersum,"
							+ "completetime,ordertime,orderstate,endtask,driver,ordertype,route) "
							+ " select 'бордюр','бордюр','бордюр',num,?,?,now(),now(),"
							+ "5,"
							+ cfg.border_endtask
							+ ",driver,"
							+ cfg.border_state
							+ ",'бордюр' from drivershift where sign=? "
							+ "and complete=0 order by num desc limit 1");
					mysql.setString(1, comm.body.get("price"));
					mysql.setString(2, comm.body.get("price"));
					mysql.setString(3, comm.sign);
					mysql.executePrep();

					mysql.prepare("select orders.num from orders,drivershift where "
							+ "orders.drivershift=drivershift.num and drivershift.sign=? and orders.orderstate=5");
					mysql.setString(1, comm.sign);
					mysql.queryPrep();
					mysql.next();
					order = mysql.getLong("num");
					endtask = 10;
					System.out.println(order);
				}

				if (cfg.set_cost && cfg.cost_column.length() > 0) {
					mysql.prepare("update orders,drivershift set orders."
							+ cfg.cost_column
							+ "=? where orders.num=? and "
							+ "orders.drivershift=drivershift.num and drivershift.sign=? and (orders."
							+ cfg.cost_column + "<=? or " + "orders."
							+ cfg.cost_column + " is null)");
					mysql.setString(1, comm.body.get("price"));
					mysql.setLong(2, order);
					mysql.setString(3, comm.sign);
					mysql.setString(4, comm.body.get("price"));
					mysql.executePrep();
					mysql.prepare("update orders set orders.orderstate=4, orders.paysum=orders."
							+ cfg.cost_column
							+ ",completetime=now(),endtask=?,oldnum=num,mess='ВЫПОЛНИЛ ЗАКАЗ' where orders.num=?");
					// mysql.setString(1, comm.body.get("price"));
					mysql.setLong(1, endtask);
					mysql.setLong(2, order);
					cnt = mysql.executePrep();
					System.out.println(cnt);
				} else {
					mysql.prepare("update orders set orders.orderstate=4, orders.paysum=?,completetime=now(),endtask=?,oldnum=num,mess='ВЫПОЛНИЛ ЗАКАЗ' where orders.num=?");
					mysql.setString(1, comm.body.get("price"));
					mysql.setLong(2, endtask);
					mysql.setLong(3, order);
					cnt = mysql.executePrep();
					System.out.println(cnt);
				}

				if (cfg.autocomplete) {
					mysql.prepare("update orders set orders.orderstate=5, orders.paysum=?,completetime=now(),endtask=?,oldnum=num,mess='ВЫПОЛНИЛ ЗАКАЗ' where orders.num=?");
					long stopid = 0;
					int stoporder = 1;
					if (cfg.compute_stops) {
						mysql.prepare("select streetto,houseto from orders where num=?");
						mysql.setLong(1, order);
						mysql.queryPrep();
						if (mysql.next()) {
							stopid = getNearestStop(
									mysql.getString("streetto"),
									mysql.getString("houseto"));
							System.out.println(stopid);

						}
						mysql.prepare("select max(stoporder)as mx from drivershift where complete=0 and stopid="
								+ stopid + " group by stopid;");
						mysql.queryPrep();
						if (mysql.next())
							stoporder = mysql.getInt("mx") + 1;
					}
					System.out.println("stopid:" + stopid);
					mysql.prepare("select drivershift,cash from orders where num="
							+ order);
					mysql.queryPrep();
					mysql.next();
					long drivershift = mysql.getLong("drivershift");
					System.out.println("drivershift=" + drivershift);
					int cash = mysql.getInt("cash");
					mysql.prepare("update drivershift set orderscount=orderscount+1, addressorders=addressorders+1,cashorders=cashorders+?"
							+ ",cashlessorders=cashlessorders+1-?,drvstate=1,statetime=now(),totalsum=totalsum+?,totalsum_paid=totalsum_paid+?"
							+ ",totalsumcash=totalsumcash+?*?,totalsumcashless=totalsumcashless+?*(1-?) where num=?");
					mysql.setLong(1, cash);
					mysql.setLong(2, cash);
					mysql.setString(3, comm.body.get("price"));
					mysql.setString(4, comm.body.get("price"));
					mysql.setString(5, comm.body.get("price"));
					mysql.setLong(6, cash);
					mysql.setString(7, comm.body.get("price"));
					mysql.setLong(8, cash);
					mysql.setLong(9, drivershift);
					cnt = mysql.executePrep();

					if (cfg.compute_stops) {
						mysql.prepare("update drivershift,refstops set "
								+ "drivershift.stopid="
								+ stopid
								+ ", drivershift.stoporder="
								+ stoporder
								+ ",drivershift.location=concat(refstops.street,', ',refstops.house)"
								+ ",drivershift.locstreet=refstops.street,drivershift.lochouse=refstops.house"
								+ ",drivershift.loctime=now(),drivershift.stopvirtual=0 "
								+ "where drivershift.num=" + drivershift
								+ " and refstops.num=" + stopid);
						mysql.executePrep();
					}
					System.out.println("cnt=" + cnt);
					String insComm = "insert into orderscomplete(`lock`,oper,disp,ordertype,orderstate,preorder,roadside,phone,town,street,house,porch,"
							+ "apart,zonefrom,meet,addressfrom,addressto,townto,streetto,houseto,zoneto,route,suburb,distance,distcity,distsuburb,client,"
							+ "clientname,prefcar,driver,drivershift,channel,ordertime,pretime,driverstart,driverwait,clientringup,driverload,completetime,"
							+ "endtask,cash,dcard,driveupprice,waitprice,puredriveprice,ordersum,paysum,discount,carddiscount,drvcharged,opercharged,"
							+ "dispcharged,info,legend_drv,legend_order,usecldiscount,cldbalance,drvbonus,msgid,waittime,waitingstart,oldnum,ordercompany,"
							+ "passenger,ordervendor,stopid,podachatime,usemanualdist,worktime,closeuserid,linecolumn,ordervendorexecute,driverfororder,"
							+ "oper2,mess,messtime,ordercounts,addballs,timeorderproc,clientphone,msgidtime,notusecommondiscount,driversetorders,"
							+ "orderwasdrivers,proctime,javamess) "
							+ "select `lock`,oper,disp,ordertype,orderstate,preorder,roadside,phone,town,street,house,porch,"
							+ "apart,zonefrom,meet,addressfrom,addressto,townto,streetto,houseto,zoneto,route,suburb,distance,distcity,distsuburb,client,"
							+ "clientname,prefcar,driver,drivershift,channel,ordertime,pretime,driverstart,driverwait,clientringup,driverload,completetime,"
							+ "endtask,cash,dcard,driveupprice,waitprice,puredriveprice,ordersum,paysum,discount,carddiscount,drvcharged,opercharged,"
							+ "dispcharged,info,legend_drv,legend_order,usecldiscount,cldbalance,drvbonus,msgid,waittime,waitingstart,oldnum,ordercompany,"
							+ "passenger,ordervendor,stopid,podachatime,usemanualdist,worktime,closeuserid,linecolumn,ordervendorexecute,driverfororder,"
							+ "oper2,mess,messtime,ordercounts,addballs,timeorderproc,clientphone,msgidtime,notusecommondiscount,driversetorders,"
							+ "orderwasdrivers,proctime,javamess "
							+ " from orders where orders.num=" + order + ";";

					mysql.prepare(insComm);
					mysql.executePrep();
					mysql.prepare("delete from orders where num=" + order + ";");
					mysql.executePrep();
					System.out.println("order " + order + " closed");
					mysql.prepare("update drivershift set drvstate=1,statetime=now() where sign=? and endtime is null");
					mysql.setString(1, comm.sign);
					mysql.executePrep();
				}
				for (int i = 0; i < trackinfo.size(); i++) {
					if (trackinfo.get(i).id - order == 0) {
						trackinfo.remove(i);
						break;
					}
				}
				if (cnt == 1)
					return "0\n0\nA_OK";
			} else if (comm.body.get("state").equals("REJECT")) {
				System.out.println(sdf.format(Calendar.getInstance().getTime())
						+ " sign=" + comm.sign + " trying to rejects order "
						+ order);
				// mmm62 2012
				String outString = "0\n0\nA_ERR\ninfo:Отказ запрещен. Заказ остается закрепленным за вами.";
				
				// отказ водителя 2012 mmm62
				String tmp =
				 "update orders,drivershift set orders.orderstate=0,"
				 + "orders.driver=null,orders.drivershift=null,mess='"
				 + comm.sign
				 + " отказался' where orders.num="
				 + order
				 + " and drivershift.sign='"
				 + comm.sign
				 + "' and drivershift.num=orders.drivershift;";
				  System.out.println(tmp);
				 //int cnt = mysqlstat.executeUpdate(tmp);
				 mysql.prepare(tmp);	 mysql.queryPrep();
				 tmp = "update drivershift set drvstate=1,statetime=now() where sign='" + comm.sign + "' and endtime is null;";
				 mysql.prepare(tmp);	 mysql.queryPrep();
				 //if (cnt == 1)
				 //return "0\n0\nA_OK";
				 // xxxxxxxxxxx mmm62
				mysql.prepare("select num from orders where num=? and driver=? and orderstate<=2"
						+ "");
				mysql.setLong(1, comm.body.get("id"));
				mysql.setString(2, comm.sign);
				mysql.queryPrep();
				if (mysql.next())
					outString = "0\n0\nA_ERR\ninfo:Отказ запрещен. Заказ остается закрепленным за вами.";
				else
					outString = "0\n0\nA_OK";
				return outString;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
		}
		return "0\n0\nA_ERR\ninfo:Ошибка обработки (возможно, заказ закрыт)";
	}

	public long getNearestStop(String str, String home) {
		long stopid = 0;
		try {
			mysql.prepare("select num,street,house from refstops;");
			mysql.queryPrep();
			float mindist = 99999999;
			while (mysql.next()) {
				float curdist = map.getDistStrings(str, home,
						mysql.getString("street"), mysql.getString("house"));
				if (curdist > 0 && curdist < mindist) {
					mindist = curdist;
					stopid = mysql.getLong("num");
				}
			}
		} catch (Exception e) {

		}
		return stopid;
	}

	public String getNum(String num) {
		String out = "";
		for (int i = 0; i < num.length(); i++) {
			if (num.charAt(i) > '9' || num.charAt(i) < '0')
				break;
			out = out + num.charAt(i);
		}
		return out;
	}

	public String processRDrvstateID(Command comm) {
		try {
			if (!cfg.disable_busy_state) {
				mysql.prepare("select num,name from refdrvstates order by num;");
			} else {
				mysql.prepare("select num,name from refdrvstates where not (num=2) order by num"
						+ ";");
			}
			mysql.queryPrep();
			String out = "0\n0\nA_DRVSTATEID\n";
			while (mysql.next())
				out += "id:" + mysql.getInt("num") + "|state:"
						+ mysql.getString("name") + "|";

			if (cfg.autoUpdate)
				if (!accounts.get(comm.sign).version.equals(cfg.currVersion)) {
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " sign="
							+ comm.sign
							+ " needs update (version "
							+ accounts.get(comm.sign).version + " )");
					out += "$0\n0\nR_UPDATE\naddr:" + cfg.updateUrl
							+ "|port:80";
					return out;
				}
			if (cfg.confUpdate)
				if (!confUpdated.contains(comm.from)) {
					confUpdated.add(comm.from);
					try {
						File confFile = new File(confFileName);
						FileWriter tmp = new FileWriter(confFile, true);
						BufferedWriter outWriter = new BufferedWriter(tmp);
						outWriter.write(comm.from + "\n");
						outWriter.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					int signInt = 0;
					try {
						signInt = Integer.parseInt(comm.sign);
					} catch (Exception e) {
						e.printStackTrace();
					}
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " sign="
							+ comm.sign
							+ " config needs update (version "
							+ accounts.get(comm.sign).version + " )");
					if (comm.from.contains("texet")) {
						out += "$0\n0\nR_UPDATE\naddr:www.ufalinux.ru/pub/gtsettings_texet.zip|port:80";
					} else {
						if (signInt < 500)
							out += "$0\n0\nR_UPDATE\naddr:" + cfg.updateConfigs
									+ "|port:80";
						else
							out += "$0\n0\nR_UPDATE\naddr:" + cfg.urlOwn
									+ "|port:80";
					}
				}
			// String currorder = checkOrder(comm.sign);
			// if (currorder != null)
			// out += "$" + currorder;
			return out;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "0\n0\nA_ERR";
	}

	public String processADrvstate(Command comm) {
		if (cfg.shift_report != 0) {
			if (comm.body.get("id").equals("" + cfg.shift_report)) {
				try {
					String out = "Краткий отчет за смену:\n";
					int cashless_cnt = 0;
					int cashless_sum = 0;
					int cash_cnt = 0;
					int cash_sum = 0;
					mysql.prepare("select count(*)as cnt,sum(orderscomplete.paysum) as sum "
							+ "from orderscomplete,drivershift where drivershift.sign=? and orderscomplete.drivershift=drivershift.num and "
							+ "drivershift.complete=0 and orderscomplete.cash=0 group by orderscomplete.cash");
					mysql.setString(1, comm.sign);
					mysql.queryPrep();
					if (mysql.next()) {
						cashless_cnt = mysql.getInt("cnt");
						cashless_sum = mysql.getInt("sum");
					}
					mysql.prepare("select count(*)as cnt,sum(orderscomplete.paysum) as sum "
							+ "from orderscomplete,drivershift where drivershift.sign=? and orderscomplete.drivershift=drivershift.num and "
							+ "drivershift.complete=0 and orderscomplete.cash=1 group by orderscomplete.cash");

					mysql.setString(1, comm.sign);
					mysql.queryPrep();
					if (mysql.next()) {
						cash_cnt = mysql.getInt("cnt");
						cash_sum = mysql.getInt("sum");
					}
					out += "Безналичных заказов: " + cashless_cnt
							+ " на сумму " + cashless_sum + "\n"
							+ "Наличных заказов: " + cash_cnt + " на сумму "
							+ cash_sum;
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " sign="
							+ comm.sign
							+ " : shift report sent");
					return "0\n0\nA_OK$0\n0\nA_INFO\ninfo:" + out;

				} catch (Exception e) {
					e.printStackTrace();
					return "0\n0\nA_ERR";
				}
			}
		}
		if (cfg.daily_report != 0) {
			if (comm.body.get("id").equals("" + cfg.daily_report)) {
				try {
					String out = "Краткий отчет за день:\n";
					int cashless_cnt = 0;
					int cashless_sum = 0;
					int cash_cnt = 0;
					int cash_sum = 0;
					mysql.prepare("select count(*)as cnt,sum(orderscomplete.paysum) as sum "
							+ "from orderscomplete,drivershift where drivershift.sign=? and orderscomplete.drivershift=drivershift.num and "
							+ "("
							+ "(year(endtime)=year(curdate()) and month(endtime)=month(curdate()) and dayofmonth(endtime)=dayofmonth(curdate()))"
							+ "or(year(starttime)=year(curdate()) and month(starttime)=month(curdate()) and dayofmonth(starttime)=dayofmonth(curdate()))"
							+ ") "
							+ "and orderscomplete.cash=0 group by orderscomplete.cash");
					mysql.setString(1, comm.sign);
					mysql.queryPrep();

					if (mysql.next()) {
						cashless_cnt = mysql.getInt("cnt");
						cashless_sum = mysql.getInt("sum");
					}

					mysql.prepare("select count(*)as cnt,sum(orderscomplete.paysum) as sum from orderscomplete,drivershift where "
							+ "drivershift.sign=? and orderscomplete.drivershift=drivershift.num and "
							+ "("
							+ "(year(endtime)=year(curdate()) and month(endtime)=month(curdate()) and dayofmonth(endtime)=dayofmonth(curdate()))"
							+ "or(year(starttime)=year(curdate()) and month(starttime)=month(curdate()) and dayofmonth(starttime)=dayofmonth(curdate()))"
							+ ") "
							+ "and orderscomplete.cash=1 group by orderscomplete.cash");
					mysql.setString(1, comm.sign);
					mysql.queryPrep();
					if (mysql.next()) {
						cash_cnt = mysql.getInt("cnt");
						cash_sum = mysql.getInt("sum");
					}
					out += "Безналичных заказов: " + cashless_cnt
							+ " на сумму " + cashless_sum + "\n"
							+ "Наличных заказов: " + cash_cnt + " на сумму "
							+ cash_sum;
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " sign="
							+ comm.sign
							+ " : daily report sent");
					return "0\n0\nA_OK$0\n0\nA_INFO\ninfo:" + out;

				} catch (Exception e) {
					e.printStackTrace();
					return "0\n0\nA_ERR";
				}
			}
		}

		if (cfg.balance_request > 0) {
			if (comm.body.get("id").equals("" + cfg.balance_request)) {
				mysql.prepare("select balance from refdrivers where sign=?");
				mysql.setString(1, comm.sign);
				mysql.queryPrep();
				if (mysql.next()) {
					int balance = mysql.getInt("balance");
					String out = "Ваш баланс составляет " + balance;
					return "0\n0\nA_OK$0\n0\nA_INFO\ninfo:" + out;
				}
			}
		}

		try {
			mysql.prepare("update drivershift set drvstate=?,statetime=now() where sign=? and endtime is null");
			mysql.setLong(1, comm.body.get("id"));
			mysql.setString(2, comm.sign);
			mysql.executePrep();
			return "0\n0\nA_OK";
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "0\n0\nA_ERR";
	}

	public String processAInfo(Command comm) {
		return "0\n0\nA_OK";
	}

	public String processRSettngs(Command comm) {
		String out = "0\n0\nA_SETTINGS\n" + cfg.config_string;
		return out;
	}

	public String processRFreerun(Command comm) {
		try {
			boolean hasOrder = false;
			mysql.prepare("select orders.route as route,orders.meet as meet,hour(orders.pretime)as hr,minute(orders.pretime) as mn,"
					+ "orders.preorder,orders.orderstate as orderstate,orders.num,orders.street,orders.house,orders.porch,"
					+ "orders.addressfrom,orders.streetto,orders.houseto,orders.addressto from orders,drivershift where "
					+ " orders.drivershift=drivershift.num and drivershift.sign=? ");
			//по просьбе казани /
			//mysql.prepare("call get_order1(?)");
			
			mysql.setString(1, comm.sign);
			if (mysql.queryPrep()) {
				hasOrder = true;
			}
			String commstr = "";
			if (!hasOrder && !cfg.only_disp) {
				commstr = "select route as route,meet as meet,hour(pretime)as hr,minute(pretime) as mn,preorder,num,street,"
						+ "house,porch,addressfrom,streetto,houseto,addressto,hour(ordertime)as ohr,minute(ordertime) as omn,round(paysum) as cena from orders where "
						+ " orderstate=0 and (preorder=0 or ((pretime-interval "
						+ cfg.pretime
						+ " minute) < now()))and not (street is null) and length(street)>3 and ordertime< (now()-interval "
						+ cfg.freerun_delay + " minute ) ";
				
				
				if (cfg.order_vendor != null) {
					commstr += " and ordervendor='" + cfg.order_vendor + "' ";
				}
				if (cfg.freerun_denied_prefix.length() > 0)
					commstr += " and not (meet like '"
							+ cfg.freerun_denied_prefix + "%')";
				 //System.out.println(commstr);
				//по просьбе казани /
				//commstr = "call get_order2("+comm.sign+","+cfg.pretime+","+cfg.freerun_delay+")";
				mysql.prepare(commstr);
				mysql.queryPrep();
			}
			String out = "";
			int cnt = mysql.rowCount();
			while (mysql.next()) {
				float xfrom = 0;
				float yfrom = 0;

				String home = mysql.getString("house");
				String mess = "";
				if (cfg.show_destination) {
					try {
						mess += "Назначение: ";
						String route = mysql.getString("route");
						String[] routearr = route.split("\\x0d\\x0a");
						// System.out.println(routearr);
						if (routearr.length > 1) {
							for (int i = 1; i < routearr.length - 1; i++) {
								String currRoute = routearr[i];
								// System.out.println(currRoute);
								String[] currRouteArr = currRoute.split("\t");
								mess += currRouteArr[0] + " " + currRouteArr[1]
										+ " " + currRouteArr[2] + " " + ";";
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

				if (cnt < cfg.orders_coords_count) {
					// System.out.println(mysql.getString("street"));
					// long curwp = -1;
					// curwp = map.getWP(mysql.getString("street"), home);
					// System.out.println("wp:"+curwp);
					// if (curwp > 0)
					{
						Point curpoint = map.getCoords(
								mysql.getString("street"), home);
						xfrom = curpoint.x;
						yfrom = curpoint.y;
					}
				}

				int preorder = mysql.getInt("preorder");
				String pretime = "99-99";
				if (preorder == 1) {
					String mn = mysql.getString("mn");
					if (mn.length() == 1)
						mn = "0" + mn;
					pretime = mysql.getString("hr") + "-" + mn;
				}

				out += "0\n0\nR_ORDER\n";
			//	out += "id:" + mysql.getInt("num") + "|" + "address:ул "
			//			+ mysql.getString("street") + ", д " + home + ", пд "
			//			+ mysql.getString("porch") + ", "
			//			+ mysql.getString("addressfrom") + ", "
			//			+ mysql.getString("meet") + " " + mess;
					out += "id:" + mysql.getInt("num") + "|"
							// добавляем время приема заказа
			                + "address:"+mysql.getString("ohr")+"-"+mysql.getString("omn")+" ул "
							+ mysql.getString("street") + ", д " + home + ", пд "
							+ mysql.getString("porch") + ", "
							+ mysql.getString("addressfrom") + ", "
							+ mysql.getString("meet") + " " + mess+" ЦЕНА "+mysql.getString("cena");
					
				if (hasOrder) {
					out += " Закреплен за " + comm.sign;
				}
				out += "|" + "xcoord:" + yfrom + "|ycoord:" + xfrom + "|"
						+ "address:ул " + mysql.getString("streetto") + ", д "
						+ mysql.getString("houseto") + ", "
						+ mysql.getString("addressto") + "|" + "xcoord:"
						+ yfrom + "|ycoord:" + xfrom + "|" + "time:" + pretime;

				out += "$";
				if (hasOrder && cfg.only_disp)
					out += "0\n0\nA_INFO\ninfo:Вам назначен заказ$";
			}

			out += "0\n0\nA_OK";
			System.out.println(sdf.format(Calendar.getInstance().getTime())
					+ " sign=" + comm.sign + " : freerun sent");
			return out;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "0\n0\nA_ERR";
	}

	public void processACoords(Command comm) {
		try {
			float xcoord = Float.parseFloat(comm.body.get("xcoord"));
			float ycoord = Float.parseFloat(comm.body.get("ycoord"));

			// if(comm.from.contains("android")){
			// float tmp=xcoord;
			// xcoord=ycoord;
			// ycoord=tmp;
			//
			// }
			System.out.println(sdf.format(Calendar.getInstance().getTime())
					+ " " + comm.sign + " : " + ycoord + ", " + xcoord + " ");

			if (xcoord * ycoord == 0f) {
				return;
			}

			String sqlcomm = "insert into driversplaces(device,sign,geom) values ('"
					+ comm.devID
					+ "','"
					+ comm.sign
					+ "',"
					+ "st_geometryfromtext('POINT("
					+ ycoord
					+ " "
					+ xcoord
					+ ")',4326));";

			psql.execute(sqlcomm);

			if (cfg.zone_tariffs) {
				for (int i = 0; i < trackinfo.size(); i++) {
					if (trackinfo.get(i).driver.equals(comm.sign)) {
						if (trackinfo.get(i).computedCost)
							return;
					}
				}
				sqlcomm = "select st_within(st_pointfromtext('POINT(" + ycoord
						+ " " + xcoord
						+ ")',4326),geom)as inner from zones where id=1;";

				boolean inner = false;
				if (psql.query(sqlcomm)) {
					inner = psql.res.getBoolean("inner");
				}
				if (inner) {

					mysql.prepare("update orders,refdrivers set orders.ordertype=javamess where "
							+ "orders.driver=refdrivers.num and refdrivers.sign=? and not(orders.javamess is null) and "
							+ "not (orders.ordertype like ?)");
					mysql.setString(1, comm.sign);
					mysql.setString(2, cfg.cityout_tariff.toString());
					mysql.executePrep();
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime()) + " sign=" + comm.sign + " in zone");
				} else {
					mysql.prepare("update orders,refdrivers set orders.javamess=orders.ordertype where "
							+ "orders.driver=refdrivers.num and refdrivers.sign=? and not (orders.ordertype like ?)");
					mysql.setString(1, comm.sign);
					mysql.setString(2, cfg.cityout_tariff.toString());
					mysql.executePrep();

					mysql.prepare("update orders,refdrivers set orders.ordertype=? where "
							+ "orders.driver=refdrivers.num and refdrivers.sign=?");
					mysql.setString(1, cfg.cityout_tariff.toString());
					mysql.setString(2, comm.sign);
					mysql.executePrep();
					System.out
							.println(sdf.format(Calendar.getInstance()
									.getTime())
									+ " sign="
									+ comm.sign
									+ " out of zone");
				}
			}

			try {
				if (cfg.zone_stops) {

					mysql.prepare("select drivershift.stopid,drivershift.drvstate,drivershift.stoporder "
							+ "from drivershift where drivershift.sign=? and drivershift.complete=0 "
							+ "");
					mysql.setString(1, comm.sign);
					String oldstop = "";
					int oldid = 0;
					int state = 1;
					int stoporder = 0;
					if (mysql.queryPrep()) {
						mysql.next();
						oldid = mysql.getInt("stopid");
						state = mysql.getInt("drvstate");
						stoporder = mysql.getInt("stoporder");
					}
					// System.out.println(comm.sign+" "+oldstop+" "+stoporder+" "+state);

					mysql.prepare("select name from refstops where num=?");
					mysql.setLong(1, oldid);
					if (mysql.queryPrep()) {
						mysql.next();
						oldstop = mysql.getString("name");
					}

					if (state < 2) {
						sqlcomm = "select name from zones where st_within(st_pointfromtext('POINT("
								+ ycoord + " " + xcoord + ")',4326),geom);";
						// System.out.println(sqlcomm);
						if (psql.query(sqlcomm)) {
							String newstop = psql.getString("name");
							// System.out.println(oldstop+" "+newstop);
							if (!(newstop.toUpperCase().equals(oldstop
									.toUpperCase()))) {
								System.out.println(comm.sign
										+ "stop changed from " + oldstop
										+ " to " + newstop);
								mysql.prepare("select num from refstops where name=?");
								mysql.setString(1, newstop);
								mysql.queryPrep();
								mysql.next();
								int newid = mysql.getInt("num");
								mysql.prepare("select max(stoporder)as mx from drivershift where complete=0 and stopid=?");
								mysql.setLong(1, newid);
								int maxorder = 0;
								if (mysql.queryPrep()) {
									mysql.next();
									maxorder = mysql.getInt("mx");
								}
								mysql.prepare("update drivershift set stopid=?,stoporder=? where complete=0 and sign=?");
								mysql.setLong(1, newid);
								mysql.setLong(2, maxorder + 1);
								mysql.setString(3, comm.sign);
								mysql.executePrep();
								mysql.prepare("update drivershift set stoporder=stoporder-1 where complete=0 and stopid=? and stoporder>?");
								mysql.setLong(1, oldid);
								mysql.setLong(2, stoporder);
								mysql.executePrep();
							}
						} else {
							mysql.prepare("update drivershift set stopid=0 where complete=0 and sign=?");
							mysql.setString(1, comm.sign);
							mysql.executePrep();
						}

					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				if (cfg.address_stops) {
					mysql.prepare("select drvstate from drivershift where sign=? and complete=0");
					mysql.setString(1, comm.sign);
					mysql.queryPrep();
					int curstate = 2;
					while (mysql.next()) {
						curstate = mysql.getInt("drvstate");
					}
					if (curstate == 1) {
						mysql.prepare("select * from refstops");
						mysql.queryPrep();
						float dist = 100000000000f;
						int num = 0;
						String curStreet = "";
						String curHome = "";
						Point cur = new Point();
						cur.x = Float.parseFloat(comm.body.get("ycoord"));
						cur.y = Float.parseFloat(comm.body.get("xcoord"));

						while (mysql.next()) {
							int tmpnum = mysql.getInt("num");
							Point curstop = map.getCoords(
									mysql.getString("street"),
									mysql.getString("house"));
							float curdist = map.getDistRadial(cur, curstop);
							// System.out.println(tmpnum+" "+dist+" "+curdist);
							if (curdist < dist) {
								dist = curdist;
								num = tmpnum;
								curStreet = mysql.getString("street");
								curHome = mysql.getString("house");
							}
						}

						mysql.prepare("select stopid,stoporder from drivershift where sign=? and complete=0");
						mysql.setString(1, comm.sign);
						mysql.queryPrep();
						int oldstop = 0;
						int oldorder = 0;
						while (mysql.next()) {
							oldstop = mysql.getInt("stopid");
							oldorder = mysql.getInt("stoporder");
						}

						if (oldstop != num) {
							System.out.println(" driver " + comm.sign
									+ " changes stopplace from " + oldstop
									+ " to " + num);
							mysql.prepare("update drivershift set stoporder=stoporder-1 where stopid=? "
									+ "and complete=0 and stoporder>?");
							mysql.setLong(1, oldstop);
							mysql.setLong(2, oldorder);
							mysql.executePrep();

							mysql.prepare("select max(stoporder) as newstop from drivershift where stopid=? and complete=0");
							mysql.setLong(1, num);
							mysql.queryPrep();
							int neworder = 0;
							while (mysql.next()) {
								neworder = mysql.getInt("newstop");
							}
							if (neworder > 0)
								neworder++;
							mysql.prepare("update drivershift set stopid=?,stoporder=?,locstreet=?,lochouse=? where sign=? and complete=0");
							mysql.setLong(1, num);
							mysql.setLong(2, neworder);
							mysql.setString(3, curStreet);
							mysql.setString(4, curHome);
							mysql.setString(5, comm.sign);
							mysql.executePrep();
						}
					}
				}
			} catch (Exception e) {

			}

			accounts.get(comm.sign).ttl = cfg.ttl;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getSpeedLimit(int type) {
		if (type == 0)
			return "0\n0\nR_OVERSPEED\nspeed1:65|speed2:80";
		else
			return "0\n0\nR_OVERSPEED\nspeed1:0|speed2:0";
	}

	public String processRAlarm(Command comm) {
		if (cfg.alarm_disp || cfg.alarm_drivers)
			try {
				processACoords(comm);
				String lon = comm.body.get("ycoord");
				String lat = comm.body.get("xcoord");

				String street = "";
				String homenum = "";

				String sqlcomm = "select homes.id as id,st_x(homes.geom) as lon,st_y(homes.geom) as lat,"
						+ "st_distance(homes.geom,st_pointfromtext('POINT ("
						+ lon
						+ " "
						+ lat
						+ ")',4326)),"
						+ "lines.name as street,homes.strnum as home from homes,lines where "
						+ "st_contains(st_buffer(st_pointfromtext('POINT ("
						+ lon
						+ " "
						+ lat
						+ ")',4326),1),homes.geom) "
						+ "and homes.street=lines.id "
						+ "order by st_distance(homes.geom,st_pointfromtext('POINT ("
						+ lon + " " + lat + ")',4326))  limit 1;";
				// System.out.println(psqlcomm);
				if (psql.query(sqlcomm)) {
					street = psql.getString("street");
					homenum = psql.getString("home");
				} else {
					sqlcomm = "select st_x(waypoints.geom) as lon,st_y(waypoints.geom) as lat,"
							+ "st_distance(waypoints.geom,st_pointfromtext('POINT ("
							+ lon
							+ " "
							+ lat
							+ ")',4326)),"
							+ "lines.name as street,'' as home from waypoints,lines where "
							+ "st_contains(st_buffer(st_pointfromtext('POINT ("
							+ lon
							+ " "
							+ lat
							+ ")',4326),1),waypoints.geom) "
							+ "and waypoints.street=lines.id "
							+ "order by st_distance(waypoints.geom,st_pointfromtext('POINT ("
							+ lon + " " + lat + ")',4326))  limit 1;";
					if (psql.query(sqlcomm)) {
						street = psql.getString("street");
						homenum = psql.getString("home");
					}
				}

				mysql.prepare("select drivershift.num as num,refcars.carnumber as carnum,refcars.model as model,"
						+ "refcars.color as color from drivershift,refcars where drivershift.sign=? "
						+ "and drivershift.car=refcars.carnumber order by num desc limit 1");
				mysql.setString(1, comm.sign);
				mysql.queryPrep();
				mysql.next();
				String drivershift = mysql.getString("num");
				String carnum = mysql.getString("carnum");
				String model = mysql.getString("model");
				String color = mysql.getString("color");
				if (cfg.alarm_disp) {
					mysql.prepare("update drivershift set statedescr='ALARM' where num="
							+ drivershift);
					mysql.executePrep();
					mysql.prepare("insert into orders (drivershift,street,house,ordertime,info,mess,meet,addressfrom,porch) values ("
							+ drivershift
							+ ",'"
							+ street
							+ "','"
							+ homenum
							+ "',now(),'ТРЕВОГА','ТРЕВОГА','ТРЕВОГА','ТРЕВОГА','ТРЕВОГА');");
					mysql.executePrep();
				}
				if (cfg.alarm_drivers)
					sendToAll("ТРЕВОГА! " + street + " " + homenum
							+ " позывной " + comm.sign + " машина " + model
							+ " " + color + " " + carnum);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return "0\n0\nA_OK";
	}

	public void sendToAll(String mess) {
		Vector<String> signs = new Vector<String>(accounts.keySet());
		for (int i = 0; i < signs.size(); i++) {
			sendInfo(signs.get(i), mess);
		}
	}

	public String processRParkingsID(Command comm) {
		String out = "0\n0\nA_PARKINGID\n";
		String sqlcomm = "select drivershift.stopid,count(drivershift.num) as cnt,"
				+ "refstops.name from drivershift,refstops where drivershift.complete=0 "
				+ "and not(drivershift.drvstate=2) and not(drivershift.stopid=0) and "
				+ "drivershift.stopid=refstops.num group by drivershift.stopid;";
		mysql.prepare(sqlcomm);
		mysql.queryPrep();
		// while (mysql.next()) {
		// out += "id:" + mysql.getString("stopid") + "|name:"
		// + mysql.getString("name") + "|countdrv:"
		// + mysql.getString("cnt") + "|";
		// }

		sqlcomm = "select num,name from refstops;";
		mysql.prepare(sqlcomm);
		mysql.queryPrep();
		HashMap<String, String> stops = new HashMap<String, String>();
		Vector<String> stopsnum = new Vector<String>();
		while (mysql.next()) {
			stops.put(mysql.getString("num"), mysql.getString("name"));
			stopsnum.add(mysql.getString("num"));
		}

		for (int i = 0; i < stopsnum.size(); i++) {
			mysql.prepare("select count(*) as cnt from drivershift where complete=0 and not(drvstate=2) and "
					+ "stopid=?");
			mysql.setLong(1, stopsnum.get(i));
			mysql.queryPrep();
			mysql.next();
			out += "id:" + stopsnum.get(i) + "|name:"
					+ stops.get(stopsnum.get(i)) + "|countdrv:"
					+ mysql.getLong("cnt") + "|";
		}
		return out;
	}

	public boolean processPacket(Packet pack) {
		Message mess = (Message) pack;
		String body = mess.getBody();
		if (!body.contains("A_COORDS"))
			System.err.println(sdf.format(Calendar.getInstance().getTime())
					+ " " + body);
		if (body.equals("bye")) {
			end = true;
			return false;
		}

		if (body.contains("massend")) {
			StringTokenizer strt = new StringTokenizer(body, "\n");
			strt.nextToken();
			String message = "";
			if (strt.hasMoreTokens()) {
				message = message + strt.nextToken() + " ";
			}
			sendToAll(message);
			return true;
		}

		String from = pack.getTo();
		StringTokenizer strtFrom = new StringTokenizer(pack.getFrom(), "/");
		String to = strtFrom.nextToken();// pack.getFrom();

		String id = "";
		String sign = "";
		String commBody = "";
		String outBody = "0\n0\n";
		String strType = "A_ERR";
		StringTokenizer strt = new StringTokenizer(body, "\n");
		try {
			id = strt.nextToken();
			sign = strt.nextToken();
			strType = strt.nextToken();
			try {
				commBody = strt.nextToken();
			} catch (Exception e) {
				// System.out.println("ERR: " + id + "; " + sign + "; " + type
				// + "; ");
			}
		} catch (Exception e) {
			System.err.println("packet parsing error");
			outBody += "A_ERR\ninfo:" + e.getMessage();
		}
		if (!commBody.contains("xcoord"))
			System.err.println(to + " (" + sign + ") > " + commBody);
		Command comm = new Command(to, id, sign, commBody);

		if ((accounts.get(comm.sign)) == null && (!strType.equals("R_OPEN"))) {
			this.connection
					.sendPacket(new SendingPacket(to, from,
							"0\n0\nA_ERR\ninfo:Вы не авторизированы в системе. Начните смену заново"));
			return true;
		}

		// System.err.println("signs:"+blockedSigns);
		// System.err.println("devices:"+blockedDevices);
		if (blockedDevices.contains(id) || blockedJids.contains(to)
				|| blockedSigns.contains(sign)) {
			this.connection.sendPacket(new SendingPacket(to, from,
					"0\n0\nA_ERR\ninfo:Ваша учетная запись заблокирована."));
			return true;
		}

		boolean dontsend = false;

		try{
		switch (types.valueOf(strType)) {
		case A_OK:
			dontsend = true;
			break;
		case R_SETTINGS:
			outBody = processRSettngs(comm);
			break;
		case R_OPEN:
			outBody = processROpen(comm);
			break;
		case R_CLOSE:
			outBody = processRClose(comm);
			break;
		case A_ORDER:
			outBody = processAOrder(comm);
			break;
		case R_DRVSTATEID:
			outBody = processRDrvstateID(comm);
			break;
		case A_DRVSTATE:
			outBody = processADrvstate(comm);
			break;
		case A_INFO:
			outBody = processAInfo(comm);
			break;
		case R_FREERUN:
			outBody = processRFreerun(comm);
			break;
		case R_ALARM:
			outBody = processRAlarm(comm);
			// outBody="0\n0\nA_OK";
			break;
		case R_TARIF:
			outBody += "R_TARIF\nname:тариф1|MinimalKm:0|MinimalPrice:50|PriceKm:10|PriceMinute:2|WaitMinutes:10"
					+ "|AutoMinutes:0|AutoMinutesSpeed:0|AutoMinutesTime:2|AutoKm:0|AutoKmSpeed:5|AutoKmTime:5";
			break;
		case R_CALL:
			outBody += "A_CALL\nname:Подача 1|price:10|name:Подача 2|price:20";
			break;
		case A_COORDS:
			processACoords(comm);
			// outBody += "A_OK";
			return true;
			// break;
		case R_CONFIG:
			outBody += processRConfig(comm);
			break;
		case R_PARKINGID:
			outBody = processRParkingsID(comm);
			break;
		default:
			outBody += "A_ERR";
			break;
		}}catch(Exception e){
			e.printStackTrace();
			outBody+="A_ERR\ninfo:пнх";
		}

		// if(type!=TYPE_R_FREERUN)
		System.err.println(sdf.format(Calendar.getInstance().getTime()) + " "
				+ comm.sign + "(" + comm.from + ") < " + outBody);
		if (!dontsend) {
			StringTokenizer strtOut = new StringTokenizer(outBody, "$");
			while (strtOut.hasMoreTokens())
				this.connection.sendPacket(new SendingPacket(to, from, strtOut
						.nextToken()));
		}
		return true;
	}

	private String processRConfig(Command comm) {
		return "A_CONFIG\n" + cfg.configBody;
	}

	public void checkConnection() {

		if (!psql.isConnected()) {
			psql.connect();
		}

		if (!mysql.isConnected()) {
			mysql.connect();
			tw = new TariffWorks(cfg, mysql, trackinfo);
		}

		try {
			if (!connection.isConnected()) {
				connection.connect();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean sendTaxstart(String sign) {
		try {
			String to = accounts.get(sign).jid;
			if (to == null)
				return false;
			this.connection.sendPacket(new SendingPacket(to, cfg.jabberAcc,
					"0\n0\nR_TAXSTART"));
			if (sendInfo(
					sign,
					"Клиенту перезвонили, пошел простой. При посадке клиента включите таксометр (кнопка \"С пассажиром\").")) {
				System.out.println(sdf.format(Calendar.getInstance().getTime())
						+ " sign=" + sign
						+ " : awaiting successfully sent to (" + to + ")");
				return true;
			}
		} catch (Exception e) {
			System.out.println(sdf.format(Calendar.getInstance().getTime())
					+ " sign=" + sign + "sending awaiting failed to (" + sign
					+ ")");
		}
		return false;
	}

	public boolean sendInfo(String sign, String mess) {
		String to = "";
		try {
			to = accounts.get(sign).jid;
			if (to == null)
				return false;
			this.connection.sendPacket(new SendingPacket(to, cfg.jabberAcc,
					"0\n0\nA_INFO\ninfo:" + mess));
		} catch (Exception e) {
			return false;
		}
		System.out.println(sdf.format(Calendar.getInstance().getTime())
				+ " sign=" + sign + " : message sent to (" + to + ") \"" + mess
				+ "\"");
		return true;
	}

	public boolean sendErr(String sign, String mess) {
		String to = "";
		try {
			to = accounts.get(sign).jid;
			if (to == null)
				return false;
			this.connection.sendPacket(new SendingPacket(to, cfg.jabberAcc,
					"0\n0\nA_ERR\ninfo:" + mess));
		} catch (Exception e) {
			return false;
		}
		System.out.println(sdf.format(Calendar.getInstance().getTime())
				+ " sign=" + sign + " : error sent to (" + to + ") \"" + mess
				+ "\"");
		return true;
	}

	public boolean sendOrder(String sign, Integer id) {
		String jid = accounts.get(sign).jid;
		if (jid == null)
			return false;
		try {
			mysql.prepare("select num,street,house,porch,addressfrom,streetto,houseto,addressto from orders where "
					+ " num=" + id);
			mysql.queryPrep();
			String out = "";
			if (mysql.next()) {
				float xfrom = 0;
				float yfrom = 0;
				String home = getNum(mysql.getString("house"));
				long curwp = -1;
				curwp = map.getWP(mysql.getString("street"), home);
				if (curwp > 0) {
					Point curpoint = map.getCoords(curwp);
					xfrom = curpoint.x;
					yfrom = curpoint.y;
				}
				out += "0\n0\nR_ORDER\n";
				out += "id:" + mysql.getInt("num") + "|" + "address:ул "
						+ mysql.getString("street") + ", д " + home + ", пд "
						+ mysql.getString("porch") + ", "
						+ mysql.getString("addressfrom") + "|" + "xcoord:"
						+ yfrom + "|ycoord:" + xfrom + "|" + "address:ул "
						+ mysql.getString("streetto") + ", д "
						+ mysql.getString("houseto") + ", "
						+ mysql.getString("addressto") + "|"
						+ "xcoord:43.643643|ycoord:43.123123|" + "time:99-99";
			}
			this.connection.sendPacket(new SendingPacket(jid, cfg.jabberAcc,
					out));
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean trackComands() {
		try {
			// System.out.println("comm tick");
			File dir = new File(cfg.tmplocation);
			FileFilter filter = new FileFilter() {
				public boolean accept(File arg0) {
					String name = arg0.getName();
					// System.out.println(name+" "+name.startsWith(".")+name.endsWith("taxicomm"));
					return ((!name.startsWith(".")) && (name
							.endsWith("taxicomm")));
				}
			};

			File[] childrens = dir.listFiles(filter);
			if (childrens != null) {
				int fnum = childrens.length;
				for (int i = 0; i < fnum; i++) {
					File curchild = childrens[i];
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " processing "
							+ curchild.getAbsolutePath());

					try {
						FileInputStream tmpStream = new FileInputStream(
								curchild);
						BufferedReader in = new BufferedReader(
								new InputStreamReader(tmpStream));
						String command = in.readLine();

						if (command.equals("massend")) {
							String mess = "";
							String messAdd = "";
							while ((messAdd = in.readLine()) != null)
								mess += messAdd + " ";
							System.out.println(mess);
							sendToAll(mess);
						}

						if (command.equals("sendto")) {
							String sign = in.readLine();
							String mess = "";
							String messAdd = "";
							while ((messAdd = in.readLine()) != null)
								mess += messAdd + " ";
							System.out.println(mess);
							sendInfo(sign, mess);
						}
						tmpStream.close();
					} catch (Exception e) {
						e.printStackTrace();
						System.out.println(sdf.format(Calendar.getInstance()
								.getTime())
								+ " commandfile "
								+ curchild.getAbsolutePath() + " incorrect");
					}
					curchild.delete();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public void trackOrders() {
		try {
			String sqlcomm = "select orders.num as id,orders.orderstate as state,"
					+ "drivershift.sign as sign, orders.ordertype as tariff, orders.paysum as cost"
					+ " from orders,drivershift where orders.drivershift=drivershift.num ;";
			// System.out.println(sqlcomm);
			mysql.prepare(sqlcomm);
			mysql.queryPrep();
			currorders = new Vector<OrderTrackInfo>();
			while (mysql.next()) {
				OrderTrackInfo tmp1 = new OrderTrackInfo();
				tmp1.id = mysql.getInt("id");
				tmp1.status = mysql.getInt("state");
				tmp1.driver = mysql.getString("sign");
				String tariffsString = mysql.getString("tariff");
				try {
					if (tariffsString != null) {
						StringTokenizer strtTariff = new StringTokenizer(
								tariffsString, ", ");
						while (strtTariff.hasMoreTokens()) {
							tmp1.tariff = Integer.parseInt(strtTariff
									.nextToken());
						}
					} else
						tmp1.tariff = 1;
				} catch (Exception e) {
					e.printStackTrace();
					tmp1.tariff = 1;
				}
				tmp1.cost = mysql.getFloat("cost");
				if (tmp1.status < 4 && tmp1.cost > 0) {
					// System.out.println(tmp1.id + " found fixed cost");
					tmp1.computedCost = true;
				}
				currorders.add(tmp1);
				// System.out.println("id=" + tmp1.id);
			}
			// System.out.println(currorders.size());

			for (int i = 0; i < currorders.size(); i++) {
				OrderTrackInfo curr = currorders.get(i);
				// boolean found = false;
				for (int j = 0; j < trackinfo.size(); j++) {
					if (curr.id - trackinfo.get(j).id == 0) {
						if (curr.status >= trackinfo.get(j).status
								&& !curr.driver.equals("")) {
							curr.autoflag = trackinfo.get(j).autoflag;
							curr.seen = trackinfo.get(j).seen;
							curr.seenNew = trackinfo.get(j).seenNew;
							if (trackinfo.get(j).computedCost)
								curr.computedCost = trackinfo.get(j).computedCost;
						}
						if ((curr.status != trackinfo.get(j).status)
								&& (trackinfo.get(j).status != 4)) {
							trackinfo.get(j).seen = 0;
							curr.seen = 0;
						}
						break;
					}
				}
			}

			for (int i = 0; i < trackinfo.size(); i++) {
				OrderTrackInfo curr = trackinfo.get(i);
				// System.out.println(curr.id + " " + curr.status + " "
				// + curr.autoflag+" "+curr.cost+" "+curr.computedCost );

				boolean found = false;
				for (int j = 0; j < currorders.size(); j++)
					if (curr.id - currorders.get(j).id == 0) {
						// System.out.println("found2: " + curr.id);
						// System.err.println(curr.id+" "+curr.status+" "+curr.autoflag+" "+curr.seen);
						if (curr.autoflag == 1) {
							if (currorders.get(j).status == 3 && curr.seen == 0) {
								System.out.println(sdf.format(Calendar
										.getInstance().getTime())
										+ " waiting:"
										+ curr.id);
								if (sendTaxstart(curr.driver)) {
									// System.out.println("awaiting sent to driver");
									currorders.get(j).seen = 1;
								} else {
									// System.out.println("driver not informed, will be retried later");
									currorders.get(j).seen = 0;
								}
							}

							// if (!zone_tariffs)

							if (currorders.get(j).tariff - curr.tariff != 0) {
								System.out.println(sdf.format(Calendar
										.getInstance().getTime())
										+ " order "
										+ curr.id
										+ " tariff changed, sending to sign="
										+ curr.driver);
								try {
									curr.tariff = currorders.get(j).tariff;
									String to = accounts.get(curr.driver).jid;
									String out = tw.getTariff(curr.id);
									System.out.println("tariff:" + curr.tariff
											+ "\n" + out);
									// System.out.println(out);
									this.connection
											.sendPacket(new SendingPacket(to,
													cfg.jabberAcc, out));
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							if (cfg.send_cost && curr.computedCost) {
								if ((curr.cost != currorders.get(j).cost)
										&& (curr.cost > 0)
										&& (currorders.get(j).cost > 0)) {
									System.out
											.println("trying to set another cost");
									String to = accounts.get(curr.driver).jid;
									String out = tw.getTariff(curr.id);
									System.err.println(out);
									this.connection
											.sendPacket(new SendingPacket(to,
													cfg.jabberAcc, out));
								}
							}

						}
						found = true;
						break;
					}

				if (curr.autoflag == 1
						&& (curr.status == 1 || curr.status == 2) && !found) {
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime()) + " order " + curr.id + " closed");
					sendErr(curr.driver, "Заказ закрыт диспетчером");
				}
			}
			// System.err.println();

		} catch (Exception e) {
			e.printStackTrace();
		}
		trackinfo = currorders;
	}

	public boolean trackDrivers() {
		try {
			Vector<String> signs = new Vector<String>(accounts.keySet());
			for (int i = 0; i < signs.size(); i++) {
				DriverInfo tmpdrv = accounts.get(signs.get(i));
				tmpdrv.ttl--;
				if (tmpdrv.ttl < 0) {
					System.out.println(sdf.format(Calendar.getInstance()
							.getTime())
							+ " "
							+ tmpdrv.sign
							+ " ("
							+ tmpdrv.jid
							+ ") removed due to inactivity");
					accounts.remove(tmpdrv.sign);
					mysql.prepare("update drivershift set complete=1,endtime=now() where complete=0 and sign='"
							+ tmpdrv.sign + "';");
					mysql.executePrep();
				}
			}
		} catch (Exception e) {

		}
		return true;
	}

	public String checkOrder(String sign) {
		String out = "";
		try {
			mysql.prepare("select orders.meet as meet,hour(orders.pretime)as hr,minute(orders.pretime) as mn,"
					+ "orders.preorder,orders.orderstate as orderstate,orders.num,orders.street,orders.house,orders.porch,"
					+ "orders.addressfrom,orders.streetto,orders.houseto,orders.addressto from orders,drivershift where "
					+ " orders.drivershift=drivershift.num and drivershift.sign=?");
			mysql.setString(1, sign);
			if (!mysql.queryPrep())
				return null;

			String pretime = "99-99";
			if (mysql.getInt("preorder") == 1) {
				pretime = mysql.getString("hr") + "-" + mysql.getString("mn");
			}
			out += "0\n0\nR_ORDER\n";
			out += "id:" + mysql.getInt("num") + "|" + "address:ул "
					+ mysql.getString("street") + ", д "
					+ mysql.getString("house") + ", пд "
					+ mysql.getString("porch") + ", "
					+ mysql.getString("addressfrom") + ", "
					+ mysql.getString("meet") + "|" + "xcoord:" + 0
					+ "|ycoord:" + 0 + "|" + "address:ул "
					+ mysql.getString("streetto") + ", д "
					+ mysql.getString("houseto") + ", "
					+ mysql.getString("addressto") + "|" + "xcoord:" + 0
					+ "|ycoord:" + 0 + "|" + "time:" + pretime;

			int orderstate = mysql.getInt("orderstate");
			out += "|orderstate:";
			switch (orderstate) {
			case 1:
				out += "CONFIRM";
				break;
			case 2:
				out += "ONPLACE";
				break;
			case 3:
				out += "ONPLACE";
			case 4:
				out += "ONDRIVE";
				break;
			}
			out += "$";
			return out;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public void run() {
		// System.out.println("check start");
		// System.out.println("check started");
		checkConnection();
		// trackBlocks();
		// System.out.println("track started");
		trackOrders();
		// System.out.println("states writes");
		writeStates();
		// System.out.println("driverstrack started");
		trackDrivers();
		// System.out.println(getTariff(1));
		trackComands();
		gcTTL--;
		if (gcTTL <= 0) {
			System.gc();
			gcTTL = 100;
		}
	}

}