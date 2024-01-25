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
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

/**
 * Integration test for {@link AppController}.
 *
 * @author wxq
 */
public class AppControllerIntegrationTest extends AbstractIntegrationTest {

  @Test
  @Sql(scripts = "/sql/openapi/ConsumerServiceIntegrationTest.testFindAppIdsAuthorizedByConsumerId.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindAppsAuthorized() {
    final String token = "3c16bf5b1f44b465179253442460e8c0ad845289";
    HttpHeaders httpHeaders = new HttpHeaders();
    httpHeaders.set(HttpHeaders.AUTHORIZATION, token);

    ResponseEntity<OpenAppDTO[]> responseEntity =
        restTemplate.exchange(this.url("/openapi/v1/apps/authorized"), HttpMethod.GET,
            new HttpEntity<>(httpHeaders), OpenAppDTO[].class);

    OpenAppDTO[] openAppDTOS = responseEntity.getBody();
    assertEquals(2, openAppDTOS.length);

    Set<String> appIds = new HashSet<>();
    for (OpenAppDTO openAppDTO : openAppDTOS) {
      appIds.add(openAppDTO.getAppId());
    }

    assertTrue(appIds.contains("consumer-test-app-id-0"));
    assertTrue(appIds.contains("consumer-test-app-id-1"));
  }
}
