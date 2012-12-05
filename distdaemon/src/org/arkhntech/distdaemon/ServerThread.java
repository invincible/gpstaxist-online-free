package org.arkhntech.distdaemon;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Vector;

public class ServerThread extends Thread {
	private Socket clientSocket = null;
	private Data pf;

	public ServerThread(Socket sock, Data dt) {
		super("DistServerThread");
		this.clientSocket = sock;
		pf = dt;
	}

	public void run() {
		try {
			long starttime = System.currentTimeMillis();
			PrintWriter out = null;

			out = new PrintWriter(clientSocket.getOutputStream(), true);
			BufferedReader in = null;

			in = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream(), "cp1251"));
			// in = new BufferedReader(new InputStreamReader(
			// clientSocket.getInputStream()));

			String inputLine;

			if ((inputLine = in.readLine()) != null) {
				String modLine = inputLine;
				if (modLine.equals("q")) {
					System.exit(0);
				}
				Vector<String> srcStreets = pf.parseString(in.readLine());
				System.out.println("" + srcStreets.size() + srcStreets);
				Vector<String> srcHomes = pf.parseString(in.readLine());
				System.out.println("" + srcHomes.size() + srcHomes);
				Vector<String> dstStreets = pf.parseString(in.readLine());
				System.out.println("" + dstStreets.size() + dstStreets);
				Vector<String> dstHomes = pf.parseString(in.readLine());
				System.out.println("" + dstHomes.size() + dstHomes);
				if (srcHomes.size() == 0)
					srcHomes.add("0");
				if (dstHomes.size() == 0)
					dstHomes.add("0");
				if (srcStreets.size() == 0)
					srcStreets.add("");
				if (dstStreets.size() == 0)
					dstStreets.add("");
				String outLine = "";
				try {
					if (modLine.equals("002")) {
						for (int i = 0; i < dstStreets.size(); i++) {
							String ret = "";
							try {
								ret = pf.getDistNew(srcStreets.get(i),
										srcHomes.get(i), dstStreets.get(i),
										dstHomes.get(i), true);
							} catch (Exception e) {
								System.err
										.println("PROCESSING ERROR, RETURNING 0 INSTEAD");
								e.printStackTrace();
								ret = "0";
							}
							outLine += "\"" + ret + "\",";
						}
						outLine = outLine.substring(0, outLine.length() - 1);

					} else {
						String ret = "";
						try {
							ret = pf.getDistNew(srcStreets.get(0),
									srcHomes.get(0), dstStreets.get(0),
									dstHomes.get(0), true);
						} catch (Exception e) {
							System.err
									.println("PROCESSING ERROR, RETURNING 0 INSTEAD");
							e.printStackTrace();
							ret = "0";
						}

						outLine = ret;
					}
					// System.out.println(outLine);
				} catch (Exception e) {
					System.err.println("PROCESSING ERROR, SENDING 0 DISTANCE");
					e.printStackTrace();
					outLine="0";
				}
				out.print(outLine);

			}

			out.close();
			in.close();
			clientSocket.close();
			// System.out.println(clientSocket.isClosed());
			long endtime = System.currentTimeMillis();
			System.out.println(endtime - starttime);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
