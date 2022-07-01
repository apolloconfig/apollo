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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.HttpClientErrorException;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kezhenxu at 2019/1/14 13:24.
 *
 * @author kezhenxu (kezhenxu at lizhi dot fm)
 */
@ActiveProfiles("skipAuthorization")
public class ServerConfigControllerTest extends AbstractIntegrationTest {
  @Test
  public void shouldSuccessWhenParameterValid() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey("validKey");
    serverConfig.setValue("validValue");
    ResponseEntity<ServerConfig> responseEntity = restTemplate.postForEntity(
        url("/server/config"), serverConfig, ServerConfig.class
    );
    assertEquals(responseEntity.getBody().getKey(), serverConfig.getKey());
    assertEquals(responseEntity.getBody().getValue(), serverConfig.getValue());
  }

  @Test
  public void shouldFailWhenParameterInvalid() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey("  ");
    serverConfig.setValue("valid");
    try {
      restTemplate.postForEntity(
          url("/server/config"), serverConfig, ServerConfig.class
      );
      Assert.fail("Should throw");
    } catch (final HttpClientErrorException e) {
      assertThat(
          new String(e.getResponseBodyAsByteArray()),
          containsString("ServerConfig.Key cannot be blank")
      );
    }
    serverConfig.setKey("valid");
    serverConfig.setValue("   ");
    try {
      restTemplate.postForEntity(
          url("/server/config"), serverConfig, ServerConfig.class
      );
      Assert.fail("Should throw");
    } catch (final HttpClientErrorException e) {
      assertThat(
          new String(e.getResponseBodyAsByteArray()),
          containsString("ServerConfig.Value cannot be blank")
      );
    }
  }
}
