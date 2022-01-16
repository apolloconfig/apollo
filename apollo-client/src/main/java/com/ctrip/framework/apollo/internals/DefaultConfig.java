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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.core.utils.DeferredLoggerFactory;
import com.ctrip.framework.apollo.enums.ConfigSourceType;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfig extends AbstractConfig implements RepositoryChangeListener {

  private static final Logger logger = DeferredLoggerFactory.getLogger(DefaultConfig.class);
  private final String m_namespace;
  private final Properties m_resourceProperties;
  private final AtomicReference<Properties> m_configProperties;
  private final ConfigRepository m_configRepository;
  private final RateLimiter m_warnLogRateLimiter;

  private volatile ConfigSourceType m_sourceType = ConfigSourceType.NONE;

  /**
   * Constructor.
   *
   * @param namespace        the namespace of this config instance
   * @param configRepository the config repository for this config instance
   */
  public DefaultConfig(String namespace, ConfigRepository configRepository) {
    m_namespace = namespace;
    m_resourceProperties = loadFromResource(m_namespace);
    m_configRepository = configRepository;
    m_configProperties = new AtomicReference<>();
    m_warnLogRateLimiter = RateLimiter.create(0.017); // 1 warning log output per minute
    initialize();
  }

  private void initialize() {
    try {
      updateConfig(m_configRepository.getConfig(), m_configRepository.getSourceType());
    } catch (Throwable ex) {
      Tracer.logError(ex);
      logger.warn(
          "Init Apollo Local Config failed - namespace: {}, reason: {}.",
          m_namespace,
          ExceptionUtil.getDetailMessage(ex));
    } finally {
      // register the change listener no matter config repository is working or not
      // so that whenever config repository is recovered, config could get changed
      m_configRepository.addChangeListener(this);
    }
  }

  /**
   * get property from cached repository properties file
   *
   * @param key property key
   * @return value
   */
  protected String getPropertyFromRepository(String key) {
    Properties properties = m_configProperties.get();
    if (properties != null) {
      return properties.getProperty(key);
    }
    return null;
  }

  /**
   * get property from additional properties file on classpath
   *
   * @param key property key
   * @return value
   */
  protected String getPropertyFromAdditional(String key) {
    Properties properties = this.m_resourceProperties;
    if (properties != null) {
      return properties.getProperty(key);
    }
    return null;
  }

  /**
   * try to print a warn log when can not find a property
   *
   * @param value value
   */
  protected void tryWarnLog(String value) {
    if (value == null && m_configProperties.get() == null && m_warnLogRateLimiter.tryAcquire()) {
      logger.warn(
          "Could not load config for namespace {} from Apollo, please check whether the configs are released in Apollo! Return default value now!",
          m_namespace);
    }
  }

  /**
   * get property names from cached repository properties file
   *
   * @return property names
   */
  protected Set<String> getPropertyNamesFromRepository() {
    Properties properties = m_configProperties.get();
    if (properties == null) {
      return Collections.emptySet();
    }
    return this.stringPropertyNames(properties);
  }

  /**
   * get property names from additional properties file on classpath
   *
   * @return property names
   */
  protected Set<String> getPropertyNamesFromAdditional() {
    Properties properties = m_resourceProperties;
    if (properties == null) {
      return Collections.emptySet();
    }
    return this.stringPropertyNames(properties);
  }

  @Override
  public String getProperty(String key, String defaultValue) {
    // step 1: check system properties, i.e. -Dkey=value
    String value = System.getProperty(key);

    // step 2: check local cached properties file
    if (value == null) {
      value = this.getPropertyFromRepository(key);
    }

    /*
     * step 3: check env variable, i.e. PATH=...
     * normally system environment variables are in UPPERCASE, however there might be exceptions.
     * so the caller should provide the key in the right case
     */
    if (value == null) {
      value = System.getenv(key);
    }

    // step 4: check properties file from classpath
    if (value == null) {
      value = this.getPropertyFromAdditional(key);
    }

    this.tryWarnLog(value);

    return value == null ? defaultValue : value;
  }

  @Override
  public Set<String> getPropertyNames() {
    // propertyNames include system property and system env might cause some compatibility issues,
    // though that looks like the correct implementation.
    Set<String> fromRepository = this.getPropertyNamesFromRepository();
    Set<String> fromAdditional = this.getPropertyNamesFromAdditional();
    if (fromRepository == null || fromRepository.isEmpty()) {
      return fromAdditional;
    }
    if (fromAdditional == null || fromAdditional.isEmpty()) {
      return fromRepository;
    }
    Set<String> propertyNames =
        Sets.newLinkedHashSetWithExpectedSize(fromRepository.size() + fromAdditional.size());
    propertyNames.addAll(fromRepository);
    propertyNames.addAll(fromAdditional);
    return propertyNames;
  }

  @Override
  public ConfigSourceType getSourceType() {
    return m_sourceType;
  }

  private Set<String> stringPropertyNames(Properties properties) {
    // jdk9以下版本Properties#enumerateStringProperties方法存在性能问题，keys() + get(k) 重复迭代,
    // jdk9之后改为entrySet遍历.
    Map<String, String> h = Maps.newLinkedHashMapWithExpectedSize(properties.size());
    for (Map.Entry<Object, Object> e : properties.entrySet()) {
      Object k = e.getKey();
      Object v = e.getValue();
      if (k instanceof String && v instanceof String) {
        h.put((String) k, (String) v);
      }
    }
    return h.keySet();
  }

  @Override
  public synchronized void onRepositoryChange(String namespace, Properties newProperties) {
    if (newProperties.equals(m_configProperties.get())) {
      return;
    }

    ConfigSourceType sourceType = m_configRepository.getSourceType();
    Properties newConfigProperties = propertiesFactory.getPropertiesInstance();
    newConfigProperties.putAll(newProperties);

    Map<String, ConfigChange> actualChanges =
        updateAndCalcConfigChanges(newConfigProperties, sourceType);

    // check double checked result
    if (actualChanges.isEmpty()) {
      return;
    }

    this.fireConfigChange(m_namespace, actualChanges);

    Tracer.logEvent("Apollo.Client.ConfigChanges", m_namespace);
  }

  private void updateConfig(Properties newConfigProperties, ConfigSourceType sourceType) {
    m_configProperties.set(newConfigProperties);
    m_sourceType = sourceType;
  }

  private Map<String, ConfigChange> updateAndCalcConfigChanges(
      Properties newConfigProperties, ConfigSourceType sourceType) {
    List<ConfigChange> configChanges =
        calcPropertyChanges(m_namespace, m_configProperties.get(), newConfigProperties);

    ImmutableMap.Builder<String, ConfigChange> actualChanges = new ImmutableMap.Builder<>();

    /** === Double check since DefaultConfig has multiple config sources ==== **/

    // 1. use getProperty to update configChanges's old value
    for (ConfigChange change : configChanges) {
      change.setOldValue(this.getProperty(change.getPropertyName(), change.getOldValue()));
    }

    // 2. update m_configProperties
    updateConfig(newConfigProperties, sourceType);
    clearConfigCache();

    // 3. use getProperty to update configChange's new value and calc the final changes
    for (ConfigChange change : configChanges) {
      change.setNewValue(this.getProperty(change.getPropertyName(), change.getNewValue()));
      switch (change.getChangeType()) {
        case ADDED:
          if (Objects.equals(change.getOldValue(), change.getNewValue())) {
            break;
          }
          if (change.getOldValue() != null) {
            change.setChangeType(PropertyChangeType.MODIFIED);
          }
          actualChanges.put(change.getPropertyName(), change);
          break;
        case MODIFIED:
          if (!Objects.equals(change.getOldValue(), change.getNewValue())) {
            actualChanges.put(change.getPropertyName(), change);
          }
          break;
        case DELETED:
          if (Objects.equals(change.getOldValue(), change.getNewValue())) {
            break;
          }
          if (change.getNewValue() != null) {
            change.setChangeType(PropertyChangeType.MODIFIED);
          }
          actualChanges.put(change.getPropertyName(), change);
          break;
        default:
          // do nothing
          break;
      }
    }
    return actualChanges.build();
  }

  private Properties loadFromResource(String namespace) {
    String name = String.format("META-INF/config/%s.properties", namespace);
    InputStream in = ClassLoaderUtil.getLoader().getResourceAsStream(name);
    Properties properties = null;

    if (in != null) {
      properties = propertiesFactory.getPropertiesInstance();

      try {
        properties.load(in);
      } catch (IOException ex) {
        Tracer.logError(ex);
        logger.error("Load resource config for namespace {} failed", namespace, ex);
      } finally {
        try {
          in.close();
        } catch (IOException ex) {
          // ignore
        }
      }
    }

    return properties;
  }
}
