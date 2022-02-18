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
package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;

@Component
public class RestTemplateFactory implements FactoryBean<RestTemplate>, InitializingBean {

  @Autowired
  private HttpMessageConverters httpMessageConverters;
  @Autowired
  private PortalConfig portalConfig;

  private RestTemplate restTemplate;

  public RestTemplate getObject() {
    return restTemplate;
  }

  public Class<RestTemplate> getObjectType() {
    return RestTemplate.class;
  }

  public boolean isSingleton() {
    return true;
  }

  public void afterPropertiesSet() throws UnsupportedEncodingException {
    CloseableHttpClient httpClient = HttpClientBuilder.create().build();

    restTemplate = new RestTemplate(httpMessageConverters.getConverters());
    HttpComponentsClientHttpRequestFactory requestFactory =
        new HttpComponentsClientHttpRequestFactory(httpClient);
    requestFactory.setConnectTimeout(portalConfig.connectTimeout());
    requestFactory.setReadTimeout(portalConfig.readTimeout());

    restTemplate.setRequestFactory(requestFactory);
  }


}
