package com.example.vcbmonitor;

import java.util.Date;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

public class SystemStatus {
	public static boolean alarmStarted=false;
	public static void startAlarm(Context _context)
	{
		System.out.println("start alarm service.........");
		
		AlarmManager am = (AlarmManager) _context.getSystemService(Context.ALARM_SERVICE);
		Intent in = new Intent("ELITOR_CLOCK");
		in.putExtra("msg", new Date().toLocaleString());
		in.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
		PendingIntent sender = PendingIntent.getBroadcast(_context, 0, in,
				PendingIntent.FLAG_CANCEL_CURRENT);
		int interval = 60*5*1000;
		long triggerAtTime = SystemClock.elapsedRealtime();
		am.setRepeating(AlarmManager.RTC_WAKEUP, triggerAtTime, interval,
				sender);
	}
}
