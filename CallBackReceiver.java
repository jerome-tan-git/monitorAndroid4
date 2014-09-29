package com.example.vcbmonitor;

import com.ibm.mqtt.MqttException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CallBackReceiver extends BroadcastReceiver {
	private PushService parent;
	public CallBackReceiver(PushService _parent)
	{
		this.parent = _parent;
	}
	@Override
	public void onReceive(Context context, Intent intent) {
		if (!intent.getStringExtra("msg").trim().toLowerCase()
				.equals("added")) {
			try {
				this.parent.getmConnection().publishToTopic("test/topic",
						intent.getStringExtra("msg"));
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
