package com.example.vcbmonitor;

import java.util.HashMap;
import java.util.UUID;


import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;

import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.IBinder;
import android.util.Log;

public class PushService extends Service {
	// this is the log tag
	public static final String TAG = "VCBMonitorService";

	// the IP address, where your MQTT broker is running.
	private static final String MQTT_HOST = "121.40.187.96";
	// the port at which the broker is running.
	private static int MQTT_BROKER_PORT_NUM = 143;
	// Let's not use the MQTT persistence.
	private static MqttPersistence MQTT_PERSISTENCE = null;
	// We don't need to remember any state between the connections, so we use a
	// clean start.
	private static boolean MQTT_CLEAN_START = true;
	// Let's set the internal keep alive for MQTT to 15 mins. I haven't tested
	// this value much. It could probably be increased.
	private static short MQTT_KEEP_ALIVE = 60 * 15;
	// Set quality of services to 0 (at most once delivery), since we don't want
	// push notifications
	// arrive more than once. However, this means that some messages might get
	// lost (delivery is not guaranteed)
	private static int[] MQTT_QUALITIES_OF_SERVICE = { 0 };
	private static int MQTT_QUALITY_OF_SERVICE = 0;
	// The broker should not retain any messages.
	private static boolean MQTT_RETAINED_PUBLISH = false;

	// MQTT client ID, which is given the broker. In this example, I also use
	// this for the topic header.
	// You can use this to run push notifications for multiple apps with one
	// MQTT broker.
	public static String MQTT_CLIENT_ID = "tokudu";

	// These are the actions for the service (name are descriptive enough)
	private static final String ACTION_START = MQTT_CLIENT_ID + ".START";
	private static final String ACTION_STOP = MQTT_CLIENT_ID + ".STOP";
	private static final String ACTION_KEEPALIVE = MQTT_CLIENT_ID
			+ ".KEEP_ALIVE";
	private static final String ACTION_RECONNECT = MQTT_CLIENT_ID
			+ ".RECONNECT";

	// Connection log for the push service. Good for debugging.
	// private ConnectionLog mLog;

	// Connectivity manager to determining, when the phone loses connection
	private ConnectivityManager mConnMan;
	// Notification manager to displaying arrived push notifications
	private NotificationManager mNotifMan;

	// Whether or not the service has been started.
	private boolean mStarted;
	private HashMap<String, Long> waitForCallBack = new HashMap<String, Long>();
	// This the application level keep-alive interval, that is used by the
	// AlarmManager
	// to keep the connection active, even when the device goes to sleep.
	private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;

	// Retry intervals, when the connection is lost.
	private static final long INITIAL_RETRY_INTERVAL = 1000 * 10;
	private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 30;

	// Preferences instance
	private SharedPreferences mPrefs;
	// We store in the preferences, whether or not the service has been started
	public static final String PREF_STARTED = "isStarted";
	// We also store the deviceID (target)
	public static final String PREF_DEVICE_ID = "deviceID";
	// We store the last retry interval
	public static final String PREF_RETRY = "retryInterval";

	// Notification title
	public static String NOTIF_TITLE = "Tokudu";
	// Notification id
	private static final int NOTIF_CONNECTED = 0;

	// This is the instance of an MQTT connection.
	private MQTTConnection mConnection;
	private long mStartTime;
	private CallBackReceiver rec;

	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	
	// Static method to start the service
		public static void actionStart(Context ctx) {
			Intent i = new Intent(ctx, PushService.class);
			i.setAction(ACTION_START);
			ctx.startService(i);
		}

		// Static method to stop the service
		public static void actionStop(Context ctx) {
			Intent i = new Intent(ctx, PushService.class);
			i.setAction(ACTION_STOP);
			ctx.startService(i);
		}

		// Static method to send a keep alive message
		public static void actionPing(Context ctx) {
			Intent i = new Intent(ctx, PushService.class);
			i.setAction(ACTION_KEEPALIVE);
			ctx.startService(i);
		}
	
	@Override
	public void onCreate() {
		super.onCreate();

		log("Creating service");
		mStartTime = System.currentTimeMillis();
		rec = new CallBackReceiver(this);
		IntentFilter filter = new IntentFilter();
		filter.addAction("android.intent.action.MY_CALLBACK_RECEIVER");
		registerReceiver(rec, filter);
		// try {
		// mLog = new ConnectionLog();
		// Log.i(TAG, "Opened log at " + mLog.getPath());
		// } catch (IOException e) {
		// Log.e(TAG, "Failed to open log", e);
		// }

		// Get instances of preferences, connectivity manager and notification
		// manager
		mPrefs = getSharedPreferences(TAG, MODE_PRIVATE);
		mConnMan = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		mNotifMan = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		/*
		 * If our process was reaped by the system for any reason we need to
		 * restore our state with merely a call to onCreate. We record the last
		 * "started" value and restore it here if necessary.
		 */
		handleCrashedService();
	}

	// This method does any necessary clean-up need in case the server has been
	// destroyed by the system
	// and then restarted
	private void handleCrashedService() {
		if (wasStarted() == true) {
			log("Handling crashed service...");
			// stop the keep alives
			stopKeepAlives();

			// Do a clean start
			start();
		}
	}

