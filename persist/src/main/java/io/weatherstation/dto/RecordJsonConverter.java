package io.weatherstation.dto;

import io.vertx.core.json.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class RecordJsonConverter {
	public static final String KEY_WEATHER_STATION_ID = "weatherStationId";
	public static final String KEY_TIMESTAMP = "timestamp";
	public static final String KEY_MEASUREMENT = "measurement";
	public static final String KEY_MEASUREMENT_TEMPERATURE = "temperature";
	public static final String KEY_MEASUREMENT_HUMIDITY = "humidity";
	public static final String KEY_MEASUREMENT_AIR_PRESSURE = "airPressure";

	private RecordJsonConverter() {}

	public static JsonObject toJson(RecordDto record) {
		return new JsonObject()
			.put(KEY_WEATHER_STATION_ID, record.getWeatherStationId())
			.put(KEY_TIMESTAMP, record.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME))
			.put(KEY_MEASUREMENT, new JsonObject()
				.put(KEY_MEASUREMENT_TEMPERATURE, record.getMeasurementDto().getTemperature())
				.put(KEY_MEASUREMENT_HUMIDITY, record.getMeasurementDto().getHumidity())
				.put(KEY_MEASUREMENT_AIR_PRESSURE, record.getMeasurementDto().getAirPressure())
			);
	}

	public static RecordDto fromJson(JsonObject recordJson) {
		JsonObject measurementJson = recordJson.getJsonObject(KEY_MEASUREMENT);
		return new RecordDto(
			recordJson.getLong(KEY_WEATHER_STATION_ID),
			LocalDateTime.parse(recordJson.getString(KEY_TIMESTAMP), DateTimeFormatter.ISO_DATE_TIME),
			new MeasurementDto(
				measurementJson.getDouble(KEY_MEASUREMENT_TEMPERATURE),
				measurementJson.getDouble(KEY_MEASUREMENT_HUMIDITY),
				measurementJson.getDouble(KEY_MEASUREMENT_AIR_PRESSURE)
			)
		);
	}
}
