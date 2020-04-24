package io.weatherstation;

import io.quarkus.test.junit.QuarkusTest;
import io.weatherstation.dto.MeasurementDto;
import io.weatherstation.mqtt.MqttMessageParser;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
public class MqttMessageParserTest {
	@Inject @ConfigProperty(name = "mqtt.topic-prefix")
	private String topicPrefix;

	@Test
	public void testParseWeatherStationIdFromTopic() {
		long weatherStationId =
				MqttMessageParser.parseWeatherStationId("sensor-data/2", this.topicPrefix);
		assertEquals(2, weatherStationId);
	}

	@Test
	public void testParseMeasurementCsv() {
		MeasurementDto measurement =
				MqttMessageParser.parseMeasurementCsv("31.90;95.49;1023");
		assertNotNull(measurement);
		assertEquals(31.9, measurement.getTemperature());
		assertEquals(95.49, measurement.getHumidity());
		assertEquals(1023, measurement.getAirPressure());
	}
}
