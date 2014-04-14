package ru.ufalinux.tasp;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.JabberConfig;
import ru.ufalinux.tasp.dataworks.MainConfig;
import ru.ufalinux.tasp.dataworks.ProcessingService;
import ru.ufalinux.tasp.dataworks.Types;
import ru.ufalinux.tasp.jabberworks.JabberListenerService;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.Toast;

public class MainActivity extends TabActivity {
	/** Called when the activity is first created. */
	public static Intent jabber;
	public static Intent processing;
	static final int ALERT_DIALOG = 0;
	TabHost thost;
	MainActivity act;
	//GlobalStopListView ListView;
	

	public void onResume() {
		super.onResume();
		Data.mainAct = this;
		Data.nManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
		if (Data.isLogged && Data.currState != Types.A_ORDER_ONDRIVE) {
			thost.setCurrentTab(1);
		}
		if (Data.isLogged && Data.currState.equals(Types.A_ORDER_ONDRIVE)) {
			thost.setCurrentTab(2);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		act = this;
		setContentView(R.layout.main);
		Log.d(Data.TAG, "=========== ЗАПУСК ПРОГРАММЫ ===============");
		//GlobalStopListView = (ListView) findViewById (R.id.stopsListView);

		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		System.err.println(prefs.getBoolean("correct", false));
		String server=prefs.getString("server", "");
		if (server.length()<4) {
			Intent jSettings = new Intent(this, JabberSettingsActivity.class);
			startActivityForResult(jSettings, 12);
		} else {
			startServices();

			// connection.disconnect();
			// System.exit(0);

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			thost = getTabHost();
			TabHost.TabSpec spec;
			Intent intent;
			thost.getTabWidget().setVisibility(View.GONE);
			// thost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);

			// View indicator1 = createTabView(thost.getContext(), "Смена");
			intent = new Intent().setClass(this, LoginActivity.class);
			spec = thost.newTabSpec("login").setIndicator("Смена").setContent(intent);
			thost.addTab(spec);
			// indicator1 = createTabView(thost.getContext(), "Заказы");
			intent = new Intent().setClass(this, OrdersActivity.class);
			spec = thost.newTabSpec("orders").setIndicator("Заказы").setContent(intent);
			thost.addTab(spec);
			
			// indicator1 = createTabView(thost.getContext(), "Таксометр");
			intent = new Intent().setClass(this, TaxometerActivity.class);
			spec = thost.newTabSpec("taxometer").setIndicator("Таксометр").setContent(intent);
			thost.addTab(spec);

			intent = new Intent().setClass(this, StatesListActivity.class);
			//spec = thost.newTabSpec("taxometer").setIndicator("Статусы").setContent(intent);
			spec = thost.newTabSpec("states").setIndicator("Статусы").setContent(intent);
			thost.addTab(spec);
			
			intent = new Intent().setClass(this, StopsListActivity.class);
			spec = thost.newTabSpec("stops").setIndicator("Стоянки").setContent(intent);
			Log.d(Data.TAG, "Добавлено меню стоянки 1");
//			spec = thost.newTabSpec("stops").setIndicator("Стоянки").setContent(intent);
			thost.addTab(spec);
			
			thost.getTabWidget().setPadding(0, 0, 0, 0);
			
			// thost.getTabWidget().getChildAt(0).getLayoutParams().height = 20;
			// thost.getTabWidget().getChildAt(1).getLayoutParams().height = 20;
			// thost.getTabWidget().getChildAt(2).getLayoutParams().height = 20;
			// neversleep();
			AlertThread alertThread = new AlertThread(alertHandler);
			alertThread.start();
			getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		//menu.
		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		TabHost thost = getTabHost();
		Intent intent;
		switch (item.getItemId()) {
		case R.id.login_menu_item:
			thost.setCurrentTab(0);
			return true;
		case R.id.orders_menu_item:
			thost.setCurrentTab(1);
			return true;
		case R.id.taxometer_menu_item:
			thost.setCurrentTab(2);
			return true;
		case R.id.states_menu_item:
			thost.setCurrentTab(3);
			return true;
		case R.id.stops_menu_item:
			Log.d(Data.TAG, "Выбрано меню Стоянки");
			//Toast.makeText(this, "Стоянки 11", Toast.LENGTH_LONG).show();
			thost.setCurrentTabByTag("stops");
			//thost.setCurrentTab(4);
			Log.d(Data.TAG, "Стоянки 12");
			return true;
		case R.id.disp_menu_item:
			if (MainConfig.jabber.server.equals("taxidil.dyndns.org")) // для такси дилижанс
			{String tel = Data.dispdiliphones[Data.dispcurrphone];	Data.dispcurrphone = Data.dispcurrphone+1;
				if (Data.dispcurrphone==Data.dispdiliphones.length) { Data.dispcurrphone=0;};
				intent = new Intent(Intent.ACTION_DIAL); 				// звоним диспетчеру
				intent.setData(Uri.parse("tel:"+tel.toString()));	startActivity(intent);}
			//MainConfig.jabber.
			return true;
		case R.id.client_menu_item:
			intent = new Intent(Intent.ACTION_DIAL); 				// звоним клиенту
			intent.setData(Uri.parse("tel:"+Data.currOrder.clientPhone.toString()));	startActivity(intent);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
			System.err.println("try to commit settings");
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(this);
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("server", data.getStringExtra("server"));
			editor.putString("user", data.getStringExtra("user"));
			editor.putString("password", data.getStringExtra("password"));
			editor.putString("disp", data.getStringExtra("disp"));
			editor.putBoolean("correct", true);
			editor.commit();
			
			System.err.println("commit settings");
			startServices();
			
			thost = getTabHost();
			TabHost.TabSpec spec;
			Intent intent;
			thost.getTabWidget().setVisibility(View.GONE);
			// thost.getTabWidget().setDividerDrawable(R.drawable.tab_divider);

			// View indicator1 = createTabView(thost.getContext(), "Смена");
			intent = new Intent().setClass(this, LoginActivity.class);
			spec = thost.newTabSpec("login").setIndicator("Смена")
					.setContent(intent);
			thost.addTab(spec);

			// indicator1 = createTabView(thost.getContext(), "Заказы");
			intent = new Intent().setClass(this, OrdersActivity.class);
			spec = thost.newTabSpec("orders").setIndicator("Заказы")
					.setContent(intent);
			thost.addTab(spec);
			// indicator1 = createTabView(thost.getContext(), "Таксометр");
			intent = new Intent().setClass(this, TaxometerActivity.class);
			spec = thost.newTabSpec("taxometer").setIndicator("Таксометр")
					.setContent(intent);
			thost.addTab(spec);
			
			intent = new Intent().setClass(this, StatesListActivity.class);
			spec = thost.newTabSpec("taxometer").setIndicator("Статусы")
					.setContent(intent);
			thost.addTab(spec);
			
			intent = new Intent().setClass(this, StopsListActivity.class);
			spec = thost.newTabSpec("stops").setIndicator("Стоянки")
					.setContent(intent);
			Log.d(Data.TAG, "Добавлено меню стоянки 2");

			thost.addTab(spec);
			
			thost.getTabWidget().setPadding(0, 0, 0, 0);
			// thost.getTabWidget().getChildAt(0).getLayoutParams().height = 20;
			// thost.getTabWidget().getChildAt(1).getLayoutParams().height = 20;
			// thost.getTabWidget().getChildAt(2).getLayoutParams().height = 20;
			// neversleep();
			AlertThread alertThread = new AlertThread(alertHandler);
			alertThread.start();
			getWindow().addFlags(LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	public void restartJabber() {
		try {
			stopService(jabber);
			System.err.println("jabber stopped");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			stopService(processing);
			System.err.println("processing stopped");
		} catch (Exception e) {
			// TODO: handle exception
		}
		try {
//			jabber = new Intent(this, JabberListenerService.class);
			startService(jabber);
			System.err.println("jabber started");
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			startService(processing);
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

	protected void startServices() {
		if (!isDataServiceRunning()) {
			processing = new Intent(this, ProcessingService.class);
			System.err.println("try to start data");
			startService(processing);
		}

		new MainConfig(PreferenceManager.getDefaultSharedPreferences(this));
		if (!isJabberServiceRunning()) {
			jabber = new Intent(this, JabberListenerService.class);
			startService(jabber);
		}
	}

	public boolean isJabberServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("ru.ufalinux.tasp.jabberworks.JabberListenerService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return true;
	}

	protected boolean isDataServiceRunning() {
		ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		for (RunningServiceInfo service : manager
				.getRunningServices(Integer.MAX_VALUE)) {
			if ("ru.ufalinux.tasp.dataworks.ProcessingService"
					.equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	final Handler alertHandler = new Handler() {
		public void handleMessage(Message msg) {
			if (msg.arg1 == ALERT_DIALOG) {
				System.out.println(Data.alert);
				// showDialog(ALERT_DIALOG);
				AlertDialog.Builder builder = new AlertDialog.Builder(act);
				builder.setMessage(Data.alert + "")
						.setCancelable(false)
						.setNegativeButton("Ok",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog,
											int id) {
										dialog.cancel();
									}
								});
				Dialog dialog = builder.create();
				Data.alert = "";
				try {
					dialog.show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	};

	private class AlertThread extends Thread {
		Handler mHandler;

		public AlertThread(Handler handler) {
			mHandler = handler;
		}

		public void run() {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
			while (true) {
				synchronized (Data.alert) {
					try {
						Data.alert.wait(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// System.err.println("alert tick");
				if (Data.alert.length() > 5) {
					Message msg = mHandler.obtainMessage();
					msg.arg1 = ALERT_DIALOG;
					mHandler.sendMessage(msg);
				}
			}
		}
	}

}