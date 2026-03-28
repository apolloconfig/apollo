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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.AdminServiceTestConfiguration;
import org.junit.After;
import org.junit.Before;
import org.mockito.MockitoAnnotations;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.converter.autoconfigure.HttpMessageConverters;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AdminServiceTestConfiguration.class,
    webEnvironment = WebEnvironment.RANDOM_PORT)
public abstract class AbstractControllerTest {

  @Autowired
  private HttpMessageConverters httpMessageConverters;

  private AutoCloseable mocks;
  protected RestTemplate restTemplate = new RestTemplate();

  @PostConstruct
  private void postConstruct() {
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
    restTemplate.setMessageConverters(httpMessageConverters.getConverters());
  }

  @Before
  public void openMocks() {
    mocks = MockitoAnnotations.openMocks(this);
  }

  @After
  public void closeMocks() throws Exception {
    if (mocks != null) {
      mocks.close();
    }
  }

  @Value("${local.server.port}")
  protected int port;

  protected String url(String path) {
    return "http://localhost:" + port + path;
  }

  protected String namespaceBaseUrl() {
    return url("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName:.+}");
  }

  protected String appBaseUrl() {
    return url("/apps/{appId}");
  }

  protected String clusterBaseUrl() {
    return url("/apps/{appId}/clusters/{clusterName}");
  }

  protected String itemBaseUrl() {
    return url("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items");
  }
}
