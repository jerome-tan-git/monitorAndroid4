package com.example.vcbmonitor;

import com.ibm.mqtt.IMqttClient;
import com.ibm.mqtt.MqttBrokerUnavailableException;
import com.ibm.mqtt.MqttClient;
import com.ibm.mqtt.MqttException;
import com.ibm.mqtt.MqttNotConnectedException;
import com.ibm.mqtt.MqttPersistence;
import com.ibm.mqtt.MqttPersistenceException;

public class MQTTThread implements Runnable{
	private IMqttClient mqttClient = null;
	private boolean mqttCleanStart = true;
	private short mqttKeepAlive = 60 * 15;
	private String clientID = "";
	public MQTTThread(IMqttClient _mqttClient, boolean _mqttCleanStart, short _mqttKeepAlive, String _clientID)
	{
		this.mqttClient = _mqttClient;
		this.mqttCleanStart = _mqttCleanStart;
		this.mqttKeepAlive = _mqttKeepAlive;
		this.clientID = _clientID;
	}
	
	
	@Override
	public void run() {
		try {
			mqttClient.connect(clientID, mqttCleanStart, mqttKeepAlive);
		} catch (MqttPersistenceException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttBrokerUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttNotConnectedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
