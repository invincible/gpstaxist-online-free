package ru.ufalinux.tasp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class JabberSettingsActivity extends Activity {

	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.jabber_settings);
		Button gogo = (Button) findViewById(R.id.jabberOkButton);
		gogo.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				gogoclick();
			}
		});
	}

	public void gogoclick(){
		TextView serverField = (TextView) findViewById(R.id.jabberServerField);
		TextView userField = (TextView) findViewById(R.id.jabberUserField);
		TextView passField = (TextView) findViewById(R.id.jabberPasswordField);
		TextView dispField = (TextView) findViewById(R.id.jabberDispField);
		String server = serverField.getText().toString();
		String user = userField.getText().toString();
		String pass = passField.getText().toString();
		String disp = dispField.getText().toString();
		Intent intent=this.getIntent();
		intent.putExtra("password", pass);
		intent.putExtra("server", server);
		intent.putExtra("user", user);
		intent.putExtra("disp", disp);
//		System.err.println("cont:"+getApplicationContext().getApplicationInfo()+" "+getParent());
		if(server.length()*user.length()*pass.length()*disp.length()==0){
			Toast toast = Toast.makeText(getApplicationContext(), "Неверный ввод", 5);
			toast.show();
//			this.setResult(RESULT_OK);
//			this.finish();
		}else{
			this.setResult(RESULT_OK,intent);
			this.finish();
		}
	}
	
}
