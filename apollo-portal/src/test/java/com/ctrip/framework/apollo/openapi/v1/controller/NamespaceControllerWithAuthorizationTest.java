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
package com.ctrip.framework.apollo.openapi.v1.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import java.util.Arrays;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;

/**
 * @author wxq
 */
public class NamespaceControllerWithAuthorizationTest extends AbstractControllerTest {

  static final HttpHeaders HTTP_HEADERS_WITH_TOKEN = new HttpHeaders() {{
    set(HttpHeaders.AUTHORIZATION, "3c16bf5b1f44b465179253442460e8c0ad845289");
  }};

  /**
   * test method {@link NamespaceController#createAppNamespace(String, OpenAppNamespaceDTO)}.
   */
  @Ignore("need admin server for this case")
  @Test
  @Sql(scripts = "/sql/openapi/NamespaceControllerTest.testCreateAppNamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateAppNamespace() {
    final String appId = "consumer-test-app-id-0";
    final String namespaceName = "create-app-namespace-success";

    // query
    {
      ResponseEntity<String> responseEntity =
          restTemplate.exchange(
              url("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces"),
              HttpMethod.GET, new HttpEntity<>(HTTP_HEADERS_WITH_TOKEN), String.class, "DEV", appId,
              "default");
      String responseEntityBody = responseEntity.getBody();
      assertNotNull(responseEntityBody);
      assertFalse(responseEntityBody.contains(namespaceName));
    }

    // create it
    final OpenAppNamespaceDTO dto = new OpenAppNamespaceDTO();
    dto.setAppId(appId);

    dto.setName(namespaceName);
    dto.setFormat(ConfigFileFormat.Properties.getValue());
    dto.setDataChangeCreatedBy("apollo");

    restTemplate.exchange(this.url("/openapi/v1/apps/{appId}/appnamespaces"), HttpMethod.POST,
        new HttpEntity<>(dto, HTTP_HEADERS_WITH_TOKEN), OpenAppNamespaceDTO.class, dto.getAppId());

    // query again to confirm
    {
      ResponseEntity<OpenNamespaceDTO[]> responseEntity =
          restTemplate
              .getForEntity("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces",
                  OpenNamespaceDTO[].class, "DEV", appId, "default");
      OpenNamespaceDTO[] openNamespaceDTOS = responseEntity.getBody();
      assertNotNull(openNamespaceDTOS);
      assertEquals(1, Arrays.stream(openNamespaceDTOS)
          .filter(openNamespaceDTO -> namespaceName.equals(openNamespaceDTO.getNamespaceName()))
          .count());
    }
  }

  /**
   * test method {@link NamespaceController#createAppNamespace(String, OpenAppNamespaceDTO)}.
   */
  @Test
  @Sql(scripts = "/sql/openapi/NamespaceControllerTest.testCreateAppNamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateAppNamespaceUnauthorized() {
    OpenAppNamespaceDTO dto = new OpenAppNamespaceDTO();
    dto.setAppId("consumer-test-app-id-0");
    dto.setName("namespace-0");
    dto.setFormat(ConfigFileFormat.Properties.getValue());
    dto.setDataChangeCreatedBy("apollo");
    try {
      restTemplate.postForEntity(
          url("/openapi/v1/apps/{appId}/appnamespaces"),
          dto, OpenAppNamespaceDTO.class, dto.getAppId()
      );
      Assert.fail("should throw");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
    }
  }

  /**
   * test method {@link NamespaceController#createAppNamespace(String, OpenAppNamespaceDTO)}. Just
   * for check Authorization is ok.
   */
  @Test
  @Sql(scripts = "/sql/openapi/NamespaceControllerTest.testCreateAppNamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateAppNamespaceInvalidNamespaceName() {
    OpenAppNamespaceDTO dto = new OpenAppNamespaceDTO();
    dto.setAppId("consumer-test-app-id-0");
    dto.setName("invalid name");
    dto.setFormat(ConfigFileFormat.Properties.getValue());
    dto.setDataChangeCreatedBy("apollo");

    try {
      restTemplate.exchange(this.url("/openapi/v1/apps/{appId}/appnamespaces"), HttpMethod.POST,
          new HttpEntity<>(dto, HTTP_HEADERS_WITH_TOKEN), OpenAppNamespaceDTO.class,
          dto.getAppId());
      Assert.fail("should throw");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
      String result = e.getResponseBodyAsString();
      assertTrue(result.contains(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
      assertTrue(result.contains(InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE));
    }
  }

  /**
   * test method {@link NamespaceController#createAppNamespace(String, OpenAppNamespaceDTO)} without
   * authority.
   */
  @Test
  @Sql(scripts = "/sql/openapi/NamespaceControllerTest.testCreateAppNamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateAppNamespaceWithoutAuthority() {
    final OpenAppNamespaceDTO dto = new OpenAppNamespaceDTO();
    dto.setAppId("consumer-test-app-id-1");
    dto.setName("create-app-namespace-fail");
    dto.setFormat(ConfigFileFormat.Properties.getValue());
    dto.setDataChangeCreatedBy("apollo");

    try {
      restTemplate.exchange(this.url("/openapi/v1/apps/{appId}/appnamespaces"), HttpMethod.POST,
          new HttpEntity<>(dto, HTTP_HEADERS_WITH_TOKEN), OpenAppNamespaceDTO.class,
          dto.getAppId());
      fail("should throw");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
      String result = e.getResponseBodyAsString();
      assertTrue(result.contains("org.springframework.security.access.AccessDeniedException"));
    }

    // random app id
    dto.setAppId(UUID.randomUUID().toString());

    try {
      restTemplate.exchange(this.url("/openapi/v1/apps/{appId}/appnamespaces"), HttpMethod.POST,
          new HttpEntity<>(dto, HTTP_HEADERS_WITH_TOKEN), OpenAppNamespaceDTO.class,
          dto.getAppId());
      fail("should throw");
    } catch (HttpClientErrorException e) {
      assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
      String result = e.getResponseBodyAsString();
      assertTrue(result.contains("org.springframework.security.access.AccessDeniedException"));
    }
  }
}
