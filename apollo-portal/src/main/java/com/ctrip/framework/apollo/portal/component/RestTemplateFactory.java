/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.audit.component.ApolloAuditHttpInterceptor;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import java.time.Duration;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestTemplateFactory implements FactoryBean<RestTemplate>, InitializingBean {

  private final HttpMessageConverters httpMessageConverters;
  private final PortalConfig portalConfig;
  private final ApolloAuditHttpInterceptor apolloAuditHttpInterceptor;

  private RestTemplate restTemplate;

  public RestTemplateFactory(final HttpMessageConverters httpMessageConverters,
      final PortalConfig portalConfig,
      final ApolloAuditHttpInterceptor apolloAuditHttpInterceptor) {
    this.httpMessageConverters = httpMessageConverters;
    this.portalConfig = portalConfig;
    this.apolloAuditHttpInterceptor = apolloAuditHttpInterceptor;
  }

  @Override
  public RestTemplate getObject() {
    return restTemplate;
  }

  @Override
  public Class<RestTemplate> getObjectType() {
    return RestTemplate.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  @Override
  public void afterPropertiesSet() {

    ConnectionConfig connectionConfig = ConnectionConfig.custom()
        .setConnectTimeout(Timeout.ofMilliseconds(portalConfig.connectTimeout()))
        .setTimeToLive(TimeValue.ofMilliseconds(portalConfig.connectionTimeToLive())).build();

    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder
        .create().setMaxConnTotal(portalConfig.connectPoolMaxTotal())
        .setMaxConnPerRoute(portalConfig.connectPoolMaxPerRoute())
        .setDefaultConnectionConfig(connectionConfig).build();

    RequestConfig requestConfig = RequestConfig.custom()
        .setResponseTimeout(Timeout.ofMilliseconds(portalConfig.readTimeout())).build();

    CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig).evictExpiredConnections().build();

    restTemplate = new RestTemplate(httpMessageConverters.getConverters());
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    // Waiting for a pooled connection is distinct from establishing a new socket connection.
    requestFactory.setConnectionRequestTimeout(Duration.ofMillis(portalConfig.connectTimeout()));

    restTemplate.setRequestFactory(requestFactory);
    restTemplate.getInterceptors().add(apolloAuditHttpInterceptor);
  }


}
