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

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @see org.springframework.cloud.consul.discovery.ConsulDiscoveryProperties
 * @see org.springframework.cloud.consul.ConsulProperties
 * @see org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean
 */
@ConfigurationProperties(prefix = ApolloRegistryDiscoveryProperties.PREFIX)
public class ApolloRegistryDiscoveryProperties {

  public static final String PREFIX = "apollo.registry.discovery";

  /**
   * enable discovery of registry or not
   */
  private boolean enabled = false;

  /**
   * true mean only return instances which have same label as self.
   * false mean return all instances without filter by label.
   */
  private boolean filterByLabel = true;

  /**
   * health check interval.
   * <p>
   * if current time - the last time of instance's heartbeat < healthCheckInterval,
   * <p>
   * then this instance is healthy.
   */
  private long healthCheckInterval = 61;

  public boolean isFilterByLabel() {
    return filterByLabel;
  }

  public void setFilterByLabel(boolean filterByLabel) {
    this.filterByLabel = filterByLabel;
  }

  public long getHealthCheckInterval() {
    return healthCheckInterval;
  }

  public void setHealthCheckInterval(long healthCheckInterval) {
    this.healthCheckInterval = healthCheckInterval;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

}