	public void startKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.setRepeating(AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + KEEP_ALIVE_INTERVAL,
				KEEP_ALIVE_INTERVAL, pi);
	}

	public void log(String message) {
		log(message, null);
	}

	public void log(String message, Throwable e) {
		System.out.println(message);
		// if (e != null) {
		// Log.e(TAG, message, e);
		//
		// } else {
		// Log.i(TAG, message);
		// }
		//
		// if (mLog != null)
		// {
		// try {
		// mLog.println(message);
		// } catch (IOException ex) {}
		// }
	}

	// Reads whether or not the service has been started from the preferences
	private boolean wasStarted() {
		return mPrefs.getBoolean(PREF_STARTED, false);
	}

	// Remove all scheduled keep alives
	public void stopKeepAlives() {
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_KEEPALIVE);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	//
	private synchronized void connect() {
		log("Connecting...");
		// fetch the device ID from the preferences.
		// String deviceID = mPrefs.getString(PREF_DEVICE_ID, null);
		String deviceID = "test_1";
		// Create a new connection only if the device id is not NULL
		if (deviceID == null) {
			log("Device ID not found.");
		} else {
			try {
				mConnection = new MQTTConnection(MQTT_HOST, deviceID, this);
			} catch (MqttException e) {
				// Schedule a reconnect, if we failed to connect
				log("MqttException: "
						+ (e.getMessage() != null ? e.getMessage() : "NULL"));
				if (isNetworkAvailable()) {
					scheduleReconnect(mStartTime);
				}
			}
			setStarted(true);
		}
	}

	// Sets whether or not the services has been started in the preferences.
	private void setStarted(boolean started) {
		mPrefs.edit().putBoolean(PREF_STARTED, started).commit();
		mStarted = started;
	}

	// We schedule a reconnect based on the starttime of the service
	public void scheduleReconnect(long startTime) {
		// the last keep-alive interval
		long interval = mPrefs.getLong(PREF_RETRY, INITIAL_RETRY_INTERVAL);

		// Calculate the elapsed time since the start
		long now = System.currentTimeMillis();
		long elapsed = now - startTime;

		// Set an appropriate interval based on the elapsed time since start
		if (elapsed < interval) {
			interval = Math.min(interval * 4, MAXIMUM_RETRY_INTERVAL);
		} else {
			interval = INITIAL_RETRY_INTERVAL;
		}

		log("Rescheduling connection in " + interval + "ms.");

		// Save the new internval
		mPrefs.edit().putLong(PREF_RETRY, interval).commit();

		// Schedule a reconnect using the alarm manager.
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.set(AlarmManager.RTC_WAKEUP, now + interval, pi);
	}

	// Check if we are online
	public boolean isNetworkAvailable() {
		NetworkInfo info = mConnMan.getActiveNetworkInfo();
		if (info == null) {
			return false;
		}
		return info.isConnected();
	}

	private synchronized void start() {
		log("Starting service...");

		// Do nothing, if the service is already running.
		if (mStarted == true) {
			Log.w(TAG, "Attempt to start connection that is already active");
			return;
		}

		// Establish an MQTT connection
		connect();
		if (mConnection != null) {
			try {
				mConnection.publishToTopic("test/topic", "a");
			} catch (MqttException e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
			}
		}
		// Register a connectivity listener
		registerReceiver(mConnectivityChanged, new IntentFilter(
				ConnectivityManager.CONNECTIVITY_ACTION));
	}

	public synchronized void reconnectIfNecessary() {
		if (mStarted == true && mConnection == null) {
			log("Reconnecting...");
			connect();
		}
	}

	// Display the topbar notification
	public void showNotification(String text) {
		ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
		ComponentName cn = am.getRunningTasks(1).get(0).topActivity;
		System.out.println("pkg:" + cn.getPackageName());
		System.out.println("cls:" + cn.getClassName());
		// send to frontend
		Intent intent1 = new Intent();
		String uuid = UUID.randomUUID().toString();
		intent1.setAction("android.intent.action.MY_RECEIVER");
		intent1.putExtra("msg", "" + text);
		intent1.putExtra("ID", "" + uuid);
		this.waitForCallBack.put(uuid, System.currentTimeMillis());
		this.sendBroadcast(intent1);

		if (!cn.getClassName().trim().equals("com.example.mychat.MainActivity")) {
			NotificationManager nm = (NotificationManager) this
					.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification n = new Notification(R.drawable.icon, "请验证：" + (text),
					System.currentTimeMillis());
			n.flags = Notification.FLAG_AUTO_CANCEL;
			Intent i = new Intent(this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_NEW_TASK);
			// PendingIntent
			PendingIntent contentIntent = PendingIntent.getActivity(this,
					R.string.app_name, i, PendingIntent.FLAG_UPDATE_CURRENT);

			n.setLatestEventInfo(this, "请验证：" + (text), "请验证：" + (text),
					contentIntent);
			nm.notify(R.string.app_name, n);
		}
		this.waitForCallBack.clear();		
	}

	// Remove the scheduled reconnect
	public void cancelReconnect() {
		Intent i = new Intent();
		i.setClass(this, PushService.class);
		i.setAction(ACTION_RECONNECT);
		PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
		AlarmManager alarmMgr = (AlarmManager) getSystemService(ALARM_SERVICE);
		alarmMgr.cancel(pi);
	}

	// This receiver listeners for network changes and updates the MQTT
	// connection
	// accordingly
	private BroadcastReceiver mConnectivityChanged = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			// Get network info
			NetworkInfo info = (NetworkInfo) intent
					.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

			// Is there connectivity?
			boolean hasConnectivity = (info != null && info.isConnected()) ? true
					: false;

			log("Connectivity changed: connected=" + hasConnectivity);

			if (hasConnectivity) {
				reconnectIfNecessary();
			} else if (mConnection != null) {
				// if there no connectivity, make sure MQTT connection is
				// destroyed
				mConnection.disconnect();
				cancelReconnect();
				mConnection = null;
			}
		}
	};

	public MQTTConnection getmConnection() {
		return mConnection;
	}

	public void setmConnection(MQTTConnection mConnection) {
		this.mConnection = mConnection;
	}

}
