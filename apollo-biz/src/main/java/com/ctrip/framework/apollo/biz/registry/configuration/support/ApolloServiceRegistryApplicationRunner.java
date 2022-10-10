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

import com.ctrip.framework.apollo.biz.registry.DatabaseServiceRegistry;
import com.ctrip.framework.apollo.biz.registry.ServiceInstance;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public class ApolloServiceRegistryApplicationRunner
    implements ApplicationRunner {

  private static final Logger log = LoggerFactory
      .getLogger(ApolloServiceRegistryApplicationRunner.class);

  private final ApolloServiceRegistryProperties registration;

  private final DatabaseServiceRegistry serviceRegistry;

  /**
   * for {@link #heartbeat()}
   */
  private final ScheduledExecutorService heartbeatScheduledExecutorService;

  public ApolloServiceRegistryApplicationRunner(
      ApolloServiceRegistryProperties registration,
      DatabaseServiceRegistry serviceRegistry
  ) {
    this.registration = registration;
    this.serviceRegistry = serviceRegistry;
    this.heartbeatScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ApolloThreadFactory.create("ApolloServiceRegistryHeartBeat", true)
    );
  }

  @Override
  public void run(ApplicationArguments args) throws Exception {
    // register
    log.info(
        "register to database. '{}': uri '{}', label '{}' ",
        this.registration.getServiceName(),
        this.registration.getUri(),
        this.registration.getCluster()
    );
    // heartbeat as same as register
    this.heartbeatScheduledExecutorService
        .scheduleAtFixedRate(this::heartbeat, 0, this.registration.getHeartbeatIntervalInSecond(),
            TimeUnit.SECONDS);
  }

  @PreDestroy
  private void deregister() {
    try {
      this.serviceRegistry.deregister(this.registration);
      log.info(
          "deregister success, '{}' uri '{}', label '{}'",
          this.registration.getServiceName(),
          this.registration.getUri(),
          this.registration.getCluster()
      );
    } catch (Exception e) {
      log.error(
          "deregister fail, '{}' uri '{}',  label '{}'",
          this.registration.getServiceName(),
          this.registration.getUri(),
          this.registration.getCluster(),
          e
      );
    }
  }

  private void heartbeat() {
    try {
      this.serviceRegistry.register(this.registration);
    } catch (Exception e) {
      log.error("fail to send heartbeat by scheduled task", e);
    }
  }

}
