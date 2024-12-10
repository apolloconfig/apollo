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
package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.audit.component.ApolloAuditHttpInterceptor;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.TimeUnit;

@Component
public class RestTemplateFactory implements FactoryBean<RestTemplate>, InitializingBean {

  private final HttpMessageConverters httpMessageConverters;
  private final PortalConfig portalConfig;
  private final ApolloAuditHttpInterceptor apolloAuditHttpInterceptor;

  private RestTemplate restTemplate;

  public RestTemplateFactory(final HttpMessageConverters httpMessageConverters,
      final PortalConfig portalConfig, final ApolloAuditHttpInterceptor apolloAuditHttpInterceptor) {
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
  public void afterPropertiesSet() throws UnsupportedEncodingException {

    PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
    connectionManager.setMaxTotal(portalConfig.connectPoolMaxTotal());
    connectionManager.setDefaultMaxPerRoute(portalConfig.connectPoolMaxPerRoute());

    CloseableHttpClient httpClient = HttpClientBuilder.create()
        .setConnectionTimeToLive(portalConfig.connectionTimeToLive(), TimeUnit.MILLISECONDS)
        .setConnectionManager(connectionManager)
        .build();

    restTemplate = new RestTemplate(httpMessageConverters.getConverters());
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    requestFactory.setConnectTimeout(portalConfig.connectTimeout());
    requestFactory.setReadTimeout(portalConfig.readTimeout());

    restTemplate.setRequestFactory(requestFactory);
    restTemplate.getInterceptors().add(apolloAuditHttpInterceptor);
  }


}
