package io.weatherStation.dal.jpa;

import io.weatherStation.dal.StationDao;
import io.weatherStation.entity.Station;

import javax.enterprise.context.RequestScoped;
import javax.transaction.Transactional;

@RequestScoped
@Transactional
public class StationDaoJpa extends AbstractDaoBean<Station, Long> implements StationDao {
}
