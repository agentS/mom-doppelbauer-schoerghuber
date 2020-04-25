package io.weatherStation.manager;

import io.weatherStation.dto.StationDto;

import java.util.List;

public interface StationManager {
    List<StationDto> findAll();
    StationDto findById(Long id);
    StationDto add(StationDto stationDto);
    StationDto update(StationDto stationDto);
}
