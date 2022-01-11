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
package com.ctrip.framework.apollo.config.data.extension.initialize;

import com.ctrip.framework.apollo.config.data.extension.enums.ApolloClientMessagingType;
import com.ctrip.framework.apollo.config.data.extension.properties.ApolloClientExtensionProperties;
import com.ctrip.framework.apollo.config.data.extension.properties.ApolloClientProperties;
import com.ctrip.framework.apollo.config.data.extension.webclient.ApolloClientLongPollingExtensionInitializer;
import com.ctrip.framework.apollo.config.data.extension.websocket.ApolloClientWebsocketExtensionInitializer;
import com.ctrip.framework.apollo.config.data.util.Slf4jLogMessageFormatter;
import org.apache.commons.logging.Log;
import org.springframework.boot.ConfigurableBootstrapContext;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Binder;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientExtensionInitializeFactory {

  private final Log log;

  private final ApolloClientPropertiesFactory apolloClientPropertiesFactory;

  private final ApolloClientLongPollingExtensionInitializer apolloClientLongPollingExtensionInitializer;

  private final ApolloClientWebsocketExtensionInitializer apolloClientWebsocketExtensionInitializer;

  public ApolloClientExtensionInitializeFactory(Log log,
      ConfigurableBootstrapContext bootstrapContext) {
    this.log = log;
    this.apolloClientPropertiesFactory = new ApolloClientPropertiesFactory();
    this.apolloClientLongPollingExtensionInitializer = new ApolloClientLongPollingExtensionInitializer(log,
        bootstrapContext);
    this.apolloClientWebsocketExtensionInitializer = new ApolloClientWebsocketExtensionInitializer(log,
        bootstrapContext);
  }

  /**
   * initialize extension
   *
   * @param binder      properties binder
   * @param bindHandler properties bind handler
   */
  public void initializeExtension(Binder binder, BindHandler bindHandler) {
    ApolloClientProperties apolloClientProperties = this.apolloClientPropertiesFactory
        .createApolloClientProperties(binder, bindHandler);
    if (apolloClientProperties == null || apolloClientProperties.getExtension() == null) {
      this.log.info("apollo client extension is not configured, default to disabled");
      return;
    }
    ApolloClientExtensionProperties extension = apolloClientProperties.getExtension();
    if (!extension.getEnabled()) {
      this.log.info("apollo client extension disabled");
      return;
    }
    ApolloClientMessagingType messagingType = extension.getMessagingType();
    log.debug(Slf4jLogMessageFormatter
        .format("apollo client extension messaging type: {}", messagingType));
    switch (messagingType) {
      case LONG_POLLING:
        this.apolloClientLongPollingExtensionInitializer
            .initialize(apolloClientProperties, binder, bindHandler);
        return;
      case WEBSOCKET:
        this.apolloClientWebsocketExtensionInitializer
            .initialize(apolloClientProperties, binder, bindHandler);
        return;
      default:
        throw new IllegalStateException("Unexpected value: " + messagingType);
    }
  }
}
