package ru.ufalinux.tasp;

import java.util.Vector;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.MainConfig;
import ru.ufalinux.tasp.dataworks.Order;
import ru.ufalinux.tasp.dataworks.Types;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class OrderListActivity extends Activity {

	private OrderListAdapter ordersAdapter;
	UpdateThread updThread;
	ListView orderList;
	Dialog ordertimes;
	ProgressDialog progDialog;
	ProgressThread progThread;
	protected long selectedId = 0;
	
	OrderListActivity act=this;
	
	public void onResume() {
		super.onResume();
		ordersAdapter.setData(new Vector<Order>(Data.orders));
		ordersAdapter.notifyDataSetChanged();
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.orders_layout);
		orderList = (ListView) findViewById(R.id.orderListView);
		ordersAdapter = new OrderListAdapter(this);
		orderList.setAdapter(ordersAdapter);
		ordersAdapter.notifyDataSetChanged();
		orderList.setOnItemLongClickListener(new OnItemLongClickListener() {
		
			private void startMAPpath(Order currOrderZ) {
				// TODO Auto-generated method stub
				String SS ="?saddr="+
			    currOrderZ.townfrom+" "+
			    currOrderZ.streetfrom+" "+
			    currOrderZ.housefrom+"&daddr="+
			    currOrderZ.townto+ " "+
			    currOrderZ.streetto+" "+
			    currOrderZ.houseto;
				Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
							 Uri.parse("http://maps.google.com/maps"+SS));
				startActivity(intent); 
				
			}

			private void startMAPpointA(Order currOrderZ) {
				// TODO Auto-generated method stub
						String SS =""+
					    currOrderZ.townfrom+" "+
					    currOrderZ.streetfrom+" "+
					    currOrderZ.housefrom;//+"&daddr="+
					    //currOrderZ.townto+ " "+
					    //currOrderZ.streetto+" "+
					    //currOrderZ.houseto;
						Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
									 Uri.parse("geo:0,0?q="+SS));
						startActivity(intent); 
				
			}

			private void startCALL_TO_CLIENT(Order currOrderZ) {
				// TODO Auto-generated method stub
						String SS =  currOrderZ.clientPhone;
						
						//Intent intent = new Intent(android.content.Intent.ACTION_VIEW, 
						//			 Uri.parse("geo:0,0?q="+SS));
						if (MainConfig.jabber.server.equals("cp01.ufalinux.ru")) // если череповец
						 {
							Intent intent = new Intent(android.content.Intent.ACTION_DIAL, 
							Uri.parse("tel:"+Data.dispcp02phones[0].toString()));
							startActivity(intent);
						 }
						else
						 {
						    Intent intent = new Intent(android.content.Intent.ACTION_DIAL, 
							Uri.parse("tel:"+SS.toString()));
						    startActivity(intent);
						 }
			}
			
			private void startRejectOrder(Order currOrderZ) {
				// TODO Auto-generated method stub

						Data.requestReject(currOrderZ.id);
			
			}
			
			
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				selectedId = id;
				try {
					if (Data.orders.get((int) position).time.equals("99-99")) {
						System.out.println("ordertimes:" + MainConfig.orderTimes.size());
						final CharSequence[] items = new String[8];
						items[0]="5 мин";
						items[1]="10 мин";
						items[2]="15 мин";
//						for (int i = 0; i < MainConfig.orderTimes.size(); i++) {
//							items[i] = MainConfig.orderTimes.get(i) + " мин.";
//						}
						items[3] = "Отмена";
						items[4] = "Показать на карте маршрут";
						items[5] = "Показать на карте пункт A";
						items[6] = "Звонок клиенту";
						items[7] = "Отказ от заказа";
					
						AlertDialog.Builder builder = new AlertDialog.Builder(
								getParent());
						builder.setTitle("Взять заказ:");

						builder.setItems(items,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int item) {
										if (item == 7) 
										{
											for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	
													Order currOrderZ = Data.orders.get(i);
												    startRejectOrder(currOrderZ);
													break;
													}
										
										} else
										if (item == 6) 
										{
											for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	
													Order currOrderZ = Data.orders.get(i);
												    startCALL_TO_CLIENT(currOrderZ);
													break;
													}
										
										} else
										if (item == 5) 
										{
											for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	
													Order currOrderZ = Data.orders.get(i);
												    startMAPpointA(currOrderZ);
													break;
													}
										
										} else
										if (item == 4) 
										{	for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	Order currOrderZ = Data.orders.get(i);
												    startMAPpath(currOrderZ);
													break;}
																				}
										else
										if (item == 3)
											dialog.cancel();
										else {
											Data.requestOrderTake(selectedId,
													(String) items[item]);
											progDialog = new ProgressDialog(
													getParent());
											progDialog
													.setProgressStyle(ProgressDialog.STYLE_SPINNER);
											progDialog
													.setMessage("Ожидание подтверждения...");
											progThread = new ProgressThread(
													handler2);
											// mmm62 для 4 андроида // progThread.start();
											// mmm62 для 4 андроида // progDialog.show();
											dialog.dismiss();
										}
									}

									
								});
						ordertimes = builder.create();
						ordertimes.show();
					} else {
						final CharSequence[] items2 = new String[6];
						String time = "";
						for (int i = 0; i < Data.orders.size(); i++)
							if (Data.orders.get(i).id == selectedId) {
								time = Data.orders.get(i).time;
								break;
							}
						items2[0] = "к " + time;
						items2[1] = "Отмена";
						items2[2] = "Показать на карте маршрут";
						items2[3] = "Показать на карте пункт A";
						items2[4] = "Звонок клиенту";
						items2[5] = "Отказ от заказа";
						AlertDialog.Builder builder2 = new AlertDialog.Builder(
								getParent());
						builder2.setTitle("Взять заказ:");
						builder2.setItems(items2,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int item) {
										String time = "";
										for (int i = 0; i < Data.orders.size(); i++)
											if (Data.orders.get(i).id == selectedId) {
												time = Data.orders.get(i).time;
												break;
											}
										if (item == 5) 
										{
											for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	
													Order currOrderZ = Data.orders.get(i);
													startRejectOrder(currOrderZ);
													break;
													}
										
										} else
										if (item == 4) 
										{
											for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	
													Order currOrderZ = Data.orders.get(i);
												    startCALL_TO_CLIENT(currOrderZ);
													break;
													}
										
										} else
										if (item == 3) 
										{
											for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	
													Order currOrderZ = Data.orders.get(i);
												    startMAPpointA(currOrderZ);
													break;
													}
										
										} else
										if (item == 2) 
										{	for (int i = 0; i < Data.orders.size(); i++) 
												 if (Data.orders.get(i).id.equals(selectedId)) 
												    {	Order currOrderZ = Data.orders.get(i);
												    startMAPpath(currOrderZ);
													break;}
																				}
										else
										if (item == 1)
											dialog.cancel();
										else {
											Data.requestOrderTake(selectedId,
													time);
											progDialog = new ProgressDialog(
													getParent());
											progDialog
													.setProgressStyle(ProgressDialog.STYLE_SPINNER);
											progDialog
													.setMessage("Ожидание подтверждения...");
											progThread = new ProgressThread(
													handler2);
											// mmm62 для 4 андроида // progThread.start();
											// mmm62 для 4 андроида //progDialog.show();
											dialog.dismiss();
										}
									}
								});
						ordertimes = builder2.create();
						ordertimes.show();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

				return false;
			}
		});

		updThread = new UpdateThread(handler);
		updThread.start();

	}

	final Handler handler = new Handler() { // for updating list
		public void handleMessage(Message msg) {
			int update = msg.arg1;
			if (update != 0) {
				synchronized (Data.orders) {
					ordersAdapter.setData(new Vector<Order>(Data.orders));
				}
				ordersAdapter.notifyDataSetChanged();
				if(Data.orders.size()>0 && Data.ordersChanged){
					try {
						Data.ordersChanged=false;
						Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION); 
						 MediaPlayer mMediaPlayer = new MediaPlayer();
						 mMediaPlayer.setDataSource(Data.mainAct, alert);
						 final AudioManager audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
						 if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
									mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
						            mMediaPlayer.setLooping(false);
						            mMediaPlayer.prepare();
						            mMediaPlayer.start();
						  }

					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	};

	private class UpdateThread extends Thread {
		Handler mHandler;

		UpdateThread(Handler handler) {
			this.mHandler = handler;
		}

		public void run() {
			while (true) {
				synchronized (Data.orders) {
					try {
						Data.orders.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
//				System.err.println("orders changed");
				Message msg = mHandler.obtainMessage();
				msg.arg1 = 1;
				mHandler.sendMessage(msg);
			}
		}

	}

	final Handler handler2 = new Handler() { // for handling awaiting progress
												// dialog
		public void handleMessage(Message msg) {
			int total = msg.arg1;
			progDialog.setProgress(total);
			if (total <= 0) {
				progDialog.dismiss();
				progThread.setState(ProgressThread.DONE);
				progThread.stop();
				Data.waiting = Types.NONE;
				if (Data.currState == Types.A_ORDER_CONFIRM)
					Data.orders.clear();
			}
		}
	};

	private class ProgressThread extends Thread {

		// Class constants defining state of the thread
		final static int DONE = 0;
		final static int RUNNING = 1;

		Handler mHandler;
		int mState;

		ProgressThread(Handler h) {
			mHandler = h;
		}

		@Override
		public void run() {
			mState = RUNNING;
			int ttl = 20000;
			int delay = 1000;
			while ((mState == RUNNING)
					&& (Data.waiting == Types.A_ORDER_CONFIRM)) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread was Interrupted");
				}
				// System.out.println("tick..."+ttl);

				Message msg = mHandler.obtainMessage();
				msg.arg1 = ttl;
				mHandler.sendMessage(msg);
				ttl -= delay;
				// System.out.println("tick... "+ttl);
			}
			ttl = -1;
			Message msg = mHandler.obtainMessage();
			msg.arg1 = ttl;
			mHandler.sendMessage(msg);
			System.out.println(ttl);
		}

		public void setState(int state) {
			mState = state;
		}
	}
}
