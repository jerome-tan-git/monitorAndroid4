package com.example.vcbmonitor;

import java.util.Date;

import com.example.vcbmonitor.R.id;
import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;

import android.os.Bundle;
import android.os.SystemClock;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.view.Menu;
import android.widget.TextView;

public class MainActivity extends Activity {
	private TextView et;
	MyServiceReceiver mReceiver = null;
	IntentFilter filter = null;
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		SystemStatus.startAlarm(getApplicationContext());
//		setContentView(R.layout.activity_main);
//		mReceiver = new MyServiceReceiver(this);
//		filter = new IntentFilter(TestThread.NUM_COUNT_RECEIVER);
//		this.et = (TextView) findViewById(R.id.textView2);
//		registerReceiver(mReceiver, filter);
//		
//		Thread a = new Thread(new MQTTThread());
//		a.start();
		//System.out.println("aaaaaaaaaa");
		//startService(new Intent(this.getApplicationContext(), MyTestService.class));
		//PushService.actionStart(getApplicationContext());
		
	}
	
//	@Override
//	protected void onResume() {
//		registerReceiver(mReceiver, filter);
//		super.onResume();
//	}
//
//	@Override
//	protected void onPause() {
//		unregisterReceiver(mReceiver);
//		super.onPause();
//	}

	@Override
	public void onDestroy() {
//		unregisterReceiver(mReceiver);
		super.onDestroy();
	}
	public void refreshText(String _str)
	{
		this.et.setText(_str);
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);

		return true;

	}

}
