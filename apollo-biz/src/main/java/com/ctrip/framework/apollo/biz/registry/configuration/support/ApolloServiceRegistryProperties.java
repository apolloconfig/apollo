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

import com.ctrip.framework.apollo.biz.registry.ServiceInstance;
import java.net.URI;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.core.env.PropertyResolver;

/**
 * config of register.
 *
 * @see com.ctrip.framework.apollo.core.dto.ServiceDTO
 * @see org.springframework.cloud.netflix.eureka.EurekaClientConfigBean
 * @see org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean
 */
@ConfigurationProperties(prefix = ApolloServiceRegistryProperties.PREFIX)
public class ApolloServiceRegistryProperties implements ServiceInstance {

  public static final String PREFIX = "apollo.service.registry";

  /**
   * register self to registry or not
   */
  private boolean enabled;

  /**
   * @see com.ctrip.framework.apollo.core.ServiceNameConsts#APOLLO_CONFIGSERVICE
   * @see com.ctrip.framework.apollo.core.ServiceNameConsts#APOLLO_ADMINSERVICE
   */
  private String serviceName;

  /**
   * @see ServiceInstance#getUri()
   */
  private URI uri;

  /**
   * @see ServiceInstance#getCluster()
   */
  private String cluster;

  /**
   * heartbeat to registry in second.
   */
  private long heartbeatIntervalInSecond = 10;

  @Autowired
  private PropertyResolver propertyResolver;

  @Autowired
  private InetUtils inetUtils;

  /**
   * if user doesn't config, then resolve them on the runtime.
   */
  @PostConstruct
  public void postConstruct() {
    if (this.serviceName == null) {
      this.serviceName = propertyResolver.getRequiredProperty("spring.application.name");
    }

    if (this.uri == null) {
      String host = this.inetUtils.findFirstNonLoopbackHostInfo().getIpAddress();
      Integer port = propertyResolver.getRequiredProperty("server.port", Integer.class);
      String uriString = "http://" + host + ":" + port + "/";
      this.uri = URI.create(uriString);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getServiceName() {
    return serviceName;
  }

  @Override
  public URI getUri() {
    return this.uri;
  }

  /**
   * custom the uri
   * @see ServiceInstance#getUri()
   */
  public void setUri(String uri) {
    this.uri = URI.create(uri);
  }

  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public String getCluster() {
    return cluster;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public long getHeartbeatIntervalInSecond() {
    return heartbeatIntervalInSecond;
  }

  public void setHeartbeatIntervalInSecond(long heartbeatIntervalInSecond) {
    this.heartbeatIntervalInSecond = heartbeatIntervalInSecond;
  }
}
