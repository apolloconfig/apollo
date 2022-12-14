package com.ctrip.framework.apollo.adminservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ctrip.framework.apollo.biz.entity.ServerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

/**
 * @author kl (http://kailing.pub)
 * @since 2022/12/14
 */
class ServerConfigControllerTest extends AbstractControllerTest {

  @Test
  @Sql(scripts = "/controller/test-server-config.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  void findAllServerConfig() {
    ServerConfig[] serverConfigs = restTemplate.getForObject(url("/server/config/find-all-config"), ServerConfig[].class);
    assertNotNull(serverConfigs);
    assertEquals(1, serverConfigs.length);
    assertEquals("name", serverConfigs[0].getKey());
    assertEquals("kl", serverConfigs[0].getValue());
  }

  @Test
  @Sql(scripts = "/controller/test-server-config.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  void createOrUpdatePortalDBConfig() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey("name");
    serverConfig.setValue("ckl");
    ServerConfig response = restTemplate.postForObject(url("/server/config"), serverConfig, ServerConfig.class);
    assertNotNull(response);

    ServerConfig[] serverConfigs = restTemplate.getForObject(url("/server/config/find-all-config"), ServerConfig[].class);
    assertNotNull(serverConfigs);
    assertEquals(1, serverConfigs.length);
    assertEquals("name", serverConfigs[0].getKey());
    assertEquals("ckl", serverConfigs[0].getValue());

    serverConfig = new ServerConfig();
    serverConfig.setKey("age");
    serverConfig.setValue("30");
    response = restTemplate.postForObject(url("/server/config"), serverConfig, ServerConfig.class);
    assertNotNull(response);

    serverConfigs = restTemplate.getForObject(url("/server/config/find-all-config"), ServerConfig[].class);
    assertNotNull(serverConfigs);
    assertEquals(2, serverConfigs.length);

  }
}