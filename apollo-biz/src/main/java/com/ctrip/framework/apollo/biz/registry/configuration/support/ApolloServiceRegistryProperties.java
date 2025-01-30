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

import com.ctrip.framework.apollo.biz.registry.ServiceInstance;
import com.google.common.base.Strings;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
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

  private Map<String, String> metadata = new HashMap<>(8);

  /**
   * heartbeat to registry in second.
   */
  private long heartbeatIntervalInSecond = 10;

  @Autowired
  private PropertyResolver propertyResolver;

  @Autowired
  private InetUtils inetUtils;

  @Autowired
  private ServletContext servletContext;

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
      String contextPath = Strings.isNullOrEmpty(this.servletContext.getContextPath()) ? "/"
          : this.servletContext.getContextPath();
      String uriString = "http://" + host + ":" + port + contextPath;
      this.uri = URI.create(uriString);
    }
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  @Override
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

  @Override
  public Map<String, String> getMetadata() {
    return this.metadata;
  }

  public void setMetadata(Map<String, String> metadata) {
    this.metadata = metadata;
  }

  public long getHeartbeatIntervalInSecond() {
    return heartbeatIntervalInSecond;
  }

  public void setHeartbeatIntervalInSecond(long heartbeatIntervalInSecond) {
    this.heartbeatIntervalInSecond = heartbeatIntervalInSecond;
  }
}
