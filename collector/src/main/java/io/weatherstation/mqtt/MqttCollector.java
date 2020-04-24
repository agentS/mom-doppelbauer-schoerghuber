package io.weatherstation.mqtt;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import io.weatherstation.dto.MeasurementDto;
import io.weatherstation.dto.RecordDto;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

@ApplicationScoped
public class MqttCollector {
	private final String topicPrefix;

	@Inject
	public MqttCollector(@ConfigProperty(name = "mqtt.topic-prefix") String topicPrefix) {
		this.topicPrefix = topicPrefix;
	}

	@Incoming("mqtt-sensor-data")
	@Outgoing("amqp-sensor-values")
	@Broadcast
	public String processMeasurement(MqttMessage<byte[]> message) {
		long weatherStationId = MqttMessageParser.parseWeatherStationId(message.getTopic(), this.topicPrefix);
		String payload = new String(message.getPayload());
		MeasurementDto measurement = MqttMessageParser.parseMeasurementCsv(payload);
		RecordDto record = new RecordDto(weatherStationId, measurement);
		Jsonb jsonBuilder = JsonbBuilder.create();
		String recordJson = jsonBuilder.toJson(record);
		System.out.println(recordJson);
		return recordJson;
	}
}
