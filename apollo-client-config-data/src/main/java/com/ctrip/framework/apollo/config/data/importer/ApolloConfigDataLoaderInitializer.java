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
package com.ctrip.framework.apollo.config.data.importer;

import com.ctrip.framework.apollo.config.data.extension.initialize.ApolloClientExtensionInitializeFactory;
import com.ctrip.framework.apollo.config.data.injector.ApolloConfigDataInjectorCustomizer;
import com.ctrip.framework.apollo.config.data.internals.PureApolloConfigFactory;
import com.ctrip.framework.apollo.config.data.system.ApolloClientSystemPropertyInitializer;
import com.ctrip.framework.apollo.config.data.util.Slf4jLogMessageFormatter;
import com.ctrip.framework.apollo.core.utils.DeferredLogger;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
class ApolloConfigDataLoaderInitializer {

  private static volatile boolean INITIALIZED = false;

  private final Log log;

  private final Binder binder;

  private final BindHandler bindHandler;

  private final ConfigurableBootstrapContext bootstrapContext;

  public ApolloConfigDataLoaderInitializer(
      Log log,
      Binder binder,
      BindHandler bindHandler,
      ConfigurableBootstrapContext bootstrapContext) {
    this.log = log;
    this.binder = binder;
    this.bindHandler = bindHandler;
    this.bootstrapContext = bootstrapContext;
  }

  /**
   * init apollo client (only once)
   *
   * @return initial sources as placeholders or empty list if already initialized
   */
  public List<PropertySource<?>> initApolloClient() {
    if (INITIALIZED) {
      return Collections.emptyList();
    }
    synchronized (ApolloConfigDataLoaderInitializer.class) {
      if (INITIALIZED) {
        return Collections.emptyList();
      }
      this.initApolloClientInternal();
      INITIALIZED = true;
      if (this.forceDisableApolloBootstrap()) {
        // force disable apollo bootstrap to avoid conflict
        Map<String, Object> map = new HashMap<>();
        map.put(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED, "false");
        map.put(PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED, "false");
        // provide initial sources as placeholders to avoid duplicate loading
        return Arrays.asList(
            new ApolloConfigEmptyPropertySource(
                PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME),
            new MapPropertySource(
                PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME,
                Collections.unmodifiableMap(map)));
      }
      // provide initial sources as placeholders to avoid duplicate loading
      return Arrays.asList(
          new ApolloConfigEmptyPropertySource(PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME),
          new ApolloConfigEmptyPropertySource(
              PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME));
    }
  }

  private void initApolloClientInternal() {
    new ApolloClientSystemPropertyInitializer(this.log)
        .initializeSystemProperty(this.binder, this.bindHandler);
    new ApolloClientExtensionInitializeFactory(this.log, this.bootstrapContext)
        .initializeExtension(this.binder, this.bindHandler);
    DeferredLogger.enable();
    ApolloConfigDataInjectorCustomizer.register(ConfigFactory.class, PureApolloConfigFactory::new);
  }

  private boolean forceDisableApolloBootstrap() {
    boolean bootstrapEnabled =
        this.binder
            .bind(
                this.camelCasedToKebabCase(PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED),
                Bindable.of(Boolean.class),
                this.bindHandler)
            .orElse(false);
    if (bootstrapEnabled) {
      this.log.warn(
          Slf4jLogMessageFormatter.format(
              "apollo bootstrap is force disabled. please don't configure the property [{}=true] and [spring.config.import=apollo://...] at the same time",
              PropertySourcesConstants.APOLLO_BOOTSTRAP_ENABLED));
      return true;
    }
    boolean bootstrapEagerLoadEnabled =
        this.binder
            .bind(
                this.camelCasedToKebabCase(
                    PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED),
                Bindable.of(Boolean.class),
                this.bindHandler)
            .orElse(false);
    if (bootstrapEagerLoadEnabled) {
      this.log.warn(
          Slf4jLogMessageFormatter.format(
              "apollo bootstrap eager load is force disabled. please don't configure the property [{}=true] and [spring.config.import=apollo://...] at the same time",
              PropertySourcesConstants.APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED));
      return true;
    }
    return false;
  }

  /**
   * {@link ConfigurationPropertyName#isValid(java.lang.CharSequence)}
   *
   * @param source origin propertyName
   * @return valid propertyName
   */
  private String camelCasedToKebabCase(String source) {
    if (ConfigurationPropertyName.isValid(source)) {
      return source;
    }
    StringBuilder stringBuilder = new StringBuilder(source.length() * 2);
    for (char ch : source.toCharArray()) {
      if (Character.isUpperCase(ch)) {
        stringBuilder.append("-").append(Character.toLowerCase(ch));
        continue;
      }
      stringBuilder.append(ch);
    }
    return stringBuilder.toString();
  }
}
