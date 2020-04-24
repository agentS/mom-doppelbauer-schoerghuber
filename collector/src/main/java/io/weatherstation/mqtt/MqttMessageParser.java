package io.weatherstation.mqtt;

import io.weatherstation.dto.MeasurementDto;

public final class MqttMessageParser {
	public static final String CSV_DELIMITER = ";";

	private MqttMessageParser() {}

	public static long parseWeatherStationId(String topic, String topicPrefix) {
		String weatherStationIdString = topic.substring(topicPrefix.length());
		return Long.parseLong(weatherStationIdString);
	}

	public static MeasurementDto parseMeasurementCsv(String csv) {
		String[] sensorValues = csv.split(CSV_DELIMITER);
		if (sensorValues.length != MeasurementDto.NUMBER_OF_SENSOR_VALUES) {
			throw new IllegalArgumentException("The CSV record must exactly contain " + MeasurementDto.NUMBER_OF_SENSOR_VALUES + " senor value separated by '" + CSV_DELIMITER + "'");
		}

		MeasurementDto result = new MeasurementDto();
		result.setTemperature(Double.parseDouble(sensorValues[0]));
		result.setHumidity(Double.parseDouble(sensorValues[1]));
		result.setAirPressure(Double.parseDouble(sensorValues[2]));
		return result;
	}
}
