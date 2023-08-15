package com.ctrip.framework.apollo.audit.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class ApolloAuditUtil {

  public static String generateId(){
    return UUID.randomUUID().toString().replaceAll("-","");
  }

  public static long getIdByReflect(Object o) {
    Class<?> clazz = o.getClass();
    try {
      Field idField = null;
      if (Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals("id"))) {
        idField = o.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        return (long) idField.get(o);
      } else if (Arrays.stream(clazz.getSuperclass().getDeclaredFields()).anyMatch(f -> f.getName().equals("id"))) {
        idField = o.getClass().getSuperclass().getDeclaredField("id");
        idField.setAccessible(true);
        return (long) idField.get(o);
      }
      return -1;
    } catch (NoSuchFieldException | IllegalAccessException e) {
      e.printStackTrace();
      return -1; // Return a default value or handle the error
    }
  }

  public static List<Field> getAnnotatedField(Class<? extends Annotation> annoClass, Object o) {
    Class<?> oClass = o.getClass();
    return Arrays.stream(oClass.getDeclaredFields()).filter(
        field -> field.isAnnotationPresent(annoClass)
    ).collect(Collectors.toList());
  }

  public static List<Object> toList(Object obj) {
    if(obj instanceof Collection) {
      Collection<?> collection = (Collection<?>) obj;
      return new ArrayList<>(collection);
    } else {
      return Collections.singletonList(obj);
    }
  }

}
