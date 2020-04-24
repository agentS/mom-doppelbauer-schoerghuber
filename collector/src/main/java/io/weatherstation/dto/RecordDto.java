package io.weatherstation.dto;

public final class RecordDto {
	private long weatherStationId;
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

	public MeasurementDto getMeasurementDto() {
		return this.measurementDto;
	}

	public void setMeasurementDto(MeasurementDto measurementDto) {
		this.measurementDto = measurementDto;
	}
}
