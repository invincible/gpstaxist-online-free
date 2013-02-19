package ru.ufalinux.tasp;

import ru.ufalinux.tasp.dataworks.Data;
import ru.ufalinux.tasp.dataworks.Types;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.TabHost;
import android.widget.TextView;

public class OrdersActivity extends TabActivity{

	protected TabHost thost;
	
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.orders_tab_layout);
		thost = getTabHost();
		TabHost.TabSpec spec;
		Intent intent;
		thost.getTabWidget().setVisibility(View.GONE);
		intent = new Intent().setClass(this, OrderListActivity.class);
		TextView tv=new TextView(this);
		tv.setText("Список");
		spec = thost.newTabSpec("orderlist").setIndicator("")
				.setContent(intent);
		thost.addTab(spec);
		tv=new TextView(this);
		tv.setText("Инфо");
		intent = new Intent().setClass(this, OrderInfoActivity.class);
		spec = thost.newTabSpec("orderinfo").setIndicator("")
				.setContent(intent);
		thost.addTab(spec);
		
		thost.setCurrentTab(0);
		TabChangeThread thread=new TabChangeThread(handler);
		thread.start();
	}
	
	final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int tab = msg.getData().getInt("id");
            if(thost.getCurrentTab()!=tab)
            	thost.setCurrentTab(tab);
        }
    };
    
    private class TabChangeThread extends Thread{
    	Handler mHandler;
    	
    	public TabChangeThread(Handler h) {
			mHandler=h;
		}
    	
    	public void run(){
    		while(true){
    			synchronized (Data.currState) {
    				try {
    					Data.currState.wait(1000);
    				} catch (InterruptedException e) {
    					e.printStackTrace();
    				}
//    				System.out.println("Tab thread:"+Data.currState);
    				Message msg=mHandler.obtainMessage();
    				Bundle b=new Bundle();
    				if(Data.currState==Types.NONE)
    					b.putInt("id", 0);
    				else
    					b.putInt("id", 1);
    				msg.setData(b);
    				mHandler.sendMessage(msg);
    			}
    		}
    	}
    }
	
}
