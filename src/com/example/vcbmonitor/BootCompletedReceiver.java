package com.example.vcbmonitor;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		//SystemStatus.startAlarm(context);
//		Log.i(PushService.TAG, "boot start");
//		System.out.println("Boot start...");
		PushService.actionStart(context.getApplicationContext());
		// context.startService(new Intent(context, PushService.class));
		// // {
		// //PushService.actionStart(context.getApplicationContext());
		// System.out.println("11111111111111: " + intent.getAction());
		// // }

	}

}
