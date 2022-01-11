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
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class ItemOpenApiServiceTest extends AbstractOpenApiServiceTest {

  private ItemOpenApiService itemOpenApiService;

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

    itemOpenApiService = new ItemOpenApiService(httpClient, someBaseUrl, gson);
  }

  @Test
  public void testGetItem() throws Exception {
    String someKey = "someKey";

    final ArgumentCaptor<HttpGet> request = ArgumentCaptor.forClass(HttpGet.class);

    itemOpenApiService.getItem(someAppId, someEnv, someCluster, someNamespace, someKey);

    verify(httpClient, times(1)).execute(request.capture());

    HttpGet get = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/items/%s", someBaseUrl, someEnv,
            someAppId, someCluster, someNamespace, someKey), get.getURI().toString());
  }

  @Test
  public void testGetNotExistedItem() throws Exception {
    String someKey = "someKey";

    when(statusLine.getStatusCode()).thenReturn(404);

    assertNull(itemOpenApiService.getItem(someAppId, someEnv, someCluster, someNamespace, someKey));
  }

  @Test
  public void testCreateItem() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String someCreatedBy = "someCreatedBy";

    OpenItemDTO itemDTO = new OpenItemDTO();
    itemDTO.setKey(someKey);
    itemDTO.setValue(someValue);
    itemDTO.setDataChangeCreatedBy(someCreatedBy);

    final ArgumentCaptor<HttpPost> request = ArgumentCaptor.forClass(HttpPost.class);

    itemOpenApiService.createItem(someAppId, someEnv, someCluster, someNamespace, itemDTO);

    verify(httpClient, times(1)).execute(request.capture());

    HttpPost post = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/items", someBaseUrl, someEnv, someAppId, someCluster,
            someNamespace), post.getURI().toString());

    StringEntity entity = (StringEntity) post.getEntity();

    assertEquals(ContentType.APPLICATION_JSON.toString(), entity.getContentType().getValue());
    assertEquals(gson.toJson(itemDTO), EntityUtils.toString(entity));
  }

  @Test(expected = RuntimeException.class)
  public void testCreateItemWithError() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String someCreatedBy = "someCreatedBy";

    OpenItemDTO itemDTO = new OpenItemDTO();
    itemDTO.setKey(someKey);
    itemDTO.setValue(someValue);
    itemDTO.setDataChangeCreatedBy(someCreatedBy);

    when(statusLine.getStatusCode()).thenReturn(400);

    itemOpenApiService.createItem(someAppId, someEnv, someCluster, someNamespace, itemDTO);
  }

  @Test
  public void testUpdateItem() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String someModifiedBy = "someModifiedBy";

    OpenItemDTO itemDTO = new OpenItemDTO();
    itemDTO.setKey(someKey);
    itemDTO.setValue(someValue);
    itemDTO.setDataChangeLastModifiedBy(someModifiedBy);

    final ArgumentCaptor<HttpPut> request = ArgumentCaptor.forClass(HttpPut.class);

    itemOpenApiService.updateItem(someAppId, someEnv, someCluster, someNamespace, itemDTO);

    verify(httpClient, times(1)).execute(request.capture());

    HttpPut put = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/items/%s", someBaseUrl, someEnv, someAppId, someCluster,
            someNamespace, someKey), put.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testUpdateItemWithError() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String someModifiedBy = "someModifiedBy";

    OpenItemDTO itemDTO = new OpenItemDTO();
    itemDTO.setKey(someKey);
    itemDTO.setValue(someValue);
    itemDTO.setDataChangeLastModifiedBy(someModifiedBy);

    when(statusLine.getStatusCode()).thenReturn(400);

    itemOpenApiService.updateItem(someAppId, someEnv, someCluster, someNamespace, itemDTO);
  }

  @Test
  public void testCreateOrUpdateItem() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String someCreatedBy = "someCreatedBy";

    OpenItemDTO itemDTO = new OpenItemDTO();
    itemDTO.setKey(someKey);
    itemDTO.setValue(someValue);
    itemDTO.setDataChangeCreatedBy(someCreatedBy);

    final ArgumentCaptor<HttpPut> request = ArgumentCaptor.forClass(HttpPut.class);

    itemOpenApiService.createOrUpdateItem(someAppId, someEnv, someCluster, someNamespace, itemDTO);

    verify(httpClient, times(1)).execute(request.capture());

    HttpPut put = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/items/%s?createIfNotExists=true", someBaseUrl, someEnv,
            someAppId, someCluster, someNamespace, someKey), put.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testCreateOrUpdateItemWithError() throws Exception {
    String someKey = "someKey";
    String someValue = "someValue";
    String someCreatedBy = "someCreatedBy";

    OpenItemDTO itemDTO = new OpenItemDTO();
    itemDTO.setKey(someKey);
    itemDTO.setValue(someValue);
    itemDTO.setDataChangeCreatedBy(someCreatedBy);

    when(statusLine.getStatusCode()).thenReturn(400);

    itemOpenApiService.createOrUpdateItem(someAppId, someEnv, someCluster, someNamespace, itemDTO);
  }

  @Test
  public void testRemoveItem() throws Exception {
    String someKey = "someKey";
    String someOperator = "someOperator";

    final ArgumentCaptor<HttpDelete> request = ArgumentCaptor.forClass(HttpDelete.class);

    itemOpenApiService.removeItem(someAppId, someEnv, someCluster, someNamespace, someKey, someOperator);

    verify(httpClient, times(1)).execute(request.capture());

    HttpDelete delete = request.getValue();

    assertEquals(String
        .format("%s/envs/%s/apps/%s/clusters/%s/namespaces/%s/items/%s?operator=%s", someBaseUrl, someEnv,
            someAppId, someCluster, someNamespace, someKey, someOperator), delete.getURI().toString());
  }

  @Test(expected = RuntimeException.class)
  public void testRemoveItemWithError() throws Exception {
    String someKey = "someKey";
    String someOperator = "someOperator";

    when(statusLine.getStatusCode()).thenReturn(404);

    itemOpenApiService.removeItem(someAppId, someEnv, someCluster, someNamespace, someKey, someOperator);
  }
}
