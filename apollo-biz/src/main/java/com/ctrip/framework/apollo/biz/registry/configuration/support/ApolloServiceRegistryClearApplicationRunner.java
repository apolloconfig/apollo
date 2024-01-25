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

import com.ctrip.framework.apollo.biz.entity.ServiceRegistry;
import com.ctrip.framework.apollo.biz.service.ServiceRegistryService;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * clear the unhealthy instances.
 */
public class ApolloServiceRegistryClearApplicationRunner
  implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(
      ApolloServiceRegistryClearApplicationRunner.class);

  /**
   * for {@link #clearUnhealthyInstances()}
   */
  private final ScheduledExecutorService instanceClearScheduledExecutorService;


  private final ServiceRegistryService serviceRegistryService;

  public ApolloServiceRegistryClearApplicationRunner(
      ServiceRegistryService serviceRegistryService) {
    this.serviceRegistryService = serviceRegistryService;
    this.instanceClearScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ApolloThreadFactory.create("ApolloRegistryServerClearInstances", true)
    );
  }

  /**
   * clear instance
   */
  void clearUnhealthyInstances() {
    try {
      List<ServiceRegistry> serviceRegistryListDeleted =
          this.serviceRegistryService.deleteTimeBefore(Duration.ofDays(1));
      if (serviceRegistryListDeleted != null && !serviceRegistryListDeleted.isEmpty()) {
        log.info("clear {} unhealthy instances by scheduled task", serviceRegistryListDeleted.size());
      }
    } catch (Throwable t) {
      log.error("fail to clear unhealthy instances by scheduled task", t);
    }
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    this.instanceClearScheduledExecutorService.scheduleAtFixedRate(this::clearUnhealthyInstances, 0, 1, TimeUnit.DAYS);
  }
}
