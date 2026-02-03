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
package com.ctrip.framework.apollo.adminservice;

import com.ctrip.framework.apollo.AdminServiceTestConfiguration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Configuration validation test for graceful shutdown feature.
 *
 * This test verifies that the graceful shutdown configuration is properly loaded
 * from application.yml by checking ServerProperties and the web server lifecycle.
 *
 * Note: This test does NOT verify the actual behavior of graceful shutdown
 * (e.g., waiting for in-flight requests). Full behavioral testing requires:
 * - Integration tests with real HTTP requests during shutdown
 * - Manual testing in staging/production environments
 * - Monitoring of shutdown metrics and logs
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = AdminServiceTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GracefulShutdownConfigurationTest {

  @Autowired
  private ServletWebServerApplicationContext webServerAppContext;

  @Autowired
  private ServerProperties serverProperties;

  @Test
  public void testGracefulShutdownIsConfigured() {
    assertNotNull("WebServer should be available", webServerAppContext);
    assertTrue("Server should be running", webServerAppContext.getWebServer().getPort() > 0);

    // Verify graceful shutdown is enabled in application.yml
    assertEquals("Graceful shutdown should be enabled in application.yml", "graceful",
        serverProperties.getShutdown().name().toLowerCase());

    // Verify the lifecycle processor exists (indicates graceful shutdown is enabled)
    assertNotNull("Lifecycle processor should be present for graceful shutdown",
        webServerAppContext.getBean("lifecycleProcessor"));
  }
}
