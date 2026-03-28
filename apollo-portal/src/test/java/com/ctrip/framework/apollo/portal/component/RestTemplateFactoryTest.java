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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.audit.component.ApolloAuditHttpInterceptor;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import java.util.Collections;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.HttpRoute;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.Configurable;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
import org.springframework.http.client.AbstractClientHttpRequestFactoryWrapper;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link RestTemplateFactory} timeout wiring.
 */
@ExtendWith(MockitoExtension.class)
public class RestTemplateFactoryTest {

  @Mock
  private HttpMessageConverters httpMessageConverters;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private ApolloAuditHttpInterceptor apolloAuditHttpInterceptor;

  private RestTemplateFactory restTemplateFactory;

  @BeforeEach
  public void setUp() {
    when(httpMessageConverters.getConverters())
        .thenReturn(Collections.singletonList(new StringHttpMessageConverter()));
    when(portalConfig.connectPoolMaxTotal()).thenReturn(20);
    when(portalConfig.connectPoolMaxPerRoute()).thenReturn(2);
    when(portalConfig.connectionTimeToLive()).thenReturn(60_000);
    when(portalConfig.connectTimeout()).thenReturn(3_000);
    when(portalConfig.readTimeout()).thenReturn(10_000);

    restTemplateFactory = new RestTemplateFactory(httpMessageConverters, portalConfig,
        apolloAuditHttpInterceptor);
  }

  @Test
  public void shouldSplitTimeoutConfigurationBetweenHttpClientAndRequestFactory() {
    restTemplateFactory.afterPropertiesSet();

    RestTemplate restTemplate = restTemplateFactory.getObject();
    HttpComponentsClientHttpRequestFactory requestFactory =
        (HttpComponentsClientHttpRequestFactory) ((AbstractClientHttpRequestFactoryWrapper) restTemplate
            .getRequestFactory()).getDelegate();
    HttpClient httpClient = requestFactory.getHttpClient();
    RequestConfig requestConfig = ((Configurable) httpClient).getConfig();
    PoolingHttpClientConnectionManager connectionManager =
        (PoolingHttpClientConnectionManager) ReflectionTestUtils.getField(httpClient,
            "connManager");
    ConnectionConfig connectionConfig = ReflectionTestUtils.invokeMethod(connectionManager,
        "resolveConnectionConfig", new HttpRoute(new HttpHost("http", "localhost", 80)));

    assertSame(apolloAuditHttpInterceptor, restTemplate.getInterceptors().get(0));
    assertEquals(3_000, connectionConfig.getConnectTimeout().toMilliseconds());
    assertEquals(60_000, connectionConfig.getTimeToLive().toMilliseconds());
    assertEquals(10_000, requestConfig.getResponseTimeout().toMilliseconds());
    assertEquals(3_000L, ReflectionTestUtils.getField(requestFactory, "connectionRequestTimeout"));
    assertEquals(-1L, ReflectionTestUtils.getField(requestFactory, "readTimeout"));
  }
}
