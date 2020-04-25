package io.weatherStation.dto;

import javax.json.bind.annotation.JsonbProperty;

public final class RecordDto {
	@JsonbProperty("weatherStationId")
	private long weatherStationId;
	@JsonbProperty("measurement")
	private MeasurementDto measurementDto;

	public RecordDto() {
	}

	public RecordDto(long weatherStationId, MeasurementDto measurementDto) {
		this.weatherStationId = weatherStationId;
		this.measurementDto = measurementDto;
	}

	public long getWeatherStationId() {
		return this.weatherStationId;
	}

	public void setWeatherStationId(long weatherStationId) {
		this.weatherStationId = weatherStationId;
	}

	@JsonbProperty("measurement")
	public MeasurementDto getMeasurementDto() {
		return this.measurementDto;
	}

	public void setMeasurementDto(MeasurementDto measurementDto) {
		this.measurementDto = measurementDto;
	}
}