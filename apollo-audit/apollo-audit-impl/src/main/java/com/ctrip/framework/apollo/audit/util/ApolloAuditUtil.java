/*
 * Copyright 2023 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.ctrip.framework.apollo.audit.util;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLogDataInfluenceTable;
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

  public static String generateId() {
    return UUID.randomUUID().toString().replaceAll("-", "");
  }

  public static long getIdByReflect(Object o) {
    Class<?> clazz = o.getClass();
    try {
      Field idField = null;
      if (Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals("id"))) {
        idField = o.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        return (long) idField.get(o);
      } else if (Arrays.stream(clazz.getSuperclass().getDeclaredFields())
          .anyMatch(f -> f.getName().equals("id"))) {
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

  public static List<Field> getAnnotatedFields(Class<? extends Annotation> annoClass,
      Class<?> clazz) {
    return Arrays.stream(clazz.getDeclaredFields())
        .filter(field -> field.isAnnotationPresent(annoClass)).collect(Collectors.toList());
  }

  public static Field getAnnotatedField(Class<? extends Annotation> annoClass, Class<?> clazz) {
    for (Field f : clazz.getDeclaredFields()) {
      if (f.isAnnotationPresent(annoClass)) {
        return f;
      }
    }
    return null;
  }

  public static List<Object> toList(Object obj) {
    if (obj instanceof Collection) {
      Collection<?> collection = (Collection<?>) obj;
      return new ArrayList<>(collection);
    } else {
      return Collections.singletonList(obj);
    }
  }

  public static Field tryToGetIdField(Class<?> clazz) {
    Field idField = null;
    if (Arrays.stream(clazz.getDeclaredFields()).anyMatch(f -> f.getName().equals("id"))) {
      try {
        idField = clazz.getDeclaredField("id");
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      return idField;
    } else if (Arrays.stream(clazz.getSuperclass().getDeclaredFields())
        .anyMatch(f -> f.getName().equals("id"))) {
      try {
        idField = clazz.getSuperclass().getDeclaredField("id");
      } catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      return idField;
    }
    return null;
  }

  public static String getApolloAuditLogTableName(Class<?> clazz) {
    return clazz.isAnnotationPresent(ApolloAuditLogDataInfluenceTable.class)
        ? clazz.getAnnotation(ApolloAuditLogDataInfluenceTable.class).tableName()
        : null;
  }

}
