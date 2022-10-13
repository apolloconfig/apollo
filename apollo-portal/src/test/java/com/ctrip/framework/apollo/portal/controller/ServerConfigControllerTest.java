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

import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.entity.BaseEntity;
import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by kezhenxu at 2019/1/14 13:24.
 *
 * @author kezhenxu (kezhenxu at lizhi dot fm)
 */
@ActiveProfiles("skipAuthorization")
public class ServerConfigControllerTest extends AbstractIntegrationTest {
  @Autowired
  private ServerConfigRepository serverConfigRepository;

  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
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

  @Test
  public void testFindEmpty() {
    Iterable<ServerConfig> serverConfigs = serverConfigRepository.findAll();
    List<ServerConfig> serverConfigList = Lists.newArrayList(serverConfigs);

    Assert.assertNotNull(serverConfigList);
    Assert.assertEquals(0, serverConfigList.size());
  }

  @Test
  @Sql(scripts = "/sql/permission/insert-test-config.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindConfig() {
    Iterable<ServerConfig> serverConfigs = serverConfigRepository.findAll();
    List<ServerConfig> serverConfigList = Lists.newArrayList(serverConfigs);

    Assert.assertNotNull(serverConfigList);
    Assert.assertEquals(6, serverConfigList.size());
  }

}
