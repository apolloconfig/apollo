/*
 * Copyright 2022 Apollo Authors
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

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.events.ApolloConfigChangeEvent;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.CollectionUtils;

/**
 * Create by zhangzheng on 2018/3/6
 */
public class AutoUpdateConfigChangeListener implements ConfigChangeListener,
    ApplicationListener<ApolloConfigChangeEvent>, ApplicationContextAware {

  private static final Logger logger = LoggerFactory.getLogger(
      AutoUpdateConfigChangeListener.class);

  private final boolean typeConverterHasConvertIfNecessaryWithFieldParameter;
  private ConfigurableBeanFactory beanFactory;
  private TypeConverter typeConverter;
  private final PlaceholderHelper placeholderHelper;
  private final SpringValueRegistry springValueRegistry;
  private final Gson gson;
  private final ConfigUtil configUtil;

  public AutoUpdateConfigChangeListener() {
    this.typeConverterHasConvertIfNecessaryWithFieldParameter = testTypeConverterHasConvertIfNecessaryWithFieldParameter();
    this.placeholderHelper = SpringInjector.getInstance(PlaceholderHelper.class);
    this.springValueRegistry = SpringInjector.getInstance(SpringValueRegistry.class);
    this.gson = new Gson();
    this.configUtil = ApolloInjector.getInstance(ConfigUtil.class);
  }

  @Override
  public void onChange(ConfigChangeEvent changeEvent) {
    Set<String> keys = changeEvent.changedKeys();
    if (CollectionUtils.isEmpty(keys)) {
      return;
    }
    for (String key : keys) {
      // 1. check whether the changed key is relevant
      Collection<SpringValue> targetValues = springValueRegistry.get(beanFactory, key);
      if (targetValues == null || targetValues.isEmpty()) {
        continue;
      }

      // 2. update the value
      for (SpringValue val : targetValues) {
        updateSpringValue(val);
      }
    }
  }

  private void updateSpringValue(SpringValue springValue) {
    try {
      Object value = resolvePropertyValue(springValue);
      springValue.update(value);

      logger.info("Auto update apollo changed value successfully, new value: {}, {}", value,
          springValue);
    } catch (Throwable ex) {
      logger.error("Auto update apollo changed value failed, {}", springValue.toString(), ex);
    }
  }

  /**
   * Logic transplanted from DefaultListableBeanFactory
   *
   * @see org.springframework.beans.factory.support.DefaultListableBeanFactory#doResolveDependency(org.springframework.beans.factory.config.DependencyDescriptor,
   * java.lang.String, java.util.Set, org.springframework.beans.TypeConverter)
   */
  private Object resolvePropertyValue(SpringValue springValue) {
    // value will never be null, as @Value and @ApolloJsonValue will not allow that
    Object value = placeholderHelper
        .resolvePropertyValue(beanFactory, springValue.getBeanName(), springValue.getPlaceholder());

    if (springValue.isJson()) {
      value = parseJsonValue((String) value, springValue.getGenericType());
    } else {
      if (springValue.isField()) {
        // org.springframework.beans.TypeConverter#convertIfNecessary(java.lang.Object, java.lang.Class, java.lang.reflect.Field) is available from Spring 3.2.0+
        if (typeConverterHasConvertIfNecessaryWithFieldParameter) {
          value = this.typeConverter
              .convertIfNecessary(value, springValue.getTargetType(), springValue.getField());
        } else {
          value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType());
        }
      } else {
        value = this.typeConverter.convertIfNecessary(value, springValue.getTargetType(),
            springValue.getMethodParameter());
      }
    }

    return value;
  }

  private Object parseJsonValue(String json, Type targetType) {
    try {
      return gson.fromJson(json, targetType);
    } catch (Throwable ex) {
      logger.error("Parsing json '{}' to type {} failed!", json, targetType, ex);
      throw ex;
    }
  }

  private boolean testTypeConverterHasConvertIfNecessaryWithFieldParameter() {
    try {
      TypeConverter.class.getMethod("convertIfNecessary", Object.class, Class.class, Field.class);
    } catch (Throwable ex) {
      return false;
    }

    return true;
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    //it is safe enough to cast as all known application context is derived from ConfigurableApplicationContext
    this.beanFactory = ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
    this.typeConverter = this.beanFactory.getTypeConverter();
  }

  @Override
  public void onApplicationEvent(ApolloConfigChangeEvent event) {
    if (!configUtil.isAutoUpdateInjectedSpringPropertiesEnabled()) {
      return;
    }
    this.onChange(event.getConfigChangeEvent());
  }
}
