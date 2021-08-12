/*
 * Copyright 2021 Apollo Authors
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ReleaseOpenApiServiceTest extends AbstractOpenApiServiceTest {

  private ReleaseOpenApiService releaseOpenApiService;

  private String someAppId;
  private String someEnv;
  private String someCluster;
  private String someNamespace;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    someAppId = "someAppId";
    someEnv = "someEnv";
    someCluster = "someCluster";
    someNamespace = "someNamespace";

    StringEntity responseEntity = new StringEntity("{}");
    when(someHttpResponse.getEntity()).thenReturn(responseEntity);

    releaseOpenApiService = new ReleaseOpenApiService(httpClient, someBaseUrl, gson);
  }

  @Test
  public void testPublishNamespace() throws Exception {
    String someReleaseTitle = "someReleaseTitle";
    String someReleasedBy = "someReleasedBy";

    NamespaceReleaseDTO namespaceReleaseDTO = new NamespaceReleaseDTO();
    namespaceReleaseDTO.setReleaseTitle(someReleaseTitle);
    namespaceReleaseDTO.setReleasedBy(someReleasedBy);

    final ArgumentCaptor<HttpPost> request = ArgumentCaptor.forClass(HttpPost.class);

    releaseOpenApiService.publishNamespace(someAppId, someEnv, someCluster, someNamespace, namespaceReleaseDTO);

    verify(httpClient, times(1)).execute(request.capture());

    HttpPost post = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/releases", someBaseUrl, someEnv, someAppId, someCluster,
            someNamespace), post.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testPublishNamespaceWithError() throws Exception {
    String someReleaseTitle = "someReleaseTitle";
    String someReleasedBy = "someReleasedBy";

    NamespaceReleaseDTO namespaceReleaseDTO = new NamespaceReleaseDTO();
    namespaceReleaseDTO.setReleaseTitle(someReleaseTitle);
    namespaceReleaseDTO.setReleasedBy(someReleasedBy);

    when(statusLine.getStatusCode()).thenReturn(400);

    releaseOpenApiService.publishNamespace(someAppId, someEnv, someCluster, someNamespace, namespaceReleaseDTO);
  }

  @Test
  public void testGetLatestActiveRelease() throws Exception {
    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    releaseOpenApiService.getLatestActiveRelease(someAppId, someEnv, someCluster, someNamespace);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/releases/latest", someBaseUrl, someEnv, someAppId, someCluster,
            someNamespace), get.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testGetLatestActiveReleaseWithError() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(400);

    releaseOpenApiService.getLatestActiveRelease(someAppId, someEnv, someCluster, someNamespace);
  }

  @Test
  public void testRollbackRelease() throws Exception {
    long someReleaseId = 1L;
    String someOperator = "someOperator";

    final ArgumentCaptor<HttpPut> request = ArgumentCaptor.forClass(HttpPut.class);

    releaseOpenApiService.rollbackRelease(someEnv, someReleaseId, someOperator);

    verify(httpClient, times(1)).execute(request.capture());

    HttpPut put = request.getValue();

    assertEquals(
        String.format("%s/envs/%s/releases/%s/rollback?operator=%s", someBaseUrl, someEnv, someReleaseId, someOperator),
        put.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testRollbackReleaseWithError() throws Exception {
    long someReleaseId = 1L;
    String someOperator = "someOperator";

    when(statusLine.getStatusCode()).thenReturn(400);

    releaseOpenApiService.rollbackRelease(someEnv, someReleaseId, someOperator);
  }
}
