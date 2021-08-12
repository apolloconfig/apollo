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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.enums.ConfigSourceType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class SimpleConfig extends AbstractConfig implements RepositoryChangeListener {
  private static final Logger logger = LoggerFactory.getLogger(SimpleConfig.class);
  private final String m_namespace;
  private final ConfigRepository m_configRepository;
  private volatile Properties m_configProperties;
  private volatile ConfigSourceType m_sourceType = ConfigSourceType.NONE;

  /**
   * Constructor.
   *
   * @param namespace        the namespace for this config instance
   * @param configRepository the config repository for this config instance
   */
  public SimpleConfig(String namespace, ConfigRepository configRepository) {
    m_namespace = namespace;
    m_configRepository = configRepository;
    this.initialize();
  }

  private void initialize() {
    try {
      updateConfig(m_configRepository.getConfig(), m_configRepository.getSourceType());
    } catch (Throwable ex) {
      Tracer.logError(ex);
      logger.warn("Init Apollo Simple Config failed - namespace: {}, reason: {}", m_namespace,
          ExceptionUtil.getDetailMessage(ex));
    } finally {
      //register the change listener no matter config repository is working or not
      //so that whenever config repository is recovered, config could get changed
      m_configRepository.addChangeListener(this);
    }
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    if (m_configProperties == null) {
      logger.warn("Could not load config from Apollo, always return default value!");
      return defaultValue;
    }
    return this.m_configProperties.getProperty(key, defaultValue);
  }

  @Override
  public Set<String> getPropertyNames() {
    if (m_configProperties == null) {
      return Collections.emptySet();
    }

    return m_configProperties.stringPropertyNames();
  }

  @Override
  public ConfigSourceType getSourceType() {
    return m_sourceType;
  }

  @Override
  public synchronized void onRepositoryChange(String namespace, Properties newProperties) {
    if (newProperties.equals(m_configProperties)) {
      return;
    }
    Properties newConfigProperties = propertiesFactory.getPropertiesInstance();
    newConfigProperties.putAll(newProperties);

    List<ConfigChange> changes = calcPropertyChanges(namespace, m_configProperties, newConfigProperties);
    Map<String, ConfigChange> changeMap = Maps.uniqueIndex(changes,
        new Function<ConfigChange, String>() {
          @Override
          public String apply(ConfigChange input) {
            return input.getPropertyName();
          }
        });

    updateConfig(newConfigProperties, m_configRepository.getSourceType());
    clearConfigCache();

    this.fireConfigChange(m_namespace, changeMap);

    Tracer.logEvent("Apollo.Client.ConfigChanges", m_namespace);
  }

  private void updateConfig(Properties newConfigProperties, ConfigSourceType sourceType) {
    m_configProperties = newConfigProperties;
    m_sourceType = sourceType;
  }
}
