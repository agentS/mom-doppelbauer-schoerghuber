package io.weatherStation.manager.cdi;

import io.weatherStation.dal.StationDao;
import io.weatherStation.dto.StationDto;
import io.weatherStation.entity.Station;
import io.weatherStation.manager.StationManager;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@RequestScoped
@Transactional
public class StationManagerCdi implements StationManager {
    private StationDao stationDao;

    @Inject
    public StationManagerCdi(StationDao stationDao) {
        this.stationDao = stationDao;
    }

    @Override
    public List<StationDto> findAll() {
        List<Station> stations = stationDao.findAll();
        List<StationDto> stationDtos = mapStations(stations);
        return stationDtos;
    }

    @Override
    public StationDto findById(Long id) {
        Station station = stationDao.findById(id);
        if(station != null){
            return mapStation(station);
        } else {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public StationDto add(StationDto stationDto) {
        Station station = new Station();
        station.setName(stationDto.getName());
        Station newStation = stationDao.merge(station);
        return mapStation(newStation);
    }

    @Override
    public StationDto update(StationDto stationDto) {
        Station foundStation = stationDao.findById(stationDto.getId());
        if(foundStation != null){
            foundStation.setName(stationDto.getName());
            Station updatedStation = stationDao.merge(foundStation);
            return mapStation(updatedStation);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private List<StationDto> mapStations(List<Station> stations) {
        List<StationDto> stationDtos = new ArrayList<>();
        for(Station station : stations){
            StationDto stationDto = mapStation(station);
            stationDtos.add(stationDto);
        }
        return stationDtos;
    }

    private StationDto mapStation(Station station) {
        StationDto stationDto = new StationDto();
        stationDto.setId(station.getId());
        stationDto.setName(station.getName());
        return stationDto;
    }
}
