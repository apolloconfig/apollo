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

import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ClusterOpenApiServiceTest extends AbstractOpenApiServiceTest {

  private ClusterOpenApiService clusterOpenApiService;

  private String someAppId;
  private String someEnv;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    someAppId = "someAppId";
    someEnv = "someEnv";

    StringEntity responseEntity = new StringEntity("{}");
    when(someHttpResponse.getEntity()).thenReturn(responseEntity);

    clusterOpenApiService = new ClusterOpenApiService(httpClient, someBaseUrl, gson);
  }

  @Test
  public void testGetCluster() throws Exception {
    String someCluster = "someCluster";

    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    clusterOpenApiService.getCluster(someAppId, someEnv, someCluster);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(String
            .format("%s/envs/%s/apps/%s/clusters/%s", someBaseUrl, someEnv, someAppId, someCluster),
        get.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testGetClusterWithError() throws Exception {
    String someCluster = "someCluster";

    when(statusLine.getStatusCode()).thenReturn(404);

    clusterOpenApiService.getCluster(someAppId, someEnv, someCluster);
  }

  @Test
  public void testCreateCluster() throws Exception {
    String someCluster = "someCluster";
    String someCreatedBy = "someCreatedBy";

    OpenClusterDTO clusterDTO = new OpenClusterDTO();
    clusterDTO.setAppId(someAppId);
    clusterDTO.setName(someCluster);
    clusterDTO.setDataChangeCreatedBy(someCreatedBy);

    final ArgumentCaptor<HttpPost> request = ArgumentCaptor.forClass(HttpPost.class);

    clusterOpenApiService.createCluster(someEnv, clusterDTO);

    verify(httpClient, times(1)).execute(request.capture());

    HttpPost post = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters", someBaseUrl, someEnv, someAppId), post.getURI().toString());

    StringEntity entity = (StringEntity) post.getEntity();

    assertEquals(ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());
    assertEquals(gson.toJson(clusterDTO), EntityUtils.toString(entity));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateClusterWithError() throws Exception {
    String someCluster = "someCluster";
    String someCreatedBy = "someCreatedBy";

    OpenClusterDTO clusterDTO = new OpenClusterDTO();
    clusterDTO.setAppId(someAppId);
    clusterDTO.setName(someCluster);
    clusterDTO.setDataChangeCreatedBy(someCreatedBy);

    when(statusLine.getStatusCode()).thenReturn(400);

    clusterOpenApiService.createCluster(someEnv, clusterDTO);
  }
}
