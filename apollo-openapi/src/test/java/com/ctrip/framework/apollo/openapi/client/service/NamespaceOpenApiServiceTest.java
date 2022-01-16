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

import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class NamespaceOpenApiServiceTest extends AbstractOpenApiServiceTest {

  private NamespaceOpenApiService namespaceOpenApiService;

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

    namespaceOpenApiService = new NamespaceOpenApiService(httpClient, someBaseUrl, gson);
  }

  @Test
  public void testGetNamespace() throws Exception {
    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    namespaceOpenApiService.getNamespace(someAppId, someEnv, someCluster, someNamespace);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(
        String.format(
            "%s/envs/%s/apps/%s/clusters/%s/namespaces/%s",
            someBaseUrl, someEnv, someAppId, someCluster, someNamespace),
        get.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testGetNamespaceWithError() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(404);

    namespaceOpenApiService.getNamespace(someAppId, someEnv, someCluster, someNamespace);
  }

  @Test
  public void testGetNamespaces() throws Exception {
    StringEntity responseEntity = new StringEntity("[]");
    when(someHttpResponse.getEntity()).thenReturn(responseEntity);

    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    namespaceOpenApiService.getNamespaces(someAppId, someEnv, someCluster);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(
        String.format(
            "%s/envs/%s/apps/%s/clusters/%s/namespaces",
            someBaseUrl, someEnv, someAppId, someCluster),
        get.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testGetNamespacesWithError() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(404);

    namespaceOpenApiService.getNamespaces(someAppId, someEnv, someCluster);
  }

  @Test
  public void testCreateAppNamespace() throws Exception {
    String someName = "someName";
    String someCreatedBy = "someCreatedBy";

    OpenAppNamespaceDTO appNamespaceDTO = new OpenAppNamespaceDTO();
    appNamespaceDTO.setAppId(someAppId);
    appNamespaceDTO.setName(someName);
    appNamespaceDTO.setDataChangeCreatedBy(someCreatedBy);

    final ArgumentCaptor<HttpPost> request = ArgumentCaptor.forClass(HttpPost.class);

    namespaceOpenApiService.createAppNamespace(appNamespaceDTO);

    verify(httpClient, times(1)).execute(request.capture());

    HttpPost post = request.getValue();

    assertEquals(
        String.format("%s/apps/%s/appnamespaces", someBaseUrl, someAppId),
        post.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateAppNamespaceWithError() throws Exception {
    String someName = "someName";
    String someCreatedBy = "someCreatedBy";

    OpenAppNamespaceDTO appNamespaceDTO = new OpenAppNamespaceDTO();
    appNamespaceDTO.setAppId(someAppId);
    appNamespaceDTO.setName(someName);
    appNamespaceDTO.setDataChangeCreatedBy(someCreatedBy);

    when(statusLine.getStatusCode()).thenReturn(400);

    namespaceOpenApiService.createAppNamespace(appNamespaceDTO);
  }

  @Test
  public void testGetNamespaceLock() throws Exception {
    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    namespaceOpenApiService.getNamespaceLock(someAppId, someEnv, someCluster, someNamespace);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet post = request.getValue();

    assertEquals(
        String.format(
            "%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/lock",
            someBaseUrl, someEnv, someAppId, someCluster, someNamespace),
        post.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testGetNamespaceLockWithError() throws Exception {
    when(statusLine.getStatusCode()).thenReturn(404);

    namespaceOpenApiService.getNamespaceLock(someAppId, someEnv, someCluster, someNamespace);
  }
}
