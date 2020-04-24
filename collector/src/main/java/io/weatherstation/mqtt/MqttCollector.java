package io.weatherstation.mqtt;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MqttCollector {
	@Incoming("mqtt-sensor-data")
	@Outgoing("amqp-sensor-values")
	@Broadcast
	public String processMeasurement(MqttMessage<byte[]> rawMessage) {
		System.out.println(rawMessage.getTopic());
		String payload = new String(rawMessage.getPayload());
		return payload;
	}
}
