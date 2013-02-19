package org.arkhntech.taxixmppclasses;


public class MapWork {
	PSQLDB psql;
	
	public MapWork(PSQLDB psql){
		this.psql=psql;
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
	
	public float getDistStrings(String srcStr, String srcHome, String dstStr,
			String dstHome) {
		float dist = 0;
		float distAdd = 0;

		try {
			if (srcStr.contains("+")) {
				String[] srclist = srcStr.split("\\+");
				if (srclist.length > 1) {
					distAdd += Float.parseFloat(srclist[1]);
					srcStr = srclist[0];
				} else {
					distAdd += Float.parseFloat(srclist[0]);
					srcStr = "";
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}

		try {
			if (srcHome.contains("+")) {
				String[] srclist = srcHome.split("\\+");
				if (srclist.length > 1) {
					distAdd += Float.parseFloat(srclist[1]);
					srcHome = srclist[0];
				} else {
					distAdd += Float.parseFloat(srclist[0]);
					srcHome = "";
				}
			}
		} catch (Exception e) {
			// e.printStackTrace();
		}

		try {
			if (dstHome.contains("+")) {
				String[] dstlist = dstHome.split("\\+");
				if (dstlist.length > 1) {
					distAdd += Float.parseFloat(dstlist[1]);
					dstHome = dstlist[0];
				} else {
					distAdd += Float.parseFloat(dstlist[0]);
					dstHome = "";
				}

			}
		} catch (Exception e) {
			// e.printStackTrace();
		}

		try {
			if (dstStr.contains("+")) {
				String[] dstlist = dstStr.split("\\+");
				if (dstlist.length > 1) {
					distAdd += Float.parseFloat(dstlist[1]);
					dstStr = dstlist[0];
				} else {
					distAdd += Float.parseFloat(dstlist[0]);
					dstStr = "";
				}

			}
		} catch (Exception e) {
			// e.printStackTrace();
		}

		long wpfrom = getWP(srcStr, srcHome);
		long wpto = getWP(dstStr, dstHome);
		dist = getDist(wpfrom, wpto);
		return dist + distAdd;
	}

	public float getDist(long wpfrom, long wpto) {
		float dist = -1;
		if (wpfrom > wpto) {
			long tmp = wpfrom;
			wpfrom = wpto;
			wpto = tmp;
		}
		String sqlcomm="select dist from distances where id1="
			+ wpfrom + " and id2=" + wpto;
		
		if(psql.query(sqlcomm))
			dist=psql.getFloat("dist");
		
		return dist;
	}

	public float getDistRadial(Point from, Point to){
		double wp1x=((double)(from.x))*0.0174532925;
		double wp1y=((double)from.y)*0.0174532925;
		double wp2x=((double)to.x)*0.0174532925;
		double wp2y=((double)to.y)*0.0174532925;
//		System.out.println(wp1x+" "+wp1y+" "+wp2x+ " "+wp2y);
		float dist=0;
		dist = (float) (Math.sin((wp1x-wp2x)/2)*Math.sin((wp1x-wp2x)/2) + 
				Math.cos(wp1x)*Math.cos(wp2x)*Math.sin((wp1y-wp2y)/2)*Math.sin((wp1y-wp2y)/2));
        dist=(float) (2*Math.asin(Math.sqrt(dist)));
        float R=6367444.6571225f;
        dist=R*dist;
		return dist;
	}
	
	public Point getCoords(long wp) {
		Point curpoint = new Point();
//		System.out.println("inbound wp:"+wp);
		try {
			String sqlcomm="select st_x(geom) as x, st_y(geom) as y from waypoints where id="
				+ wp;
//			System.out.println(sqlcomm);
			if(psql.query(sqlcomm)){
				curpoint.x = psql.getFloat("x");
				curpoint.y = psql.getFloat("y");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return curpoint;
	}

	public Point getCoords(String strSrc,String homeSrc){
		Point curpoint = new Point();
//		System.out.println("inbound wp:"+wp);
		String str = strSrc.toUpperCase();
		int home = 0;
		try {
			home = Integer.parseInt(getNum(homeSrc));
		} catch (Exception e) {
			// e.printStackTrace();
			home = 0;
		}
		
		try {
			String sqlcomm;
			if (home > 0) {
				sqlcomm="select st_x(homes.geom) as x," +
						"st_y(homes.geom) as y from homes,lines where upper(lines.name)=upper('"
					+ str
					+ "')"
					+ "and homes.street=lines.id and homes.strnum='"
					+ homeSrc + "' order by homes.num limit 1;";
//				System.out.println(sqlcomm);
				if (psql.query(sqlcomm)) {
					curpoint.x = psql.getFloat("x");
					curpoint.y=psql.getFloat("y");
				}
			}
			if (curpoint.x<= 0) {
				sqlcomm="select st_x(homes.geom) as x," +
						"st_y(homes.geom) as y from homes,lines where upper(lines.name)=upper('"
					+ str
					+ "')"
					+ "and homes.street=lines.id and homes.num>="
					+ home + " order by homes.num limit 1;";
//				System.out.println(sqlcomm);
				if (psql.query(sqlcomm)) {
					curpoint.x = psql.getFloat("x");
					curpoint.y=psql.getFloat("y");
				}
			}
			if (curpoint.x <= 0) {
				sqlcomm="select st_x(waypoints.geom)as x,st_y(waypoints.geom)as y " +
						"from lines,waypoints where upper(name)=upper('"
					+ str + "') and lines.middlewp=waypoints.id;";
				if (psql.query(sqlcomm)) {
					curpoint.x = psql.getFloat("x");
					curpoint.y=psql.getFloat("y");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		
		return curpoint;
	}
	
	public long getWP(String strSrc, String homeSrc) {
		long wp = -1;
		String str = strSrc.toUpperCase();
		int home = 0;
		try {
			home =  Integer.parseInt(getNum(homeSrc));
		} catch (Exception e) {
			// e.printStackTrace();
			home = 0;
		}
		
		try {
			String sqlcomm;
			if (home > 0) {
				sqlcomm="select homes.geom as wp from homes,lines where upper(lines.name)=upper('"
					+ str
					+ "')"
					+ "and homes.street=lines.id and homes.num>="
					+ home + " order by homes.num limit 1;";
//				System.out.println(sqlcomm);
				if (psql.query(sqlcomm)) {
					wp = psql.getLong("wp");
				}
			}
			if (wp < 0) {
				sqlcomm="select middlewp as wp from lines where upper(name)=upper('"
					+ str + "');";
				if (psql.query(sqlcomm)) {
					wp = psql.getLong("wp");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return wp;
	}

}
