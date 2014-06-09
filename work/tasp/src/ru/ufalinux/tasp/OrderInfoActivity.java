package ru.ufalinux.tasp;

import android.app.TabActivity;
import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.Order;
import ru.ufalinux.tasp.dataworks.Types;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

public class OrderInfoActivity extends Activity {

	TextView tv;
	Button bt;
	Button taxometerButton;
	
	ProgressDialog progDialog;
	ProgressThread progThread;
	static final int DIALOG_AWAITING_ONPLACE = 0;

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.orders_info_layout);
		tv = (TextView) findViewById(R.id.orderInfoText);
		if (Data.currOrder != null)
			tv.setText("Текущий заказ:\n" + Data.currOrder.addressfrom);
		else
			tv.setText("Нет активного заказа");
		bt = (Button) findViewById(R.id.orderOnPlaceButton);
		taxometerButton=(Button)findViewById(R.id.showTaxometerButton);
		
		
		taxometerButton.setOnClickListener(new OnClickListener() {
			
			public void onClick(View arg0) {
				showTaxometer();
				
			}
		});
		
		bt.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				processOnplaceClick();

			}
		});
	}

	private void showTaxometer(){
		TabHost thost=((TabActivity)this.getParent().getParent()).getTabHost();
//		System.err.println(getParent());
		thost.setCurrentTab(2);
	}
	
	private void processOnplaceClick() {
		bt.setEnabled(false);
		if(Data.currOrder==null){
			Data.currOrder=new Order();
			Data.currOrder.id=-1l;
		}
		Data.requestOnPlace(Data.currOrder.id);
		progDialog = new ProgressDialog(getParent());
		progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progDialog.setMessage("Ожидание подтверждения...");
		progThread = new ProgressThread(handler2);
		// mmm62 для 4 андроида //progThread.start();
		// mmm62 для 4 андроида //progDialog.show();
//		showDialog(DIALOG_AWAITING_ONPLACE);
//		tv.setText("Текущий заказ:\n" + Data.currOrder.addressfrom
//				+ "\n----\n" + Data.currOrder.addressto);
	}

	public void onResume() {
		super.onResume();
		if(Data.currState==Types.A_ORDER_CONFIRM)
			bt.setEnabled(true);
		tv = (TextView) findViewById(R.id.orderInfoText);
		if (Data.currOrder != null) {
			if (Data.currState == Types.A_ORDER_CONFIRM)
				tv.setText("Текущий заказ:\n" + Data.currOrder.addressfrom);
			else if (Data.currState == Types.A_ORDER_WAITING)
				tv.setText("Текущий заказ:\n" + Data.currOrder.addressfrom
						+ "\n----\n" + Data.currOrder.addressto);
		} else
			tv.setText("Нет активного заказа");
	}

	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch (id) {
		case DIALOG_AWAITING_ONPLACE:
			progDialog = new ProgressDialog(getParent());
			progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			progDialog.setMessage("Ожидание подтверждения...");
			progThread = new ProgressThread(handler2);
			// mmm62 для 4 андроида //progThread.start();
			dialog = progDialog;
			break;
		default:
			dialog = null;
		}
		return dialog;
	}

	final Handler handler2 = new Handler() {
		public void handleMessage(Message msg) {
			int total = msg.getData().getInt("ttl");
			progDialog.setProgress(total);
			if (total <= 0) {
				progDialog.dismiss();
				progThread.setState(ProgressThread.DONE);
				// mmm62 для 4 андроида //progThread.stop();
				if (Data.currState == Types.A_ORDER_WAITING) {
					tv.setText("Текущий заказ:\n" + Data.currOrder.addressfrom
							+ "\n----\n" + Data.currOrder.addressto);
				}
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
					&& (Data.waiting == Types.A_ORDER_WAITING)) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread was Interrupted");
				}
				// System.out.println("tick..."+ttl);

				Message msg = mHandler.obtainMessage();
				Bundle b = new Bundle();
				b.putInt("ttl", ttl);
				msg.setData(b);
				mHandler.sendMessage(msg);
				ttl -= delay;
				// System.out.println("tick... "+ttl);
			}
			ttl = -1;
			Message msg = mHandler.obtainMessage();
			Bundle b = new Bundle();
			b.putInt("ttl", ttl);
			msg.setData(b);
			mHandler.sendMessage(msg);
			System.out.println(ttl);
		}

		public void setState(int state) {
			mState = state;
		}
	} //

}
