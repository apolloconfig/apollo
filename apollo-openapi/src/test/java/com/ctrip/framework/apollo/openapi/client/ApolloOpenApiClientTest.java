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
package com.ctrip.framework.apollo.openapi.client;

import static org.junit.Assert.*;

import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import org.junit.Test;

public class ApolloOpenApiClientTest {

  @Test
  public void testCreate() {
    String someUrl = "http://someUrl";
    String someToken = "someToken";

    ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder().withPortalUrl(someUrl).withToken(someToken).build();

    assertEquals(someUrl, client.getPortalUrl());
    assertEquals(someToken, client.getToken());
  }
  /**
   * please config correct value in testCRUD,then you can remove exception
   */
  @Test(expected = Exception.class)
  public void testIllegalKey(){
    testCRUD("/");
    testCRUD(".");
    testCRUD("a.b/c");
    testCRUD("<");
    testCRUD(">");
    testCRUD("\\");
    testCRUD("`");
    testCRUD("{");
    testCRUD("}");
    testCRUD("[");
    testCRUD("]");
    testCRUD("\"");
    testCRUD("'");
  }

  public void testCRUD(String key) {
    String someUrl = "http://localhost:8070";
    String someToken = "someToken";
    String appId = "test1";
    String env = "DEV";
    String cluster = "default";
    String namespace = "application";
    String value = "abc";
    String updateValue = "ddd";
    String operator = "apollo";

    ApolloOpenApiClient client = ApolloOpenApiClient.newBuilder().withPortalUrl(someUrl).withToken(someToken).build();

    OpenItemDTO createItem = new OpenItemDTO();
    createItem.setKey(key);
    createItem.setValue(value);

    createItem.setDataChangeCreatedBy(operator);
    OpenItemDTO item1 = client.createItem(appId, env, cluster, namespace, createItem);
    System.out.println("create item : "+item1);
    assertEquals(item1.getKey(), key);

    OpenItemDTO updateItem = new OpenItemDTO();
    updateItem.setKey(key);
    updateItem.setValue(updateValue);
    updateItem.setDataChangeCreatedBy(operator);
    updateItem.setDataChangeLastModifiedBy(operator);
    client.updateItem(appId, env, cluster, namespace,updateItem);
    OpenItemDTO after = client.getItem(appId, env, cluster, namespace, key);
    System.out.println("update item : "+after);
    assertEquals(after.getValue(), updateValue);

    NamespaceReleaseDTO releaseDto = new NamespaceReleaseDTO();
    releaseDto.setReleaseTitle("Release title");
    releaseDto.setReleasedBy(operator);
    client.publishNamespace(appId,env,cluster,namespace,releaseDto);
    System.out.println("release");

    client.removeItem(appId,env,cluster,namespace,key, operator);
    client.publishNamespace(appId,env,cluster,namespace,releaseDto);
    System.out.println("delete item and release");

    OpenItemDTO getItem = client.getItem(appId, env, cluster, namespace, key);
    assertNull(getItem);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testCreateWithInvalidUrl() {
    String someInvalidUrl = "someInvalidUrl";
    String someToken = "someToken";

    ApolloOpenApiClient.newBuilder().withPortalUrl(someInvalidUrl).withToken(someToken).build();
  }
}
