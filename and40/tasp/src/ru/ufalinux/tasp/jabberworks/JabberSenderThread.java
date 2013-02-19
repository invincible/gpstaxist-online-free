package ru.ufalinux.tasp.jabberworks;

import org.jivesoftware.smack.XMPPConnection;

import ru.ufalinux.tasp.dataworks.Data;

public class JabberSenderThread implements Runnable {

	XMPPConnection conn;

	public JabberSenderThread(XMPPConnection conn) {
		this.conn = conn;
	}

	public synchronized void run() {
		try {
			System.out.println("sender started");
			while(Data.outcoming==null){
				Thread.sleep(100);
			}
			
			while (Data.jabberLogged) {
				synchronized (Data.outcoming) {
					Data.outcoming.wait(1000);
					while (!Data.outcoming.isEmpty()) {

						Command comm = Data.outcoming.poll();
						String body = Data.devID + "\n" + Data.sign + "\n"
								+ comm.type + "\n";
						int size = comm.body.keySet().size();
						String[] args = (String[]) comm.body.keySet().toArray(
								new String[size]);
						for (int i = 0; i < size; i++) {
							body += args[i] + ":" + comm.body.get(args[i])
									+ "|";
						}
						try{
							conn.sendPacket(new SendingPacket(body));
//							Data.lastJabberAct=70;
						}catch(Exception e){
							e.printStackTrace();
							Data.jabberLogged=false;
						}
					}
				}
			}
			conn.disconnect();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
