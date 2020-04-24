package com.hydroponic.dal.jpa;

import com.hydropondic.dal.Dao;
import com.hydropondic.util.TypeUtil;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.util.List;

public abstract class AbstractDaoBean<T, ID extends Serializable> implements Dao<T, ID> {

  @PersistenceContext
  private EntityManager em;

  private Class<T> entityType;

  public AbstractDaoBean() {
    ParameterizedType type = TypeUtil.getTypeInfoOfGenericBaseclass(this.getClass(), AbstractDaoBean.class);
    this.entityType = (Class<T>)(type.getActualTypeArguments()[0]);
  }

  protected EntityManager getEntityManager() {
    if (em == null) throw new IllegalStateException("EntityManager has not been set on DAO before usage");
    return em;
  }

  public Class<T> getEntityBeanType() {
    return entityType;
  }

  @Override
  public T findById(ID id) {
    T entity = getEntityManager().find(getEntityBeanType(), id);
    return entity;
  }

  @Override
  public boolean entityExists(ID id) {
    return getEntityManager().find(getEntityBeanType(), id) != null;
  }

  @Override
  public List<T> findAll() {
    TypedQuery<T> selAllQry = em.createQuery(String.format("select entity from %s entity", entityType.getName()), entityType);
    return selAllQry.getResultList();
  }

  @Override
  public T merge(T entity) {
    return getEntityManager().merge(entity);
  }

  @Override
  public void persist(T entity) {
    getEntityManager().persist(entity);
  }

  @Override
  public void remove(T entity) {
    getEntityManager().remove(entity);
  }
}
