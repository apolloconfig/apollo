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
package com.ctrip.framework.apollo.biz.registry;

import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * decorator pattern
 * <p>
 * 1. use jvm memory as cache to decrease the read of database.
 * <p>
 * 2. when database happened failure, return the cache in jvm memory.
 */
public class DatabaseDiscoveryClientMemoryCacheDecoratorImpl
    implements DatabaseDiscoveryClient {

  private static final Logger log = LoggerFactory.getLogger(
      DatabaseDiscoveryClientMemoryCacheDecoratorImpl.class
  );

  private final DatabaseDiscoveryClient delegate;

  private final Map<String, List<ServiceInstance>> serviceName2ServiceInstances = new ConcurrentHashMap<>(
      8);

  private volatile ScheduledExecutorService scheduledExecutorService;

  private static final long SYNC_TASK_PERIOD_IN_SECOND = 5;

  public DatabaseDiscoveryClientMemoryCacheDecoratorImpl(DatabaseDiscoveryClient delegate) {
    this.delegate = delegate;
  }

  public void init() {
    this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(
        ApolloThreadFactory
            .create("DatabaseDiscoveryWithCache", true)
    );
    scheduledExecutorService.scheduleAtFixedRate(this::updateCacheTask,
        SYNC_TASK_PERIOD_IN_SECOND, SYNC_TASK_PERIOD_IN_SECOND, TimeUnit.SECONDS);

    // load them for init
    try {
      this.getInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE);
    } catch (Throwable t) {
      log.error("fail to get instances of service name {}", ServiceNameConsts.APOLLO_CONFIGSERVICE, t);
    }
    try {
      this.getInstances(ServiceNameConsts.APOLLO_ADMINSERVICE);
    } catch (Throwable t) {
      log.error("fail to get instances of service name {}", ServiceNameConsts.APOLLO_ADMINSERVICE, t);
    }
  }

  void updateCacheTask() {
    try {
      // for each service name, update their service instances in memory
      this.serviceName2ServiceInstances.replaceAll(
          (serviceName, serviceInstances) -> this.delegate.getInstances(serviceName)
      );
    } catch (Throwable t) {
      log.error("fail to read service instances from database", t);
    }
  }

  List<ServiceInstance> readFromDatabase(String serviceName) {
    return this.delegate.getInstances(serviceName);
  }

  /**
   * never throw {@link Throwable}, read from memory cache
   */
  @Override
  public List<ServiceInstance> getInstances(String serviceName) {
    // put serviceName as key to map,
    // then the task use it to read service instances from database
    this.serviceName2ServiceInstances.computeIfAbsent(serviceName, this::readFromDatabase);
    // get from cache
    return this.serviceName2ServiceInstances.getOrDefault(serviceName, Collections.emptyList());
  }
}
