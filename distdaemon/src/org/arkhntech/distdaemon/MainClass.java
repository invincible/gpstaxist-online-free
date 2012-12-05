package org.arkhntech.distdaemon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;

public class MainClass {

	public static void main(String[] args) {
		ServerSocket serverSocket = null;
		Data d=new Data();
		boolean preinit = false;
		try {
			FileInputStream fin = new FileInputStream("preinit_data.dat");
			try {
				ObjectInputStream oin = new ObjectInputStream(fin);
				System.out.println("preinitialized data found, now loading");
				d = (Data) oin.readObject();
				System.out.println(d.idcount);
				oin.close();
				preinit=true;
			} catch (Exception e) {
				preinit = false;
				System.out
						.println("preinitialized data read failed, fetching from db");
				e.printStackTrace();
			}
			try {
				fin.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		} catch (FileNotFoundException e1) {
			preinit = false;
			System.out
					.println("preinitialized data not found, fetching from db");
			e1.printStackTrace();
		}
		
		if (!preinit) {
			d=new Data();
			if (!d.parseIni(new File("config.ini"))) {
				System.err.println("config.ini couldnt be parsed");
				System.exit(1);
			}
			if (!d.connectPsql()) {
				System.err.println("coulnt connect to psql");
				System.exit(1);
			}
			d.fillData();
			System.out.println("writing data");
			FileOutputStream fos;
			try {
				fos = new FileOutputStream("preinit_data.dat");
				ObjectOutputStream oos = new ObjectOutputStream(fos);
				  oos.writeObject(d);
				  oos.flush();
				  oos.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	  
		}
		System.out.println("data filled, opening socket");

		try {
			serverSocket = new ServerSocket(33401);
		} catch (IOException e) {
			System.err.println("Could not listen on port: 33401.");
			System.exit(1);
		}
		System.out.println("now listening on TCP 33401");
		boolean stop = false;
		while (!stop) {
			try {
				new ServerThread(serverSocket.accept(), d).start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			serverSocket.close();
		} catch (IOException e) {
			System.err.println("Couldnt stop server");
			e.printStackTrace();
		}
		System.out.println("Exiting");

	}

}
