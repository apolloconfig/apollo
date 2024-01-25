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
package com.ctrip.framework.apollo.portal.environment;

import static org.junit.Assert.*;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class DatabasePortalMetaServerProviderTest {

  private DatabasePortalMetaServerProvider databasePortalMetaServerProvider;
  @Mock
  private PortalConfig portalConfig;

  private Map<String, String> metaServiceMap;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    metaServiceMap = new HashMap<>();
    metaServiceMap.put("nothing", "http://unknown.com");
    metaServiceMap.put("dev", "http://server.com:8080");
    Mockito.when(portalConfig.getMetaServers()).thenReturn(metaServiceMap);

    // use mocked object to construct
    databasePortalMetaServerProvider = new DatabasePortalMetaServerProvider(portalConfig);
  }

  @Test
  public void testGetMetaServerAddress() {
    String address = databasePortalMetaServerProvider.getMetaServerAddress(Env.DEV);
    assertEquals("http://server.com:8080", address);

    String newMetaServerAddress = "http://another-server.com:8080";
    metaServiceMap.put("dev", newMetaServerAddress);

    databasePortalMetaServerProvider.reload();

    assertEquals(newMetaServerAddress, databasePortalMetaServerProvider.getMetaServerAddress(Env.DEV));

  }

  @Test
  public void testExists() {
    assertTrue(databasePortalMetaServerProvider.exists(Env.DEV));
    assertFalse(databasePortalMetaServerProvider.exists(Env.PRO));
    assertTrue(databasePortalMetaServerProvider.exists(Env.addEnvironment("nothing")));
  }
}