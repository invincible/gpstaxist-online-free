package org.arkhntech.taxixmppclasses;

import java.util.StringTokenizer;
import java.util.Vector;

public class TariffWorks {

	public MySQLDB mysql;
	public MainConfig cfg;
	Vector<OrderTrackInfo> trackinfo;

	public TariffWorks(MainConfig cfg, MySQLDB mysql,
			Vector<OrderTrackInfo> trackinfo) {
		this.cfg = cfg;
		this.mysql = mysql;
		this.trackinfo = trackinfo;
		// mysql.connect();
	}

	public String getTariff(long order) {
		float paysum = 0;
		int type = 0;
		// int streettoLength = 0;
		// int orderstate=0;
		mysql.prepare("select orders.paysum as paysum ,orders.roadside as roadside,"
				+ "orders.ordertype as ordertype,orders.drivershift as drivershift, length(orders.streetto)as lng,"
				+ "orders.orderstate as orderstate "
				+ " from orders where orders.num=?");
		mysql.setLong(1, order);
		if (mysql.queryPrep()) {
			mysql.next();
			StringTokenizer strtType = new StringTokenizer(
					mysql.getString("ordertype"), ", ");
			while (strtType.hasMoreTokens()) {
				int tmptype = Integer.parseInt(strtType.nextToken());
				if (tmptype > type)
					type = tmptype;
			}
			paysum = mysql.getFloat("paysum");
			// streettoLength = mysql.getInt("lng");
			// orderstate=mysql.getInt("orderstate");
		}
		String out = "";
		boolean fixed = false;
		// System.out.println("tariffworks: "+cfg.send_cost+ " "+paysum);
		if(paysum>0)
			fixed=true;
		
		if (cfg.force_tariff > 0 && type == 1 && !fixed ) {
			type = cfg.force_tariff;
			mysql.prepare("update orders set ordertype=? where num=?");
			mysql.setString(1, "" + type);
			mysql.setString(2, "" + order);
			mysql.executePrep();
		}

		System.err.println("sum: "+paysum+ " type: "+type+" fixed: "+fixed);
		if (cfg.send_cost && fixed) {
			// || (cfg.dual_cost && streettoLength > 0))&&(orderstate<4)) {
			out = "0\n0\nA_TARIF\nname:" + "фиксированный" + "|MinimalKm:" + 0
					+ "|MinimalPrice:" + paysum + "|PriceKm:" + 0
					+ "|AutoMinutesSpeed:" + cfg.autoMinutesSpeed
					+ "|AutoMinutesTime:" + cfg.autoMinutesTime + "|AutoKm:"
					+ cfg.autoKm + "|AutoKmSpeed:" + cfg.autoKmSpeed
					+ "|AutoKmTime:" + cfg.autoKmTime + "|call:" + 0
					+ "|WaitMinutesContinue:" + cfg.autoMinutesContinue;
			float priceMinute = 0;
			float waitMinutes = 0;
			if (cfg.idle_minutes_on_fixed) {

				try { // minutes

					mysql.prepare("select conditionparams from pricerules where priceclass=2 and absvalue=0 and conditionsubject=6 "
							+ " and conditiontype=2 and (ordertype like '"
							+ cfg.force_tariff
							+ "' or ordertype like ' "
							+ cfg.force_tariff
							+ ",%' or ordertype like '% "
							+ cfg.force_tariff
							+ "'"
							+ "or ordertype like '"
							+ cfg.force_tariff
							+ ",%' or ordertype='');");
					mysql.queryPrep();
					if (mysql.next()) {
						waitMinutes = mysql.getFloat("conditionparams");
					}

					mysql.prepare("select absvalue from pricerules where priceclass=2 and conditionsubject=6 and "
							+ "conditiontype=1 and (ordertype like '"
							+ cfg.force_tariff
							+ "' or ordertype like ' "
							+ cfg.force_tariff
							+ ",%' or ordertype like '% "
							+ cfg.force_tariff
							+ "'"
							+ "or ordertype like '"
							+ cfg.force_tariff
							+ ",%' or ordertype='');");
					mysql.queryPrep();
					if (mysql.next()) {
						priceMinute = mysql.getFloat("absvalue");
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			out += "|PriceMinute:" + priceMinute + "|WaitMinutes:"
					+ waitMinutes + "|AutoMinutes:" + cfg.autoMinutes;
			 System.out.println("tariff");

		} else {

			TariffInfo currTariff = new TariffInfo();
			try { // tariffname
				mysql.prepare("select name from refordertype where num=" + type);
				mysql.queryPrep();
				if (mysql.next()) {
					currTariff.name = mysql.getString("name");
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			try { // minutes

				mysql.prepare("select conditionparams from pricerules where priceclass=2 and absvalue=0 and conditionsubject=6 "
						+ " and conditiontype=2 and (ordertype like '"
						+ type
						+ "' or ordertype like ' "
						+ type
						+ ",%' or ordertype like '% "
						+ type
						+ "'"
						+ "or ordertype like '"
						+ type
						+ ",%' or ordertype='');");
				mysql.queryPrep();
				if (mysql.next()) {
					currTariff.WaitMinutes = mysql.getFloat("conditionparams");
				}

				mysql.prepare("select absvalue from pricerules where priceclass=2 and conditionsubject=6 and "
						+ "conditiontype=1 and (ordertype like '"
						+ type
						+ "' or ordertype like ' "
						+ type
						+ ",%' or ordertype like '% "
						+ type
						+ "'"
						+ "or ordertype like '"
						+ type
						+ ",%' or ordertype='');");
				mysql.queryPrep();
				if (mysql.next()) {
					currTariff.PriceMinute = mysql.getFloat("absvalue");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			try {// calls
				mysql.prepare("select absvalue from pricerules where priceclass=1 "
						+ "and (ordertype like '"
						+ type
						+ "' or ordertype like '% "
						+ type
						+ ",%' or ordertype like '% "
						+ type
						+ "'"
						+ "or ordertype like '"
						+ type
						+ ",%' or ordertype='');");
				mysql.queryPrep();
				while (mysql.next()) {
					currTariff.call += mysql.getFloat("absvalue");
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

			try {// kilometers
				mysql.prepare("select conditionparams,absvalue from pricerules where priceclass=3 and "
						+ "conditiontype=1 and conditionsubject=3 and (ordertype like '"
						+ type
						+ "' or "
						+ "ordertype like ' "
						+ type
						+ ",%' or ordertype like '% "
						+ type
						+ "'"
						+ "or ordertype like '"
						+ type
						+ ",%' or ordertype='');");
				mysql.queryPrep();
				if (mysql.next()) {
					currTariff.MinimalKM = mysql.getFloat("conditionparams");
					currTariff.PriceKm = mysql.getFloat("absvalue");
				} else {
					String sqlcomm = "select absvalue from pricerules where priceclass=3 and "
							+ "conditionsubject=0 and (ordertype like '"
							+ type
							+ "' or ordertype like ' "
							+ type
							+ ",%' or ordertype like '% "
							+ type
							+ "'"
							+ "or ordertype like '"
							+ type
							+ ",%' or ordertype='');";
					// System.out.println(sqlcomm);
					mysql.prepare(sqlcomm);
					mysql.queryPrep();
					if (mysql.next()) {
						currTariff.PriceKm = mysql.getFloat("absvalue");
					}
				}

				out += "";

			} catch (Exception e) {
				e.printStackTrace();
			}

			if (cfg.callAsMinimal) {
				currTariff.MinimalPrice = currTariff.call;
				currTariff.call = 0;
			}

			// скидка
			long dcard = 0;
			mysql.prepare("select dcard from orders where num=?");
			mysql.setLong(1, order);
			mysql.queryPrep();
			if (mysql.next()) {
				dcard = mysql.getLong("dcard");
			}

			if (dcard > 0 && cfg.dcards) {
				currTariff.name = "VIP " + dcard;
				Vector<TariffOption> options = new Vector<TariffOption>();
				mysql.prepare("select discount_det.ruleorder,discount_det.relvalue,discount_det.absvalue from dcards,discount_det "
						+ " where dcards.num=? and dcards.discountid=discount_det.discountid order by discount_det.ruleorder");
				mysql.setLong(1, dcard);
				mysql.queryPrep();
				while (mysql.next()) {
					options.add(new TariffOption(mysql.getLong("ruleorder"),
							mysql.getFloat("absvalue"), mysql
									.getFloat("relvalue")));
				}
				System.err
						.println("min:" + currTariff.MinimalPrice + " km:"
								+ currTariff.PriceKm + " min:"
								+ currTariff.PriceMinute);
				for (int i = 0; i < options.size(); i++) {
					TariffOption currOption = options.get(i);
					System.err.println("dcard:" + dcard + " abs:"
							+ currOption.abs + " rel:" + currOption.rel);
					if (currTariff.MinimalPrice > 0)
						currTariff.MinimalPrice -= (currOption.abs + currTariff.MinimalPrice
								* currOption.rel / 100);
					if (currTariff.call > 0)
						currTariff.call -= (currOption.abs + currTariff.call
								* currOption.rel / 100);
					currTariff.PriceKm -= (currTariff.PriceKm * currOption.rel / 100.0);
					currTariff.PriceMinute -= (currTariff.PriceMinute
							* currOption.rel / 100.0);
				}
			}

			out = "0\n0\nA_TARIF\nname:" + currTariff.name + "|MinimalKm:"
					+ currTariff.MinimalKM + "|MinimalPrice:"
					+ currTariff.MinimalPrice + "|PriceKm:"
					+ currTariff.PriceKm + "|PriceMinute:"
					+ currTariff.PriceMinute + "|WaitMinutes:"
					+ (currTariff.WaitMinutes - 1) + "|AutoMinutes:"
					+ cfg.autoMinutes + "|AutoMinutesSpeed:"
					+ cfg.autoMinutesSpeed + "|AutoMinutesTime:"
					+ cfg.autoMinutesTime + "|AutoKm:" + cfg.autoKm
					+ "|AutoKmSpeed:" + cfg.autoKmSpeed + "|AutoKmTime:"
					+ cfg.autoKmTime + "|call:" + currTariff.call
					+ "|WaitMinutesContinue:" + cfg.autoMinutesContinue;

		}
		if (cfg.tariff_recalc)
			out += "|recalc:1";
		else
			out += "|recalc:0";
		System.out.println(out);
		System.err.println(out);
		return out;
	}

}
