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

import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.spi.ApolloInjectorCustomizer;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactoryManager;
import com.ctrip.framework.apollo.spi.ConfigRegistry;
import com.ctrip.framework.apollo.spi.DefaultConfigFactory;
import com.ctrip.framework.apollo.spi.DefaultConfigFactoryManager;
import com.ctrip.framework.apollo.spi.DefaultConfigRegistry;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.factory.DefaultPropertiesFactory;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import com.ctrip.framework.apollo.util.http.DefaultHttpClient;
import com.ctrip.framework.apollo.util.http.HttpClient;

import com.ctrip.framework.apollo.util.yaml.YamlParser;
import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * self define injector
 * @author Jason Song(song_s@ctrip.com)
 * @author wxq
 */
public class DefaultInjector implements Injector {

  /**
   * container for injection.
   */
  private final Map<Class<?>, Object> container = new ConcurrentHashMap<>(32);
  private final List<ApolloInjectorCustomizer> m_customizers;

  public DefaultInjector() {
    try {
      m_customizers = ServiceBootstrap.loadAllOrdered(ApolloInjectorCustomizer.class);
    } catch (Throwable ex) {
      ApolloConfigException exception = new ApolloConfigException("Unable to initialize Injector!", ex);
      Tracer.logError(exception);
      throw exception;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T> T getInstance(Class<T> clazz) {
    try {
      for (ApolloInjectorCustomizer customizer : m_customizers) {
        T instance = customizer.getInstance(clazz);
        if (instance != null) {
          return instance;
        }
      }
      return (T) this.getInstanceByHardCode(clazz);
    } catch (Throwable ex) {
      Tracer.logError(ex);
      throw new ApolloConfigException(
          String.format("Unable to load instance for %s!", clazz.getName()), ex);
    }
  }

  /**
   * inject singleton manually by hard code.
   * <p>
   * watch out {@link IllegalStateException} Recursive update when invoke {@link Map#computeIfAbsent(Object, Function)} nested.
   */
  private Object getInstanceByHardCode(Class<?> clazz) {
    if (!this.container.containsKey(clazz)) {
      if (ConfigRegistry.class.equals(clazz)) {
        this.container.computeIfAbsent(ConfigRegistry.class, key -> new DefaultConfigRegistry());
      }
      if (ConfigFactoryManager.class.equals(clazz)) {
        ConfigRegistry configRegistry = this.getInstance(ConfigRegistry.class);
        this.container.computeIfAbsent(ConfigFactoryManager.class,
            key -> new DefaultConfigFactoryManager(configRegistry));
      }
      if (ConfigManager.class.equals(clazz)) {
        ConfigFactoryManager configFactoryManager = this.getInstance(ConfigFactoryManager.class);
        this.container.computeIfAbsent(ConfigManager.class,
            key -> new DefaultConfigManager(configFactoryManager));
      }
      if (ConfigUtil.class.equals(clazz)) {
        this.container.computeIfAbsent(ConfigUtil.class, key -> new ConfigUtil());
      }
      if (ConfigFactory.class.equals(clazz)) {
        ConfigUtil configUtil = this.getInstance(ConfigUtil.class);
        this.container.computeIfAbsent(ConfigFactory.class,
            key -> new DefaultConfigFactory(configUtil));
      }
      if (HttpClient.class.equals(clazz)) {
        ConfigUtil configUtil = this.getInstance(ConfigUtil.class);
        this.container.computeIfAbsent(HttpClient.class, key -> new DefaultHttpClient(configUtil));
      }
      if (ConfigServiceLocator.class.equals(clazz)) {
        HttpClient httpClient = this.getInstance(HttpClient.class);
        ConfigUtil configUtil = this.getInstance(ConfigUtil.class);
        this.container.computeIfAbsent(ConfigServiceLocator.class,
            key -> new ConfigServiceLocator(httpClient, configUtil));
      }
      if (RemoteConfigLongPollService.class.equals(clazz)) {
        ConfigUtil configUtil = this.getInstance(ConfigUtil.class);
        HttpClient httpClient = this.getInstance(HttpClient.class);
        ConfigServiceLocator configServiceLocator = this.getInstance(ConfigServiceLocator.class);
        this.container.computeIfAbsent(RemoteConfigLongPollService.class,
            key -> new RemoteConfigLongPollService(configUtil, httpClient, configServiceLocator));
      }
      if (PropertiesFactory.class.equals(clazz)) {
        ConfigUtil configUtil = this.getInstance(ConfigUtil.class);
        this.container.computeIfAbsent(PropertiesFactory.class,
            key -> new DefaultPropertiesFactory(configUtil));
      }
      if (YamlParser.class.equals(clazz)) {
        PropertiesFactory propertiesFactory = this.getInstance(PropertiesFactory.class);
        this.container.computeIfAbsent(YamlParser.class, key -> new YamlParser(propertiesFactory));
      }
    }
    return this.container.get(clazz);
  }

  @Override
  public <T> T getInstance(Class<T> clazz, String name) {
    try {
      for (ApolloInjectorCustomizer customizer : m_customizers) {
        T instance = customizer.getInstance(clazz, name);
        if (instance != null) {
          return instance;
        }
      }
      //does not support get instance by type and name
      return null;
    } catch (Throwable ex) {
      Tracer.logError(ex);
      throw new ApolloConfigException(
          String.format("Unable to load instance for %s with name %s!", clazz.getName(), name), ex);
    }
  }
}
