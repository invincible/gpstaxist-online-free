package ru.ufalinux.tasp.jabberworks;

import java.util.HashMap;
import java.util.StringTokenizer;

public class Command {

	public HashMap<String, String> body;
	public String type;

	public Command() {
		this.body = new HashMap<String, String>();
		this.type = "";
	}

	public Command(String type, String body) {
		// System.out.println(body);
		this.body = new HashMap<String, String>();
		this.type = type;
		if (type.equals("A_CONFIG")) {
			this.body.put("content", body);
		} else if(type.equals("A_INFO")){
			StringTokenizer strt = new StringTokenizer(body, "|:\n");
			try {
				strt.nextToken();
				String info="";
				while (strt.hasMoreTokens()) {
					info+=strt.nextToken();
				}
				this.body.put("info", info);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}else if (type.equals("A_DRVSTATEID")){
			StringTokenizer strt = new StringTokenizer(body, "|");
			System.err.println(body);
			System.err.println(strt.countTokens()+" tokens");
//			int i=0;
			try{
			while (strt.hasMoreTokens()) {
				System.err.println("next try");
				StringTokenizer strt2=new StringTokenizer(strt.nextToken(),":");
				strt2.nextToken();
				String key=strt2.nextToken();
				System.err.println(key);
				strt2=new StringTokenizer(strt.nextToken(),":");
				strt2.nextToken();
				String value=strt2.nextToken();
				System.err.println(value);
				//key=key+i;
				this.body.put(key, value);
//				i++;
			}
			}catch(Exception e){
				e.printStackTrace();
			}
			 
		}
			else {
			StringTokenizer strt = new StringTokenizer(body, "|\n");
//			 System.err.println(strt.countTokens()+" tokens");
			try {
				int num=2;
				while (strt.hasMoreTokens()) {
					StringTokenizer strt2=new StringTokenizer(strt.nextToken(),":");
					String key=strt2.nextToken();
					String value="";
					if(this.body.containsKey(key)){
						key=key+num;
						num++;
					}
					while(strt2.hasMoreTokens())
						value+=strt2.nextToken();
//					System.err.println("key "+key+ "value "+value);
					this.body.put(key, value);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public String toString(){
		String out=type;
		for(String key: body.keySet()){
			out+=" key: "+key+","+body.get(key)+";";
		}
		return out;
	}
	
}
