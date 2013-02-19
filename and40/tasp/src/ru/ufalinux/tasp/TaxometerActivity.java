package ru.ufalinux.tasp;

import java.text.DecimalFormat;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.MainConfig;
import ru.ufalinux.tasp.dataworks.Order;
import ru.ufalinux.tasp.dataworks.Tariff;
import ru.ufalinux.tasp.dataworks.Types;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TabHost;
import android.widget.TextView;

public class TaxometerActivity extends Activity {

	TextView firmNameLabel;
	TextView tariffNameLabel;
	TextView callPriceLabel;
	TextView minimalPriceLabel;
	TextView kmPriceLabel;
	TextView minutePriceLabel;
	TextView totalKmLabel;
	TextView totalMinutesLabel;
	TextView totalCostLabel;
	Button menuButton;
	Button orderlistButton;

	ProgressDialog progDialog;
	ProgressThread progThread;

	public void onResume() {
		super.onResume();
		firmNameLabel = (TextView) findViewById(R.id.firmNameLabel);
		tariffNameLabel = (TextView) findViewById(R.id.tariffNameLabel);
		callPriceLabel = (TextView) findViewById(R.id.callPriceLabel);
		minimalPriceLabel = (TextView) findViewById(R.id.minimalPriceLabel);
		kmPriceLabel = (TextView) findViewById(R.id.kmPriceLabel);
		minutePriceLabel = (TextView) findViewById(R.id.minutePriceLabel);
		totalKmLabel = (TextView) findViewById(R.id.totalKmLabel);
		totalMinutesLabel = (TextView) findViewById(R.id.totalMinutesLabel);
		totalCostLabel = (TextView) findViewById(R.id.totalCostLabel);
		Typeface face;
		face = Typeface.createFromAsset(getAssets(), "lcd.ttf");
		totalCostLabel.setTypeface(face);
		menuButton = (Button) findViewById(R.id.taxometerMenuButton);
		orderlistButton = (Button) findViewById(R.id.showOrdersButton);
		orderlistButton.setText("Действия");
		orderlistButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				showActions();

			}
		});
		updateView();
	}

	private void showActions() {
		Dialog actions;
		AlertDialog.Builder builder = new AlertDialog.Builder(getParent());
		final CharSequence[] items = new String[5];
		items[0] = "Тариф";
		items[1] = "Доплата";
		items[2] = "Пауза";
		items[3] = "Список заказов";
		items[4] = "Отмена";
		builder.setTitle("Действия:");
		builder.setItems(items, new DialogInterface.OnClickListener() {

			private Dialog createTariffsDialog() {
				Dialog tariffsDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getParent());
				final CharSequence[] items = new String[MainConfig.tariffs
						.size() + 1];
				for (int i = 0; i < MainConfig.tariffs.size(); i++) {
					items[i] = MainConfig.tariffs.get(i).name;
				}
				items[MainConfig.tariffs.size()] = "Отмена";
				builder.setTitle("Тариф");
				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (which == MainConfig.tariffs.size()) {
							dialog.cancel();
						} else {
							Data.changeTariff(MainConfig.tariffs.get(which));
							dialog.cancel();
						}
					}
				});
				tariffsDialog = builder.create();
				return tariffsDialog;
			}

			private Dialog createCallsDialog() {
				Dialog callsDialog;
				AlertDialog.Builder builder = new AlertDialog.Builder(
						getParent());
				final CharSequence[] items = new String[MainConfig.calls.size() + 1];
				for (int i = 0; i < MainConfig.calls.size(); i++) {
					items[i] = MainConfig.calls.get(i).name;
				}
				items[MainConfig.calls.size()] = "Отмена";
				builder.setTitle("Доплата");
				builder.setItems(items, new DialogInterface.OnClickListener() {

					public void onClick(DialogInterface dialog, int which) {
						if (which == MainConfig.calls.size()) {
							dialog.cancel();
						} else {
							Data.addCall(MainConfig.calls.get(which));
							dialog.cancel();
						}
					}
				});
				callsDialog = builder.create();
				return callsDialog;
			}

			public void onClick(DialogInterface dialog, int item) {
				switch (item) {
				case 0:
					createTariffsDialog().show();
					break;
				case 1:
					createCallsDialog().show();
					break;
				case 2:
					Data.currState=Types.A_ORDER_PAUSED;
					break;
				case 3:
					showOrderlist();
					break;
				case 4:
					dialog.cancel();
					break;
				default:
					dialog.cancel();
				}
			}
		});
		actions = builder.create();
		actions.show();
	}

	private void showOrderlist() {
		TabHost thost = ((TabActivity) this.getParent()).getTabHost();
		System.err.println(getParent());
		thost.setCurrentTab(1);
	}

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.taxometer_layout);
		menuButton = (Button) findViewById(R.id.taxometerMenuButton);
		firmNameLabel = (TextView) findViewById(R.id.firmNameLabel);
		tariffNameLabel = (TextView) findViewById(R.id.tariffNameLabel);
		callPriceLabel = (TextView) findViewById(R.id.callPriceLabel);
		minimalPriceLabel = (TextView) findViewById(R.id.minimalPriceLabel);
		kmPriceLabel = (TextView) findViewById(R.id.kmPriceLabel);
		minutePriceLabel = (TextView) findViewById(R.id.minutePriceLabel);
		totalKmLabel = (TextView) findViewById(R.id.totalKmLabel);
		totalMinutesLabel = (TextView) findViewById(R.id.totalMinutesLabel);
		totalCostLabel = (TextView) findViewById(R.id.totalCostLabel);
		menuButton.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				buttonClick();

			}
		});

		UpdateThread updateThread = new UpdateThread(handler);
		updateThread.start();
	}

	protected void updateView() {
		switch (Data.currState) {
		case A_ORDER_ONDRIVE:
			menuButton.setText("Завершить");
			break;
		case A_ORDER_PAUSED:
			menuButton.setText("Продолжить");
			break;
		default:
			menuButton.setText("В путь");
		}

		// System.out.println(Data.currTariff);
		DecimalFormat fmt = new DecimalFormat("#.##");

		try {
			firmNameLabel.setText(MainConfig.firmName);
			tariffNameLabel.setText(Data.currTariff.name);
			callPriceLabel.setText(fmt.format(Data.currTariff.call) + " р");
			minimalPriceLabel.setText(fmt.format(Data.currTariff.minimalPrice)
					+ " р");
			kmPriceLabel.setText(fmt.format(Data.currTariff.priceKm) + " р/км");
			minutePriceLabel.setText(fmt.format(Data.currTariff.priceMinute)
					+ " р/мин");
			totalKmLabel.setText(fmt.format(Data.totalKm) + " км");
			float min = (float) Math.floor(Data.totalMin);
			DecimalFormat fmt2 = new DecimalFormat("#");
			totalMinutesLabel.setText(fmt.format(min) + " мин");
			float total = Data.totalCost;
			if (MainConfig.floorSum) {
				total = (float) (Math.floor(Data.totalCost / 5) * 5);
			}
			totalCostLabel.setText(fmt2.format(total));
		} catch (Exception e) {
			e.printStackTrace();
			if (Data.currTariff == null) {
				Data.currTariff = new Tariff();
			}
		}

	}

	final Handler handler = new Handler() {
		public void handleMessage(Message msg) {
			int type = msg.arg1;
			switch (type) {
			case 0:
				updateView();
				break;

			default:
				break;
			}
		}
	};

	private void buttonClick() {

		switch (Data.currState) {
		case A_ORDER_ONDRIVE:
			Data.requestOrderComplete(Data.currOrder.id);
			Data.currState = Types.NONE;
			break;
		case A_ORDER_CONFIRM:
		case A_ORDER_WAITING:
			Data.requestOndrive(Data.currOrder.id);
			Data.currState = Types.A_ORDER_ONDRIVE;
			Data.initDrive();
			break;
		case A_ORDER_PAUSED:
			Data.currState=Types.A_ORDER_ONDRIVE;
			break;
		default:
			try {
				Data.currTariff = MainConfig.tariffs.get(0);
			} catch (Exception e) {
				e.printStackTrace();
				Data.currTariff = new Tariff();
			}
			Data.initDrive();
			Data.currOrder = new Order();
			Data.currOrder.id = -1l;
			Data.requestOndrive(-1l);
			Data.currState = Types.A_ORDER_ONDRIVE;
			break;
		}

		progDialog = new ProgressDialog(getParent());
		progDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progDialog.setMessage("Ожидание подтверждения...");
		progThread = new ProgressThread(handler2);
		progThread.start();
		progDialog.show();
	}

	private class UpdateThread extends Thread {
		Handler mHandler;

		public UpdateThread(Handler handler) {
			mHandler = handler;
		}

		public void run() {
			while (true) {
				try {
					Message msg = mHandler.obtainMessage();
					msg.arg1 = 0;
					mHandler.sendMessage(msg);
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	final Handler handler2 = new Handler() {
		public void handleMessage(Message msg) {
			int total = msg.getData().getInt("ttl");
			progDialog.setProgress(total);
			if (total <= 0) {
				progDialog.dismiss();
				progThread.setState(ProgressThread.DONE);
				progThread.stop();
				System.err.println("prog: " + Data.waiting + ", "
						+ Data.currState);
				switch (Data.waiting) {
				case A_ORDER_ONDRIVE:
					Data.currState = Types.A_ORDER_ONDRIVE;
					Data.waiting = Types.NONE;
					Data.initDrive();
					break;
				case A_ORDER_COMPLETE:
					Data.currState = Types.NONE;
					Data.waiting = Types.NONE;
					break;
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
					&& (Data.waiting == Types.A_ORDER_ONDRIVE || Data.waiting == Types.A_ORDER_COMPLETE)) {
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
	}

}
