package io.weatherStation.dal;

import java.io.Serializable;
import java.util.List;

public interface Dao<T, ID extends Serializable> {

  boolean entityExists(ID id);
  T       findById(ID id);
  List<T> findAll();

  T       merge(T entity);
  void    persist(T entity);
  void    remove(T entity);
}
