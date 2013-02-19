package ru.ufalinux.tasp;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.Types;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TextView;

public class LoginActivity extends Activity {

	Button loginButton;
	ProgressDialog progDialog;
	ProgressThread progThread;
	EditText loginField;
	EditText carField;
	EditText passField;
	TextView loginMessageText;
	SharedPreferences prefs;

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			int total = msg.arg1;
			System.err.println("total: " + total);
			progDialog.setProgress(total);
			if (total <= 0) {
				progDialog.dismiss();
				// progThread.;
				// progThread.interrupt();
				progThread = null;
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (!Data.isLogged) {
					loginField.setEnabled(true);
					carField.setEnabled(true);
					passField.setEnabled(true);
					loginButton.setEnabled(true);

				} else {
					loginMessageText.setText("На линии");
					SharedPreferences.Editor editor = prefs.edit(); // save
																	// driver
																	// auth data
					editor.putString("sign", Data.sign);
					editor.putString("car", Data.car);
					editor.putString("signPass", passField.getText().toString());
					editor.putBoolean("authData", true);
					editor.commit();
					TabHost thost = ((TabActivity) getParent()).getTabHost();
					thost.setCurrentTab(1);
				}
			}
		}
	};

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.login_layout);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		loginButton = (Button) findViewById(R.id.loginButton);
		loginMessageText = (TextView) findViewById(R.id.loginMessageText);
		loginField = (EditText) findViewById(R.id.loginField);
		carField = (EditText) findViewById(R.id.carField);
		passField = (EditText) findViewById(R.id.passwordField);

		if (Data.isLogged) {
			loginField.setEnabled(false);
			carField.setEnabled(false);
			passField.setEnabled(false);
			loginButton.setEnabled(false);
			loginMessageText.setText("На линии");
		} else {
			if (prefs.getBoolean("authData", false)) {
				loginField.setText(prefs.getString("sign", ""));
				carField.setText(prefs.getString("car", ""));
				passField.setText(prefs.getString("signPass", ""));
				// loginClick();
			}
		}

		loginButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loginClick();
			}

		});

		Button exitButton = (Button) findViewById(R.id.exitButton);
		exitButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				Data.requestClose();
				
				try {
					Thread.sleep(3000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				Data.jabberLogged=false;
				
				try {
					stopService(MainActivity.processing);
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.exit(0);
			}
		});
		// progThread = new ProgressThread(handler);

	}

	private void loginClick() {
		Data.sign = loginField.getText().toString();
		Data.car = carField.getText().toString();
		Data.password = passField.getText().toString();
		loginField.setEnabled(false);
		carField.setEnabled(false);
		passField.setEnabled(false);
		loginButton.setEnabled(false);

		progThread = new ProgressThread(handler);
		progThread.init();
		progDialog = new ProgressDialog(this);
		progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progDialog.setMessage("Попытка начала смены...");

		Data.tryLogin();
		progThread.start();
		progDialog.show();

	}

	private class ProgressThread extends Thread {

		Handler mHandler;

		ProgressThread(Handler h) {
			mHandler = h;
		}

		int ttl;
		int delay;

		public void init() {
			ttl = 20000;
			delay = 1000;
		}

		@Override
		public void run() {
			System.err.println("prog thread started");
			while ((ttl > 0) && (!Data.isLogged)
					&& Data.waiting == Types.R_OPEN) {
				try {
					System.err.println("login ttl: " + ttl);
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					Log.e("ERROR", "Thread was Interrupted");
				}
				System.out.println(ttl + " " + Data.waiting);
				Message msg = mHandler.obtainMessage();
				msg.arg1 = ttl;
				mHandler.sendMessage(msg);
				ttl -= delay;
			}
			ttl = -1;
			Message msg = mHandler.obtainMessage();
			msg.arg1 = ttl;
			mHandler.sendMessage(msg);
			System.out.println(ttl);
			this.interrupt();

		}

	}

}
