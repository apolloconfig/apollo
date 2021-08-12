/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.spring.property;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;

/**
 * Spring @Value method info
 *
 * @author github.com/zhegexiaohuozi  seimimaster@gmail.com
 * @since 2018/2/6.
 */
public class SpringValue {

  private MethodParameter methodParameter;
  private Field field;
  private WeakReference<Object> beanRef;
  private String beanName;
  private String key;
  private String placeholder;
  private Class<?> targetType;
  private Type genericType;
  private boolean isJson;

  public SpringValue(String key, String placeholder, Object bean, String beanName, Field field, boolean isJson) {
    this.beanRef = new WeakReference<>(bean);
    this.beanName = beanName;
    this.field = field;
    this.key = key;
    this.placeholder = placeholder;
    this.targetType = field.getType();
    this.isJson = isJson;
    if(isJson){
      this.genericType = field.getGenericType();
    }
  }

  public SpringValue(String key, String placeholder, Object bean, String beanName, Method method, boolean isJson) {
    this.beanRef = new WeakReference<>(bean);
    this.beanName = beanName;
    this.methodParameter = new MethodParameter(method, 0);
    this.key = key;
    this.placeholder = placeholder;
    Class<?>[] paramTps = method.getParameterTypes();
    this.targetType = paramTps[0];
    this.isJson = isJson;
    if(isJson){
      this.genericType = method.getGenericParameterTypes()[0];
    }
  }

  public void update(Object newVal) throws IllegalAccessException, InvocationTargetException {
    if (isField()) {
      injectField(newVal);
    } else {
      injectMethod(newVal);
    }
  }

  private void injectField(Object newVal) throws IllegalAccessException {
    Object bean = beanRef.get();
    if (bean == null) {
      return;
    }
    boolean accessible = field.isAccessible();
    field.setAccessible(true);
    field.set(bean, newVal);
    field.setAccessible(accessible);
  }

  private void injectMethod(Object newVal)
      throws InvocationTargetException, IllegalAccessException {
    Object bean = beanRef.get();
    if (bean == null) {
      return;
    }
    methodParameter.getMethod().invoke(bean, newVal);
  }

  public String getBeanName() {
    return beanName;
  }

  public Class<?> getTargetType() {
    return targetType;
  }

  public String getPlaceholder() {
    return this.placeholder;
  }

  public MethodParameter getMethodParameter() {
    return methodParameter;
  }

  public boolean isField() {
    return this.field != null;
  }

  public Field getField() {
    return field;
  }

  public Type getGenericType() {
    return genericType;
  }

  public boolean isJson() {
    return isJson;
  }

  boolean isTargetBeanValid() {
    return beanRef.get() != null;
  }

  @Override
  public String toString() {
    Object bean = beanRef.get();
    if (bean == null) {
      return "";
    }
    if (isField()) {
      return String
          .format("key: %s, beanName: %s, field: %s.%s", key, beanName, bean.getClass().getName(), field.getName());
    }
    return String.format("key: %s, beanName: %s, method: %s.%s", key, beanName, bean.getClass().getName(),
        methodParameter.getMethod().getName());
  }
}
