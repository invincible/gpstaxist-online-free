package ru.ufalinux.tasp;

import java.util.Vector;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.Driverstate;
import ru.ufalinux.tasp.dataworks.MainConfig;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class StatesListActivity extends Activity {

	private StatesListAdapter statesAdapter;
//	UpdateThread updThread;
	ListView statesList;
	Dialog ordertimes;
	ProgressDialog progDialog;
//	ProgressThread progThread;
	protected long selectedId = 0;

	StatesListActivity act=this;
	
	public void onResume() {
		super.onResume();
		statesAdapter.setData(new Vector<Driverstate>(Data.driverstates));
		statesAdapter.notifyDataSetChanged();
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.states_layout);
		statesList = (ListView) findViewById(R.id.stateListView);
		statesAdapter = new StatesListAdapter(this);
		statesList.setAdapter(statesAdapter);
		statesAdapter.notifyDataSetChanged();
		statesList.setOnItemLongClickListener(new OnItemLongClickListener() {
			
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				selectedId = id;
				try {
					
						System.out.println("states size:" + MainConfig.orderTimes.size());
						final CharSequence[] items = new String[2];
						items[0]="Установить";
						items[1]="Отмена";
						AlertDialog.Builder builder = new AlertDialog.Builder(
								getParent());
						builder.setTitle("Статус:");

						builder.setItems(items,
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int item) {
										if (item == 1)
											dialog.cancel();
										else {
											Data.requestStatus((int)selectedId);
											dialog.dismiss();
										}
									}
								});
						ordertimes = builder.create();
						ordertimes.show();
					
				} catch (Exception e) {
					e.printStackTrace();
				}

				return false;
			}
		});

	}

//	private class UpdateThread extends Thread {
//		Handler mHandler;
//
//		UpdateThread(Handler handler) {
//			this.mHandler = handler;
//		}
//
//		public void run() {
//			while (true) {
//				synchronized (Data.orders) {
//					try {
//						Data.orders.wait(10000);
//					} catch (InterruptedException e) {
//						e.printStackTrace();
//					}
//				}
//				System.out.println("orders changed");
//				Message msg = mHandler.obtainMessage();
//				msg.arg1 = 1;
//				mHandler.sendMessage(msg);
//			}
//		}
//
//	}


//	private class ProgressThread extends Thread {
//
//		// Class constants defining state of the thread
//		final static int DONE = 0;
//		final static int RUNNING = 1;
//
//		Handler mHandler;
//		int mState;
//
//		ProgressThread(Handler h) {
//			mHandler = h;
//		}
//
//		@Override
//		public void run() {
//			mState = RUNNING;
//			int ttl = 20000;
//			int delay = 1000;
//			while ((mState == RUNNING)
//					&& (Data.waiting == Types.A_ORDER_CONFIRM)) {
//				try {
//					Thread.sleep(delay);
//				} catch (InterruptedException e) {
//					Log.e("ERROR", "Thread was Interrupted");
//				}
//				// System.out.println("tick..."+ttl);
//
//				Message msg = mHandler.obtainMessage();
//				msg.arg1 = ttl;
//				mHandler.sendMessage(msg);
//				ttl -= delay;
//				// System.out.println("tick... "+ttl);
//			}
//			ttl = -1;
//			Message msg = mHandler.obtainMessage();
//			msg.arg1 = ttl;
//			mHandler.sendMessage(msg);
//			System.out.println(ttl);
//		}
//
//		public void setState(int state) {
//			mState = state;
//		}
//	}
}
