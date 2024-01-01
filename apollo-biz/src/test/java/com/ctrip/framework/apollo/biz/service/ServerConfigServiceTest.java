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
package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.ServerConfig;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author kl (http://kailing.pub)
 * @since 2022/12/14
 */
public class ServerConfigServiceTest extends AbstractIntegrationTest {

  @Autowired
  private ServerConfigService serverConfigService;

  @Test
  public void findAll() {
    List<ServerConfig> serverConfigs = serverConfigService.findAll();
    assertThat(serverConfigs).isNotNull();
    assertThat(serverConfigs.size()).isGreaterThanOrEqualTo(0);
  }

  @Test
  public void createOrUpdateConfig() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey("name");
    serverConfig.setValue("kl");
    serverConfigService.createOrUpdateConfig(serverConfig);

    List<ServerConfig> serverConfigs = serverConfigService.findAll();
    assertThat(serverConfigs).isNotNull();
    assertThat(serverConfigs.get(0).getValue()).isEqualTo("kl");
    assertThat(serverConfigs.get(0).getCluster()).isEqualTo("default");
    assertThat(serverConfigs.get(0).getKey()).isEqualTo("name");


    serverConfig.setValue("kl2");
    serverConfigService.createOrUpdateConfig(serverConfig);

    serverConfigs = serverConfigService.findAll();
    assertThat(serverConfigs).isNotNull();
    assertThat(serverConfigs.size()).isEqualTo(1);
    assertThat(serverConfigs.get(0).getValue()).isEqualTo("kl2");
    assertThat(serverConfigs.get(0).getKey()).isEqualTo("name");

    serverConfig = new ServerConfig();
    serverConfig.setKey("name2");
    serverConfig.setValue("kl2");
    serverConfigService.createOrUpdateConfig(serverConfig);

    serverConfigs = serverConfigService.findAll();
    assertThat(serverConfigs).isNotNull();
    assertThat(serverConfigs.size()).isEqualTo(2);
  }
}