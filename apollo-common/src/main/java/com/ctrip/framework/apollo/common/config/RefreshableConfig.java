/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.common.config;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;

import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;


public abstract class RefreshableConfig {

  private static final Logger logger = LoggerFactory.getLogger(RefreshableConfig.class);

  private static final String LIST_SEPARATOR = ",";
  //TimeUnit: second
  private static final int CONFIG_REFRESH_INTERVAL = 60;

  protected Splitter splitter = Splitter.on(LIST_SEPARATOR).omitEmptyStrings().trimResults();

  @Autowired
  private ConfigurableEnvironment environment;

  private List<RefreshablePropertySource> propertySources;

  /**
   * register refreshable property source.
   * Notice: The front property source has higher priority.
   */
  protected abstract List<RefreshablePropertySource> getRefreshablePropertySources();

  @PostConstruct
  public void setup() {

    propertySources = getRefreshablePropertySources();
    if (CollectionUtils.isEmpty(propertySources)) {
      throw new IllegalStateException("Property sources can not be empty.");
    }

    //add property source to environment
    for (RefreshablePropertySource propertySource : propertySources) {
      propertySource.refresh();
      environment.getPropertySources().addLast(propertySource);
    }

    //task to update configs
    ScheduledExecutorService
        executorService =
        Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("ConfigRefresher", true));

    executorService
        .scheduleWithFixedDelay(() -> {
          try {
            propertySources.forEach(RefreshablePropertySource::refresh);
          } catch (Throwable t) {
            logger.error("Refresh configs failed.", t);
            Tracer.logError("Refresh configs failed.", t);
          }
        }, CONFIG_REFRESH_INTERVAL, CONFIG_REFRESH_INTERVAL, TimeUnit.SECONDS);
  }

  public int getIntProperty(String key, int defaultValue) {
    try {
      String value = getValue(key);
      return value == null ? defaultValue : Integer.parseInt(value);
    } catch (Throwable e) {
      Tracer.logError("Get int property failed.", e);
      return defaultValue;
    }
  }

  public boolean getBooleanProperty(String key, boolean defaultValue) {
    try {
      String value = getValue(key);
      return value == null ? defaultValue : "true".equals(value);
    } catch (Throwable e) {
      Tracer.logError("Get boolean property failed.", e);
      return defaultValue;
    }
  }

  public String[] getArrayProperty(String key, String[] defaultValue) {
    try {
      String value = getValue(key);
      return Strings.isNullOrEmpty(value) ? defaultValue : value.split(LIST_SEPARATOR);
    } catch (Throwable e) {
      Tracer.logError("Get array property failed.", e);
      return defaultValue;
    }
  }

  public String getValue(String key, String defaultValue) {
    try {
      return environment.getProperty(key, defaultValue);
    } catch (Throwable e) {
      Tracer.logError("Get value failed.", e);
      return defaultValue;
    }
  }

  public String getValue(String key) {
    return environment.getProperty(key);
  }

}
