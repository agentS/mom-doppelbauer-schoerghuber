package io.weatherStation.dto;

import javax.json.bind.annotation.JsonbProperty;
import java.time.LocalDateTime;

public final class RecordDto {
	@JsonbProperty("weatherStationId")
	private long weatherStationId;
	@JsonbProperty("timestamp")
	private LocalDateTime timestamp;
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
