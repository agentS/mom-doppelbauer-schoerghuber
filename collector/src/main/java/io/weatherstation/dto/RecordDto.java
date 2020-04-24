package io.weatherstation.dto;

import javax.json.bind.annotation.JsonbProperty;
import java.time.LocalDateTime;

public final class RecordDto {
	private long weatherStationId;
	private LocalDateTime timestamp;
	private MeasurementDto measurementDto;

	public RecordDto() {
	}

	public RecordDto(long weatherStationId, LocalDateTime timestamp, MeasurementDto measurementDto) {
		this.weatherStationId = weatherStationId;
		this.timestamp = timestamp;
		this.measurementDto = measurementDto;
	}

	public long getWeatherStationId() {
		return this.weatherStationId;
	}

	public void setWeatherStationId(long weatherStationId) {
		this.weatherStationId = weatherStationId;
	}

	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}

	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}

	@JsonbProperty("measurement")
	public MeasurementDto getMeasurementDto() {
		return this.measurementDto;
	}

	public void setMeasurementDto(MeasurementDto measurementDto) {
		this.measurementDto = measurementDto;
	}
}
