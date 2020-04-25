package io.weatherstation.mqtt;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import io.vertx.amqp.AmqpMessage;
import io.vertx.core.json.JsonObject;
import io.weatherstation.dto.MeasurementDto;
import io.weatherstation.dto.RecordDto;
import io.weatherstation.dto.RecordJsonConverter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.ws.rs.core.MediaType;
import java.time.LocalDateTime;

@ApplicationScoped
public class MqttCollector {
	private final String topicPrefix;

	@Inject
	public MqttCollector(@ConfigProperty(name = "mqtt.topic-prefix") String topicPrefix) {
		this.topicPrefix = topicPrefix;
	}

	@Incoming("mqtt-sensor-data")
	@Outgoing("amqp-measurement-records")
	@Broadcast
	public AmqpMessage processMeasurement(MqttMessage<byte[]> message) {
		long weatherStationId = MqttMessageParser.parseWeatherStationId(message.getTopic(), this.topicPrefix);
		String payload = new String(message.getPayload());
		MeasurementDto measurement = MqttMessageParser.parseMeasurementCsv(payload);
		RecordDto record = new RecordDto(weatherStationId, LocalDateTime.now(), measurement);
		return AmqpMessage.create()
				.contentType(MediaType.APPLICATION_JSON)
				.withJsonObjectAsBody(RecordJsonConverter.toJson(record))
				.build();
	}
}
