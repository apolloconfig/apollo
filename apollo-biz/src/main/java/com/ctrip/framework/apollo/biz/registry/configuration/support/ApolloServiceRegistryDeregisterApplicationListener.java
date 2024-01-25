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
package com.ctrip.framework.apollo.biz.registry.configuration.support;

import com.ctrip.framework.apollo.biz.registry.DatabaseServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * remove self before shutdown
 */
public class ApolloServiceRegistryDeregisterApplicationListener
  implements ApplicationListener<ContextClosedEvent> {

  private static final Logger log = LoggerFactory
      .getLogger(ApolloServiceRegistryDeregisterApplicationListener.class);
  private final ApolloServiceRegistryProperties registration;

  private final DatabaseServiceRegistry serviceRegistry;

  public ApolloServiceRegistryDeregisterApplicationListener(
      ApolloServiceRegistryProperties registration, DatabaseServiceRegistry serviceRegistry) {
    this.registration = registration;
    this.serviceRegistry = serviceRegistry;
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    this.deregister();
  }

  private void deregister() {
    try {
      this.serviceRegistry.deregister(this.registration);
      log.info(
          "deregister success, '{}' uri '{}', cluster '{}'",
          this.registration.getServiceName(),
          this.registration.getUri(),
          this.registration.getCluster()
      );
    } catch (Throwable t) {
      log.error(
          "deregister fail, '{}' uri '{}',  cluster '{}'",
          this.registration.getServiceName(),
          this.registration.getUri(),
          this.registration.getCluster(),
          t
      );
    }
  }
}
