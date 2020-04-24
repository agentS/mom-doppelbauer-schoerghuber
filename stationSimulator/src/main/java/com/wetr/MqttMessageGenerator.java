package com.wetr;

import com.wetr.simulator.impl.StationSimulator;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;

public class MqttMessageGenerator implements Callable<Void> {
    public static final String MQTT_SERVER_URI = "tcp://0.0.0.0:1883";
    public static final int MQTT_CONNECTION_TIMEOUT = 10;
    public static final String TOPIC_PREFIX = "sensor-data";

    private IMqttClient client;
    private List<StationSimulator> stationSimulators;

    public MqttMessageGenerator(List<StationSimulator> stationSimulators) throws Exception {
        this.client = createMqttClient();
        this.stationSimulators = stationSimulators;
    }

    /**
     * create new MQTT client
     * */
    public static IMqttClient createMqttClient() throws Exception {
        String publisherId = UUID.randomUUID().toString();
        IMqttClient publisher = new MqttClient(MQTT_SERVER_URI, publisherId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        options.setConnectionTimeout(MQTT_CONNECTION_TIMEOUT);
        publisher.connect(options);
        return publisher;
    }

    /**
     * publish messages to MQTT queue generated by the weather station simulators
     * */
    public Void call() throws Exception {
        if ( !client.isConnected()) {
            System.err.println("[ INFO ] " + LocalDateTime.now() + "client not connected");
            return null;
        }
        for(StationSimulator stationSimulator : stationSimulators){
            String topic = TOPIC_PREFIX + "/" + stationSimulator.getId();
            MqttMessage msg = readMessage(stationSimulator);
            msg.setQos(0);
            msg.setRetained(true);
            client.publish(topic, msg);
            System.out.println("[ INFO ] " + LocalDateTime.now() + " : " + topic + " --> " + msg);
        }
        return null;
    }

    /**
     * read message from station simulator and create to MQTT message
     * */
    private MqttMessage readMessage(StationSimulator stationSimulator) {
        String message = stationSimulator.generateMessage();
        byte[] payload = message.getBytes();
        return new MqttMessage(payload);
    }
}