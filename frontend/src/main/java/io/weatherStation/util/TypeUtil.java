package io.weatherStation.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeUtil {

  public static <T> ParameterizedType getTypeInfoOfGenericBaseclass(Class<? extends T> clazz, Class<T> baseclass) {

    Type type = clazz.getGenericSuperclass();

    while (!(type instanceof ParameterizedType) || ((ParameterizedType)type).getRawType() != baseclass) {
      if (type instanceof ParameterizedType)
        type = ((Class<?>)((ParameterizedType)type).getRawType()).getGenericSuperclass();
      else type = ((Class<?>)type).getGenericSuperclass();
    }

    return (ParameterizedType)type;
  }
}
