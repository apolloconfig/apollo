/*
 * Copyright 2025 Apollo Authors
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
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ServerConfigService;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.web.client.HttpClientErrorException;

import java.util.*;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by kezhenxu at 2019/1/14 13:24.
 *
 * @author kezhenxu (kezhenxu at lizhi dot fm)
 */
@ActiveProfiles("skipAuthorization")
public class ServerConfigControllerTest extends AbstractIntegrationTest {
  @Mock
  private ServerConfigService serverConfigService;

  @InjectMocks
  private ServerConfigController serverConfigController;

  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void shouldSuccessWhenParameterValidForPortalDBConfig() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey("validKey");
    serverConfig.setValue("validValue");
    ResponseEntity<ServerConfig> responseEntity = restTemplate
        .postForEntity(url("/server/portal-db/config"), serverConfig, ServerConfig.class);
    assertEquals(responseEntity.getBody().getKey(), serverConfig.getKey());
    assertEquals(responseEntity.getBody().getValue(), serverConfig.getValue());
  }

  @Test
  public void shouldFailWhenParameterInvalidForPortalDBConfig() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey("  ");
    serverConfig.setValue("valid");
    try {
      restTemplate.postForEntity(url("/server/portal-db/config"), serverConfig, ServerConfig.class);
      Assert.fail("Should throw");
    } catch (final HttpClientErrorException e) {
      assertThat(new String(e.getResponseBodyAsByteArray()),
          containsString("ServerConfig.Key cannot be blank"));
    }
    serverConfig.setKey("valid");
    serverConfig.setValue("   ");
    try {
      restTemplate.postForEntity(url("/server/portal-db/config"), serverConfig, ServerConfig.class);
      Assert.fail("Should throw");
    } catch (final HttpClientErrorException e) {
      assertThat(new String(e.getResponseBodyAsByteArray()),
          containsString("ServerConfig.Value cannot be blank"));
    }
  }

  @Test
  public void testFindEmpty() {
    when(serverConfigService.findAllPortalDBConfig()).thenReturn(new ArrayList<>());
    when(serverConfigService.findAllConfigDBConfig(Env.DEV)).thenReturn(new ArrayList<>());

    List<ServerConfig> serverConfigList = serverConfigController.findAllPortalDBServerConfig();
    Assert.assertNotNull(serverConfigList);
    Assert.assertEquals(0, serverConfigList.size());

    serverConfigList = serverConfigController.findAllConfigDBServerConfig(Env.DEV.getName());
    Assert.assertNotNull(serverConfigList);
    Assert.assertEquals(0, serverConfigList.size());

  }

  @Test
  public void deletePortalDBConfig() {
    serverConfigController.deletePortalDBConfig("timeout");

    verify(serverConfigService).deletePortalDBConfig("timeout");
  }

  @Test
  public void createConfigDBConfigShouldPassKeyAndClusterForNewClusterWhenSameKeyExists() {
    String key = "timeout";
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey(key);
    serverConfig.setCluster("SHAJQ");
    serverConfig.setValue("clusterValue");

    when(serverConfigService.createOrUpdateConfigDBConfig(Env.DEV, serverConfig))
        .thenReturn(serverConfig);

    ServerConfig result =
        serverConfigController.createOrUpdateConfigDBConfig(serverConfig, Env.DEV.getName());

    Assert.assertNotNull(result);
    Assert.assertEquals(key, result.getKey());
    Assert.assertEquals("SHAJQ", result.getCluster());
    verify(serverConfigService).createOrUpdateConfigDBConfig(Env.DEV, serverConfig);
    verify(serverConfigService, never()).createOrUpdateConfigDBConfig(eq(Env.DEV),
        argThat(config -> key.equals(config.getKey()) && "default".equals(config.getCluster())));
  }

  @Test
  public void updateConfigDBConfigShouldPassTargetKeyAndClusterWhenSameKeyExists() {
    String key = "timeout";
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey(key);
    serverConfig.setCluster("SHAJQ");
    serverConfig.setValue("clusterValueUpdated");

    when(serverConfigService.createOrUpdateConfigDBConfig(Env.DEV, serverConfig))
        .thenReturn(serverConfig);

    ServerConfig result =
        serverConfigController.createOrUpdateConfigDBConfig(serverConfig, Env.DEV.getName());

    Assert.assertNotNull(result);
    Assert.assertEquals(key, result.getKey());
    Assert.assertEquals("SHAJQ", result.getCluster());
    Assert.assertEquals("clusterValueUpdated", result.getValue());
    verify(serverConfigService).createOrUpdateConfigDBConfig(Env.DEV, serverConfig);
    verify(serverConfigService, never()).createOrUpdateConfigDBConfig(eq(Env.DEV),
        argThat(config -> key.equals(config.getKey()) && "default".equals(config.getCluster())));
  }

  @Test
  public void deleteConfigDBConfig() {
    serverConfigController.deleteConfigDBConfig(Env.DEV.getName(), "timeout", "default");

    verify(serverConfigService).deleteConfigDBConfig(Env.DEV, "timeout", "default");
  }

  @Test
  public void deleteConfigDBConfigShouldTargetSpecifiedClusterWhenSameKeyExists() {
    String key = "timeout";

    serverConfigController.deleteConfigDBConfig(Env.DEV.getName(), key, "SHAJQ");

    verify(serverConfigService).deleteConfigDBConfig(Env.DEV, key, "SHAJQ");
    verify(serverConfigService, never()).deleteConfigDBConfig(Env.DEV, key, "default");
  }

  @Test
  public void createUpdateDeleteConfigDBConfigShouldRespectKeyAndClusterIdentity() {
    String key = "timeout";
    ServerConfig defaultConfig = new ServerConfig();
    defaultConfig.setKey(key);
    defaultConfig.setCluster("default");
    defaultConfig.setValue("defaultValue");

    ServerConfig shajqConfig = new ServerConfig();
    shajqConfig.setKey(key);
    shajqConfig.setCluster("SHAJQ");
    shajqConfig.setValue("clusterValue");

    ServerConfig shajqConfigUpdated = new ServerConfig();
    shajqConfigUpdated.setKey(key);
    shajqConfigUpdated.setCluster("SHAJQ");
    shajqConfigUpdated.setValue("clusterValueUpdated");

    when(serverConfigService.createOrUpdateConfigDBConfig(Env.DEV, defaultConfig))
        .thenReturn(defaultConfig);
    when(serverConfigService.createOrUpdateConfigDBConfig(Env.DEV, shajqConfig))
        .thenReturn(shajqConfig);
    when(serverConfigService.createOrUpdateConfigDBConfig(Env.DEV, shajqConfigUpdated))
        .thenReturn(shajqConfigUpdated);

    serverConfigController.createOrUpdateConfigDBConfig(defaultConfig, Env.DEV.getName());
    serverConfigController.createOrUpdateConfigDBConfig(shajqConfig, Env.DEV.getName());
    serverConfigController.createOrUpdateConfigDBConfig(shajqConfigUpdated, Env.DEV.getName());
    serverConfigController.deleteConfigDBConfig(Env.DEV.getName(), key, "SHAJQ");

    verify(serverConfigService, times(1)).createOrUpdateConfigDBConfig(eq(Env.DEV),
        argThat(config -> config != null && key.equals(config.getKey())
            && "default".equals(config.getCluster())));
    verify(serverConfigService, times(2)).createOrUpdateConfigDBConfig(eq(Env.DEV),
        argThat(config -> config != null && key.equals(config.getKey())
            && "SHAJQ".equals(config.getCluster())));
    verify(serverConfigService).deleteConfigDBConfig(Env.DEV, key, "SHAJQ");
    verify(serverConfigService, never()).deleteConfigDBConfig(Env.DEV, key, "default");
  }

}
