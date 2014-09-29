package com.example.vcbmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyReceiver extends BroadcastReceiver{
	@Override
	public void onReceive(Context context, Intent intent) {
		String msg = intent.getStringExtra("msg");
		Log.i("Jerome","onclock...................... " + msg);

	}
}
