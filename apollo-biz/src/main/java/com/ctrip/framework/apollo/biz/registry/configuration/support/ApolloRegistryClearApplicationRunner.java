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
package com.ctrip.framework.apollo.biz.registry.configuration.support;

import com.ctrip.framework.apollo.biz.entity.Registry;
import com.ctrip.framework.apollo.biz.service.RegistryService;
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
public class ApolloRegistryClearApplicationRunner
  implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(ApolloRegistryClearApplicationRunner.class);

  /**
   * for {@link #clearUnhealthyInstances()}
   */
  private final ScheduledExecutorService instanceClearScheduledExecutorService;


  private final RegistryService registryService;

  public ApolloRegistryClearApplicationRunner(
      RegistryService registryService) {
    this.registryService = registryService;
    this.instanceClearScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ApolloThreadFactory.create("ApolloRegistryServerClearInstances", true)
    );
  }

  /**
   * clear instance
   */
  private void clearUnhealthyInstances() {
    try {
      List<Registry> registryListDeleted =
          this.registryService.deleteTimeBefore(Duration.ofMinutes(10));
      if (registryListDeleted != null && !registryListDeleted.isEmpty()) {
        log.info("clear {} unhealthy instances by scheduled task", registryListDeleted.size());
      }
    } catch (Exception e) {
      log.error("fail to clear unhealthy instances by scheduled task", e);
    }
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    this.instanceClearScheduledExecutorService.scheduleAtFixedRate(this::clearUnhealthyInstances, 0, 60, TimeUnit.SECONDS);
  }
}
