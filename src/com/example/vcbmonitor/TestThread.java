package com.example.vcbmonitor;

import java.util.Date;

import android.app.Service;
import android.content.Intent;

public class TestThread implements Runnable {
	
	private Service parent = null;
	public TestThread(Service _parent)
	{
		this.parent = _parent;
	}
	public static final String NUM_COUNT_RECEIVER = "com.min.musicdemo.action.NUM_COUNT";
	@Override
	public void run() {
		while (true) {
			Intent intent1 = new Intent(TestThread.NUM_COUNT_RECEIVER);
			intent1.putExtra("title", new Date().toLocaleString());  
			this.parent.sendBroadcast(intent1);  
			System.out
					.println("send:"
							+ new Date().toLocaleString()); 
			 try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
