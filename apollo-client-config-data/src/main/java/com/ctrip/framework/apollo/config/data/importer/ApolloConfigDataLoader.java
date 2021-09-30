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
package com.ctrip.framework.apollo.config.data.importer;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.config.data.util.Slf4jLogMessageFormatter;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySource;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySourceFactory;
import com.ctrip.framework.apollo.spring.util.SpringInjector;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.springframework.boot.BootstrapRegistry.InstanceSupplier;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.config.ConfigData;
import org.springframework.boot.context.config.ConfigDataLoader;
import org.springframework.boot.context.config.ConfigDataLoaderContext;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.PropertySource;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloConfigDataLoader implements ConfigDataLoader<ApolloConfigDataResource>, Ordered {

  private final Log log;

  public ApolloConfigDataLoader(Log log) {
    this.log = log;
  }

  @Override
  public ConfigData load(ConfigDataLoaderContext context, ApolloConfigDataResource resource)
      throws IOException, ConfigDataResourceNotFoundException {
    ConfigurableBootstrapContext bootstrapContext = context.getBootstrapContext();
    Binder binder = bootstrapContext.get(Binder.class);
    BindHandler bindHandler = this.getBindHandler(context);
    bootstrapContext.registerIfAbsent(ApolloConfigDataLoaderInitializer.class, InstanceSupplier
        .from(() -> new ApolloConfigDataLoaderInitializer(this.log, binder, bindHandler,
            bootstrapContext)));
    ApolloConfigDataLoaderInitializer apolloConfigDataLoaderInitializer = bootstrapContext
        .get(ApolloConfigDataLoaderInitializer.class);
    // init apollo client
    List<PropertySource<?>> initialPropertySourceList = apolloConfigDataLoaderInitializer
        .initApolloClient();
    // load config
    bootstrapContext.registerIfAbsent(ConfigPropertySourceFactory.class,
        InstanceSupplier.from(() -> SpringInjector.getInstance(ConfigPropertySourceFactory.class)));
    ConfigPropertySourceFactory configPropertySourceFactory = bootstrapContext
        .get(ConfigPropertySourceFactory.class);
    String namespace = resource.getNamespace();
    Config config = ConfigService.getConfig(namespace);
    ConfigPropertySource configPropertySource = configPropertySourceFactory
        .getConfigPropertySource(namespace, config);
    List<PropertySource<?>> propertySourceList = new ArrayList<>();
    propertySourceList.add(configPropertySource);
    propertySourceList.addAll(initialPropertySourceList);
    log.debug(Slf4jLogMessageFormatter.format("apollo client loaded namespace [{}]", namespace));
    return new ConfigData(propertySourceList);
  }

  private BindHandler getBindHandler(ConfigDataLoaderContext context) {
    return context.getBootstrapContext().getOrElse(BindHandler.class, null);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 100;
  }
}
