package com.example.vcbmonitor;

import java.util.HashMap;

import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;
import com.ibm.mqtt.MqttSimpleCallback;

public class MQTTConnection implements MqttSimpleCallback{
	public static final String TAG = "VCBMonitorService";

	// the IP address, where your MQTT broker is running.
	private static final String MQTT_HOST = "192.168.103.18";
	// the port at which the broker is running.
	private static int MQTT_BROKER_PORT_NUM = 1883;
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
	IMqttClient mqttClient = null;

	private PushService parent;
	// Creates a new connection given the broker address and initial topic
	public MQTTConnection(String brokerHostName, String initTopic, PushService _parent)
			throws MqttException {
		// Create connection spec
		String mqttConnSpec = "tcp://" + brokerHostName + ":"
				+ MQTT_BROKER_PORT_NUM;
		// Create the client and connect
		mqttClient = MqttClient.createMqttClient(mqttConnSpec,
				MQTT_PERSISTENCE);
		this.parent = _parent;
		// String clientID = MQTT_CLIENT_ID + "/" +
		// mPrefs.getString(PREF_DEVICE_ID, "");
		String clientID = "test_1";
		mqttClient.connect(clientID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);

		// register this client app has being able to receive messages
		mqttClient.registerSimpleHandler(this);

		// Subscribe to an initial topic, which is combination of client ID
		// and device ID.
		// initTopic = MQTT_CLIENT_ID + "/" + initTopic;
		initTopic = "test/topass";
		subscribeToTopic(initTopic);

		parent.log("Connection established to " + brokerHostName + " on topic "
				+ initTopic);

		// Save start time
		mStartTime = System.currentTimeMillis();
		// Star the keep-alives
		parent.startKeepAlives();
	}

	// Disconnect
	public void disconnect() {
		try {
			parent.stopKeepAlives();
			mqttClient.disconnect();
		} catch (MqttPersistenceException e) {
			parent.log("MqttException"
					+ (e.getMessage() != null ? e.getMessage() : " NULL"),
					e);
		}
	}

	/*
	 * Send a request to the message broker to be sent messages published
	 * with the specified topic name. Wildcards are allowed.
	 */
	private void subscribeToTopic(String topicName) throws MqttException {

		if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
			// quick sanity check - don't try and subscribe if we don't have
			// a connection
			parent.log("Connection error" + "No connection");
		} else {
			String[] topics = { topicName };
			mqttClient.subscribe(topics, MQTT_QUALITIES_OF_SERVICE);
		}
	}

	/*
	 * Sends a message to the message broker, requesting that it be
	 * published to the specified topic.
	 */
	public void publishToTopic(String topicName, String message)
			throws MqttException {
		if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
			// quick sanity check - don't try and publish if we don't have
			// a connection
			parent.log("No connection to public to");
		} else {
			mqttClient.publish(topicName, message.getBytes(),
					MQTT_QUALITY_OF_SERVICE, MQTT_RETAINED_PUBLISH);
		}
	}

	/*
	 * Called if the application loses it's connection to the message
	 * broker.
	 */
	public void connectionLost() throws Exception {
		parent.log("Loss of connection" + "connection downed");
		parent.stopKeepAlives();
		// null itself
		mConnection = null;
		if (parent.isNetworkAvailable() == true) {
			parent.reconnectIfNecessary();
		}
	}

	/*
	 * Called when we receive a message from the message broker.
	 */
	public void publishArrived(String topicName, byte[] payload, int qos,
			boolean retained) {
		// Show a notification
		String s = new String(payload);
		parent.showNotification(s);
		parent.log("Got message: " + s);
	}

	public void sendKeepAlive() throws MqttException {
		parent.log("Sending keep alive");
		// publish to a keep-alive topic
		publishToTopic(MQTT_CLIENT_ID + "/keepalive",
				mPrefs.getString(PREF_DEVICE_ID, ""));
	}

}
