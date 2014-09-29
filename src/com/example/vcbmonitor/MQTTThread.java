package com.example.vcbmonitor;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttPersistence;

public class MQTTThread implements Runnable{
	public static final String TAG = "PushService";

	// the IP address, where your MQTT broker is running.
	private static final String MQTT_HOST = "121.40.187.96";// 需要改成服务器IP
	// the port at which the broker is running.
	private static int MQTT_BROKER_PORT_NUM = 143;// 需要改成服务器port
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

	public static String MQTT_CLIENT_ID = "ata";// 需要改成自己需要的名称

	// These are the actions for the service (name are descriptive enough)
	private static final String ACTION_START = MQTT_CLIENT_ID + ".START";
	private static final String ACTION_STOP = MQTT_CLIENT_ID + ".STOP";
	private static final String ACTION_KEEPALIVE = MQTT_CLIENT_ID
			+ ".KEEP_ALIVE";
	private static final String ACTION_RECONNECT = MQTT_CLIENT_ID
			+ ".RECONNECT";
	private boolean mStarted;
	private static final long KEEP_ALIVE_INTERVAL = 1000 * 60 * 28;

	// Retry intervals, when the connection is lost.
	private static final long INITIAL_RETRY_INTERVAL = 1000 * 10;
	private static final long MAXIMUM_RETRY_INTERVAL = 1000 * 60 * 30;
	IMqttClient mqttClient = null;
	@Override
	public void run() {
		String mqttConnSpec = "tcp://" + MQTT_HOST + "@"
				+ MQTT_BROKER_PORT_NUM;
		try
		{
		// Create the client and connect
		mqttClient = MqttClient.createMqttClient(mqttConnSpec,
				MQTT_PERSISTENCE);
		String clientID = MQTT_CLIENT_ID + "/aaa";
		mqttClient.connect(clientID, MQTT_CLEAN_START, MQTT_KEEP_ALIVE);
//		mqttClient.registerSimpleHandler(this);
		String initTopic = "bbb";
		// Subscribe to an initial topic, which is combination of client ID
		// and device ID.
		initTopic = MQTT_CLIENT_ID + "/" + initTopic;
		subscribeToTopic(initTopic);

		System.out.println("Connection established to " + MQTT_HOST + " on topic "
				+ initTopic);
		} catch(Exception e)
		{
			e.printStackTrace();
		}
		//PushService.actionStart(getApplicationContext());
		//SystemStatus.startAlarm(this);
		
	}
	private void subscribeToTopic(String topicName) throws MqttException {

		if ((mqttClient == null) || (mqttClient.isConnected() == false)) {
			// quick sanity check - don't try and subscribe if we don't have
			// a connection
			System.out.println("Connection error" + "No connection");
		} else {
			String[] topics = { topicName };
			mqttClient.subscribe(topics, MQTT_QUALITIES_OF_SERVICE);
			System.out.println("listen to topic: " + topicName);
		}
	}
}
