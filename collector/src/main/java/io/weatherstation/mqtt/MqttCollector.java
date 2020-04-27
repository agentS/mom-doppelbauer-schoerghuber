package io.weatherstation.mqtt;

import io.smallrye.reactive.messaging.annotations.Broadcast;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import io.vertx.core.json.JsonObject;
import io.weatherstation.dto.MeasurementDto;
import io.weatherstation.dto.RecordDto;
import io.weatherstation.dto.RecordJsonConverter;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@ApplicationScoped
public class MqttCollector {
	private final String topicPrefix;

	@Inject
	public MqttCollector(@ConfigProperty(name = "mqtt.topic-prefix") String topicPrefix) {
		this.topicPrefix = topicPrefix;
	}

	@Incoming("mqtt-sensor-data")
	@Outgoing("amqp-measurement-records")
	@Acknowledgment(Acknowledgment.Strategy.MANUAL)
	@Broadcast
	public Message<String> processMeasurement(MqttMessage<byte[]> message) {
		long weatherStationId = MqttMessageParser.parseWeatherStationId(message.getTopic(), this.topicPrefix);
		String payload = new String(message.getPayload());
		MeasurementDto measurement = MqttMessageParser.parseMeasurementCsv(payload);
		LocalDateTime timestamp = LocalDateTime.now()
				.truncatedTo(ChronoUnit.SECONDS);
		RecordDto record = new RecordDto(weatherStationId, timestamp, measurement);
		return Message.of(
				RecordJsonConverter.toJson(record).toString(),
				message::ack
		);
	}
}
