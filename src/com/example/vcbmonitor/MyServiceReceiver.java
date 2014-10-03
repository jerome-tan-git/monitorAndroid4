package com.example.vcbmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class MyServiceReceiver extends BroadcastReceiver{
	
	private MainActivity parent;
	public MyServiceReceiver(MainActivity _parent)
	{
		this.parent = _parent;
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		if (intent.getAction().equals(TestThread.NUM_COUNT_RECEIVER)) {
			String title = intent.getStringExtra("title");
			System.out.println("Receive: " + title);
			//this.parent.refreshText(title);
			
		}
		
	}

}
