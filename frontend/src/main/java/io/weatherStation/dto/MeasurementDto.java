package io.weatherStation.dto;

import javax.json.bind.annotation.JsonbProperty;

public final class MeasurementDto {
	public static final int NUMBER_OF_SENSOR_VALUES = 3;

	@JsonbProperty("temperature")
	private double temperature;
	@JsonbProperty("humidity")
	private double humidity;
	@JsonbProperty("airPressure")
	private double airPressure;

	public MeasurementDto() {
	}

	public MeasurementDto(double temperature, double humidity, double airPressure) {
		this.temperature = temperature;
		this.humidity = humidity;
		this.airPressure = airPressure;
	}

	public double getTemperature() {
		return this.temperature;
	}

	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	public double getHumidity() {
		return this.humidity;
	}

	public void setHumidity(double humidity) {
		this.humidity = humidity;
	}

	public double getAirPressure() {
		return this.airPressure;
	}

	public void setAirPressure(double airPressure) {
		this.airPressure = airPressure;
	}
}
