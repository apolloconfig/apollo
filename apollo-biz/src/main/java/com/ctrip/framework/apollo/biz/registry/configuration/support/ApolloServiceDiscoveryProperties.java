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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @see org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties
 * @see org.springframework.cloud.consul.ConsulProperties
 * @see org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean
 */
@ConfigurationProperties(prefix = ApolloServiceDiscoveryProperties.PREFIX)
public class ApolloServiceDiscoveryProperties {

  public static final String PREFIX = "apollo.service.discovery";

  /**
   * enable discovery of registry or not
   */
  private boolean enabled = false;

  /**
   * health check interval.
   * <p>
   * if current time - the last time of instance's heartbeat < healthCheckInterval,
   * <p>
   * then this instance is healthy.
   */
  private long healthCheckIntervalInSecond = 61;

  public long getHealthCheckIntervalInSecond() {
    return healthCheckIntervalInSecond;
  }

  public void setHealthCheckIntervalInSecond(long healthCheckIntervalInSecond) {
    this.healthCheckIntervalInSecond = healthCheckIntervalInSecond;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

}
