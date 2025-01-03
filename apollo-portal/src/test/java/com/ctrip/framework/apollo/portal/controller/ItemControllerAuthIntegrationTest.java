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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.ControllableAuthorizationConfiguration;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.portal.PortalApplication;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.google.gson.Gson;
import javax.annotation.PostConstruct;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {PortalApplication.class,
    ControllableAuthorizationConfiguration.class}, webEnvironment = WebEnvironment.RANDOM_PORT)
public class ItemControllerAuthIntegrationTest {

  private final Gson GSON = new Gson();
  private final String appId = "testApp";
  private final String env = "LOCAL";
  private final String clusterName = "default";
  private final String namespaceName = "application";
  private final ItemDTO itemDTO = new ItemDTO("testKey", "testValue", "testComment", 1);
  protected RestTemplate restTemplate = (new TestRestTemplate()).getRestTemplate();

  @Value("${local.server.port}")
  int port;
  @Autowired
  private UserInfoHolder userInfoHolder;
  @Autowired
  private ItemService itemService;

  @PostConstruct
  private void postConstruct() {
    System.setProperty("spring.profiles.active", "test");
    restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
  }

  protected String url(String path) {
    return "http://localhost:" + port + path;
  }

  /**
   * Test cluster permission denied.
   */
  @Test
  public void testCreateItemPermissionDenied() {
    setUserId("xxx");
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.set("Content-Type", "application/json");

      HttpEntity<String> entity = new HttpEntity<>(GSON.toJson(itemDTO), headers);

      restTemplate.postForEntity(
          url("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/item"),
          entity, String.class, appId, env, clusterName, namespaceName);

      fail("should throw");
    } catch (final HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
    }
  }

  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateItemPermissionAccessed() {
    setUserId("luke");
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", "application/json");

    HttpEntity<String> entity = new HttpEntity<>(GSON.toJson(itemDTO), headers);

    restTemplate.postForEntity(
        url("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/item"),
        entity, String.class, appId, env, clusterName, namespaceName);

    // Verify that the createItem method was called with the correct parameters
    verify(itemService).createItem(eq(appId), eq(Env.valueOf(env)), eq(clusterName),
        eq(namespaceName), any(ItemDTO.class));
  }

  void setUserId(String userId) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    when(userInfoHolder.getUser()).thenReturn(userInfo);
  }
}