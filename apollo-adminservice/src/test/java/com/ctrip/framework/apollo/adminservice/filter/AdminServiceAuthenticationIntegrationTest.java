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
package com.ctrip.framework.apollo.adminservice.filter;

import com.ctrip.framework.apollo.adminservice.controller.AbstractControllerTest;
import com.ctrip.framework.apollo.common.config.RefreshablePropertySource;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

@DirtiesContext
public class AdminServiceAuthenticationIntegrationTest extends AbstractControllerTest {

  @Autowired
  private List<RefreshablePropertySource> propertySources;

  @Before
  public void setUp() throws Exception {
    doRefresh(propertySources);
  }

  @Test
  @Sql(scripts = "/controller/test-release.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/filter/test-access-control-disabled.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testWithAccessControlDisabledExplicitly() {
    String appId = "someAppId";
    AppDTO app = restTemplate
        .getForObject("http://localhost:" + port + "/apps/" + appId, AppDTO.class);

    Assert.assertEquals("someAppId", app.getAppId());
  }

  @Test
  @Sql(scripts = "/controller/test-release.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/filter/test-access-control-disabled.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testWithAccessControlDisabledExplicitlyWithAccessToken() {
    String appId = "someAppId";
    String someToken = "someToken";
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, someToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    AppDTO app = restTemplate
        .exchange("http://localhost:" + port + "/apps/" + appId, HttpMethod.GET, entity,
            AppDTO.class).getBody();

    Assert.assertEquals("someAppId", app.getAppId());
  }

  @Test
  @Sql(scripts = "/controller/test-release.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/filter/test-access-control-enabled.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testWithAccessControlEnabledWithValidAccessToken() {
    String appId = "someAppId";
    String someValidToken = "someToken";
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, someValidToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    AppDTO app = restTemplate
        .exchange("http://localhost:" + port + "/apps/" + appId, HttpMethod.GET, entity,
            AppDTO.class).getBody();

    Assert.assertEquals("someAppId", app.getAppId());
  }

  @Test(expected = HttpClientErrorException.class)
  @Sql(scripts = "/controller/test-release.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/filter/test-access-control-enabled.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testWithAccessControlEnabledWithNoAccessToken() {
    String appId = "someAppId";
    AppDTO app = restTemplate
        .getForObject("http://localhost:" + port + "/apps/" + appId, AppDTO.class);
  }

  @Test(expected = HttpClientErrorException.class)
  @Sql(scripts = "/controller/test-release.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/filter/test-access-control-enabled.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testWithAccessControlEnabledWithInValidAccessToken() {
    String appId = "someAppId";
    String someValidToken = "someInvalidToken";
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, someValidToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    AppDTO app = restTemplate
        .exchange("http://localhost:" + port + "/apps/" + appId, HttpMethod.GET, entity,
            AppDTO.class).getBody();
  }

  @Test
  @Sql(scripts = "/controller/test-release.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/filter/test-access-control-enabled-no-token.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testWithAccessControlEnabledWithNoTokenSpecified() {
    String appId = "someAppId";
    String someToken = "someToken";
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.AUTHORIZATION, someToken);
    HttpEntity<Void> entity = new HttpEntity<>(headers);

    AppDTO app = restTemplate
        .exchange("http://localhost:" + port + "/apps/" + appId, HttpMethod.GET, entity,
            AppDTO.class).getBody();

    Assert.assertEquals("someAppId", app.getAppId());
  }


  private void doRefresh(List<RefreshablePropertySource> propertySources) {
    propertySources.forEach(refreshablePropertySource -> ReflectionTestUtils
        .invokeMethod(refreshablePropertySource, "refresh"));
  }
}
