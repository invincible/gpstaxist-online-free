package org.arkhntech.distdaemon;

import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.prefs.Preferences;

import org.ini4j.Ini;
import org.ini4j.IniPreferences;

public class Data implements Serializable{

	private static final long serialVersionUID = 8978418046229079928L;
	public HashMap<String, Street> strRef;
	public float[][]dists;
	public HashMap<Long, Integer> refid;
	public int idcount;
	public int currid;
	public transient Connection psqldb;
	public transient Statement psqlstat;
	public String database_psql = "ufa";
	public String username_psql = "diligans";
	public String password_psql = "dilpass";
	public String host_psql = "192.168.149.56";
	public Integer reccount=7062;
	public Long maxid=(long) 0;

	public boolean parseIni(File iniFile) {
		try {
			Ini ini = new Ini(new FileReader(iniFile));
			Preferences prefs = new IniPreferences(ini);

			Preferences dbConfig = prefs.node("connection");
			username_psql = dbConfig.get("user", "root");
			password_psql = dbConfig.get("password", "");
			host_psql = dbConfig.get("host", "localhost");
			database_psql = dbConfig.get("dbname", "ufa");
			reccount=Integer.parseInt(dbConfig.get("reccount", "0"));
			maxid=Long.parseLong(dbConfig.get("maxid","0"));
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public boolean connectPsql() {

		Properties properties = new Properties();
		properties.setProperty("user", username_psql);
		properties.setProperty("password", password_psql);

		try {
			Class.forName("org.postgresql.Driver");
			psqldb = DriverManager.getConnection("jdbc:postgresql://"
					+ host_psql + "/" + database_psql, properties);
			DatabaseMetaData dbmd = psqldb.getMetaData();

			System.out
					.println("Connection to " + dbmd.getDatabaseProductName()
							+ " " + dbmd.getDatabaseProductVersion()
							+ " successful.\n");
			psqlstat = psqldb.createStatement();
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean fillData() {

		try {
			Class.forName("org.postgresql.Driver");
			strRef = new HashMap<String, Street>();
			psqlstat = psqldb.createStatement();

			System.out.print("extracting streets...");
			String sqlText = "select id,name,middlewp from lines;";
			System.out.println("Executing this command: " + sqlText + "\n");
			ResultSet results = psqlstat.executeQuery(sqlText);

			if (results != null) {
				while (results.next()) {
					// System.out.println("id = " + results.getInt("id")
					// + "; name = " + results.getString("name"));
					strRef.put(
							results.getString("name").toUpperCase()
									.replaceAll(",", ""),
							new Street(results.getInt("id"), results
									.getString("name"), results
									.getInt("middlewp")));
				}
			}

			Object[] strnames = strRef.keySet().toArray();

			for (int i = 0; i < strRef.size(); i++) {
				Integer strid = strRef.get(strnames[i]).id;
				// System.out.println(strid);
				results = psqlstat
						.executeQuery("select id,strnum,nearestwp from homes where street="
								+ strid + " order by num;");
				Vector<Home> tmpHomes = new Vector<Home>();
				if (results != null) {
					while (results.next()) {
						// System.out.println("id = " + results.getInt("id")
						// + "; name = " + results.getString("strnum"));
						tmpHomes.add(new Home(results.getInt("id"), results
								.getString("strnum"), results
								.getInt("nearestwp")));
					}
				}
				strRef.get(strnames[i]).homes = tmpHomes;
			}
			System.out.println("done");
			System.out.print("selecting wp count...");
//			results = sql.executeQuery("select count(distinct id2) as cnt from distances where " +
//					"((id1 in (select distinct nearestwp from homes)) or " +
//					"(id1 in (select distinct middlewp from lines)))and " +
//					"((id2 in(select distinct nearestwp from homes))or " +
//					"(id2 in (select distinct middlewp from lines)));");
//			results.next();
//			idcount = results.getInt("cnt");
			idcount=reccount;
			dists=new float[idcount][idcount];
//			results=psqlstat.executeQuery("select max(id) as mx from waypoints;");
//			results.next();
			long maxwp=maxid;//results.getLong("mx");
			System.out.println("done");

			System.out.println("extracting dists...");
			refid=new HashMap<Long, Integer>();
			for (long i = maxwp / 100; i <= maxwp; i += maxwp / 100) {
				System.out.println(i+" wp");
				results = psqlstat
						.executeQuery("select id1,id2,dist from distances where "
								+ "(id1 in (select id from usedwp) )and "
								+ "(id2 in(select id from usedwp)) "
								+ "and id1<="+i+" and id1>="+(i-maxwp/100)+";");
				while (results.next()) {
					Long id1=results.getLong("id1");
					if(id1==null)
						break;
					if(!refid.containsKey(id1)){
						refid.put(id1, currid);
						currid++;
					}
					Long id2=results.getLong("id2");
					if(id2==null)
						break;
					if(!refid.containsKey(id2)){
						refid.put(id2, currid);
						currid++;
					}
					Float dist=results.getFloat("dist");
					dists[refid.get(id1)][refid.get(id2)]=dist;
				}
				System.gc();
			}

			System.out.print("DB done...");

			System.out.println("dist done");
			results.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	public Vector<String> parseString(String in) {
		// System.out.println(in);
		StringTokenizer strt = new StringTokenizer(in, ",");
		Vector<String> out = new Vector<String>();
		while (strt.hasMoreTokens()) {
			StringTokenizer strt2 = new StringTokenizer(strt.nextToken(), "\"");
			if (strt2.hasMoreTokens())
				out.add(strt2.nextToken());
			else
				out.add(null);
		}
		return out;
	}

	public String getNum(String num){
		String out="";
		for(int i=0;i<num.length();i++){
			if(num.charAt(i)>'9'||num.charAt(i)<'0')
				break;
			out=out+num.charAt(i);
		}
		return out;
	}

	public String getDistNew(String srcStr, String srcHome, String dstStr,
			String dstHome, boolean leadNull) throws Exception{
		// System.out.println(strRef.size());
		// System.out.println(strRef.containsKey("ЛЕНИНА"));
		//long starttime = System.currentTimeMillis();
		long wpfrom = 0;
		long wpto = 0;
		// System.out.println(srcStr.toUpperCase());
		Street strfrom = null;
		Street strto = null;
		Double distAdd = 0.0;

		//System.out.println(srcHome+" "+dstHome);
		//System.out.println(srcHome.contains("+")+ " "+srcHome.indexOf("+"));
		
		try {
			if (srcStr.contains("+")) {
				String[]srclist=srcStr.split("\\+");
				//System.out.println(srclist);
				if(srclist.length>1){
					distAdd+=Float.parseFloat(srclist[1]);
					srcStr=srclist[0];
				}else{
					distAdd+=Float.parseFloat(srclist[0]);
					srcStr="";
				}
			}
			//System.out.println(distAdd);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		
		try {
			if (srcHome.contains("+")) {
				String[]srclist=srcHome.split("\\+");
				//System.out.println(srclist);
				if(srclist.length>1){
					distAdd+=Float.parseFloat(srclist[1]);
					srcHome=srclist[0];
				}else{
					distAdd+=Float.parseFloat(srclist[0]);
					srcHome="";
				}
			}
			//System.out.println(distAdd);
		} catch (Exception e) {
			//e.printStackTrace();
		}

		try {
			if (dstHome.contains("+")) {
//				int pos = dstHome.indexOf("+");
//				distAdd += Float.parseFloat(dstHome.substring(pos,
//						dstHome.length() - pos));
//				dstHome = dstHome.substring(0, pos);
				String[]dstlist=dstHome.split("\\+");
				if(dstlist.length>1){
					distAdd+=Float.parseFloat(dstlist[1]);
					dstHome=dstlist[0];
				}else{
					distAdd+=Float.parseFloat(dstlist[0]);
					dstHome="";
				}
				
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		
		try {
			if (dstStr.contains("+")) {
//				int pos = dstHome.indexOf("+");
//				distAdd += Float.parseFloat(dstHome.substring(pos,
//						dstHome.length() - pos));
//				dstHome = dstHome.substring(0, pos);
				String[]dstlist=dstStr.split("\\+");
				if(dstlist.length>1){
					distAdd+=Float.parseFloat(dstlist[1]);
					dstStr=dstlist[0];
				}else{
					distAdd+=Float.parseFloat(dstlist[0]);
					dstStr="";
				}
				
			}
		} catch (Exception e) {
			//e.printStackTrace();
		}
		
		try {
			strfrom = strRef.get(srcStr.toUpperCase());
			strto = strRef.get(dstStr.toUpperCase());
			//System.out.println("strfrom: "+strfrom.name+" strto:"+strto.name);
		} catch (Exception e) {
			e.printStackTrace();
			return "-1";
		}

		// System.out.println(strfrom + " " + strto);
		Integer homenumSrc = 0;
		try {
			homenumSrc = Integer.parseInt(getNum(srcHome));
		} catch (Exception e) {
			homenumSrc = 0;
		}
		Integer homenumDst = 0;
		try {
			homenumDst = Integer.parseInt(getNum(dstHome));
		} catch (Exception e) {
			homenumDst = 0;
		}
		//System.out.println("homesrc:"+homenumSrc+" homedst:"+homenumDst);
		try {
			Vector<Home> homesfrom = strfrom.homes;
			int size=0;
			try{
				size = strfrom.homes.size();
			}catch(Exception e){
				e.printStackTrace();
				System.err.println("strfrom homes is null!");
			}
			for (int i = 0; i < size; i++) {
				int tmpnum=Integer.parseInt(getNum(homesfrom.get(i).num));
				//System.out.println(tmpnum);
				if ( tmpnum == homenumSrc) {
					wpfrom = homesfrom.get(i).wp;
					//System.out.println("homesfrom tick hard");
					break;
				}
			}
			if (wpfrom == 0) {
				for (int i = 0; i < size; i++) {
					if (Integer.parseInt(getNum(homesfrom.get(i).num)) > homenumSrc) {
						wpfrom = homesfrom.get(i).wp;
						//System.out.println("homenum ticks soft");
						break;
					}
				}
			}
			if (wpfrom == 0) {
				wpfrom = strfrom.midPoint;
			}
		} catch (Exception e) {
			e.printStackTrace();
			homenumSrc = 0;
			wpfrom = -1;
		}

		try {

			Vector<Home> homesto = null;
			
			int size=0;
			try{
				homesto=strto.homes;
				size = strto.homes.size();
			}catch(Exception e){
				e.printStackTrace();
				System.err.println("strto hones is null!");
			}
			for (int i = 0; i < size; i++) {
				int tmpnum=Integer.parseInt(getNum(homesto.get(i).num));
				//System.out.println(tmpnum);
				if ( tmpnum == homenumDst) {
					wpto = homesto.get(i).wp;
					//System.out.println("hometo ticks hard");
					break;
				}
			}
			if (wpto == 0) {
				for (int i = 0; i < size; i++) {
					if (Integer.parseInt(getNum(homesto.get(i).num)) > homenumDst) {
						wpto = homesto.get(i).wp;
						break;
					}
				}
			}
			if (wpto == 0) {
				wpto = strto.midPoint;
			}
		} catch (Exception e) {
			e.printStackTrace();
			homenumDst = 0;
			wpto = -1;
		}
		// System.out.println(homenumSrc + " " + homenumDst);

		Double dist = -1.0;
		//long waitstart = 0;
		if (wpfrom != -1 && wpto != -1) {
			try {
				if (wpfrom > wpto) {
					long tmpwp = wpfrom;
					wpfrom = wpto;
					wpto = tmpwp;
				}
				//System.out.println("wpfrom: "+wpfrom+" wpto: "+wpto);
				//System.out.println("reqwait");
				//waitstart = System.currentTimeMillis();
				dist=(double) dists[refid.get(wpfrom)][refid.get(wpto)];
				// String commstr = "select dist from distances where id1="
				// + wpfrom + " and id2=" + wpto + ";";
				// System.out.println(commstr);
				// ResultSet res = psqlstat.executeQuery(commstr);
				// res.next();
				// dist = res.getDouble("dist");
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		//long endtime = System.currentTimeMillis();
		//System.out.println(waitstart - starttime);
		//System.out.println(endtime - starttime);
		dist += distAdd;
		//System.out.println("dist:"+dist);
		if (dist > 0 && leadNull)
			return "0" + dist.intValue();
		else
			return "" + dist.intValue();
	}

}
