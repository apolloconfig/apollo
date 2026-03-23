/*
 * Copyright 2025 Apollo Authors
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

import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.CollectionUtils;


public abstract class RefreshableConfig implements DisposableBean {

  private static final Logger logger = LoggerFactory.getLogger(RefreshableConfig.class);

  private static final String LIST_SEPARATOR = ",";
  // TimeUnit: second
  private static final int CONFIG_REFRESH_INTERVAL = 60;

  protected Splitter splitter = Splitter.on(LIST_SEPARATOR).omitEmptyStrings().trimResults();
  protected static final String[] EMPTY_STRING_ARRAY = new String[0];

  @Autowired
  private ConfigurableEnvironment environment;

  private List<RefreshablePropertySource> propertySources;
  private ScheduledExecutorService executorService;

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

    // add property source to environment
    for (RefreshablePropertySource propertySource : propertySources) {
      propertySource.refresh();
      environment.getPropertySources().addLast(propertySource);
    }

    // task to update configs
    executorService =
        Executors.newScheduledThreadPool(1, ApolloThreadFactory.create("ConfigRefresher", true));

    executorService.scheduleWithFixedDelay(() -> {
      try {
        propertySources.forEach(RefreshablePropertySource::refresh);
      } catch (Throwable t) {
        logger.error("Refresh configs failed.", t);
        Tracer.logError("Refresh configs failed.", t);
      }
    }, CONFIG_REFRESH_INTERVAL, CONFIG_REFRESH_INTERVAL, TimeUnit.SECONDS);
  }

  @Override
  public void destroy() {
    if (executorService != null) {
      executorService.shutdownNow();
    }
  }

  /**
   * Trims each element and omits empty strings. Use for comma-separated configs where empty
   * items (e.g. from consecutive commas) should be omitted.
   */
  protected List<String> trimAndOmitEmpty(String[] arr) {
    if (arr == null || arr.length == 0) {
      return Collections.emptyList();
    }
    List<String> result = new ArrayList<>();
    for (String s : arr) {
      String trimmed = s.trim();
      if (!trimmed.isEmpty()) {
        result.add(trimmed);
      }
    }
    return result;
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

  public int checkInt(int value, int min, int max, int defaultValue) {
    if (value >= min && value <= max) {
      return value;
    }
    logger.warn("Configuration value '{}' is out of bounds [{} - {}]. Using default value '{}'.",
        value, min, max, defaultValue);
    return defaultValue;
  }

}
