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
package com.ctrip.framework.apollo.openapi.client.service;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class AppOpenApiServiceTest extends AbstractOpenApiServiceTest {

  private AppOpenApiService appOpenApiService;

  private String someAppId;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    someAppId = "someAppId";

    StringEntity responseEntity = new StringEntity("[]");
    when(someHttpResponse.getEntity()).thenReturn(responseEntity);

    appOpenApiService = new AppOpenApiService(httpClient, someBaseUrl, gson);
  }

  @Test
  public void testGetEnvClusterInfo() throws Exception {
    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    appOpenApiService.getEnvClusterInfo(someAppId);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(String
        .format("%s/apps/%s/envclusters", someBaseUrl, someAppId), get.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testGetEnvClusterInfoWithError() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(500);

    appOpenApiService.getEnvClusterInfo(someAppId);
  }
}
