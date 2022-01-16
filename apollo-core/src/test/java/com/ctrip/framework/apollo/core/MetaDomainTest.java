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
package com.ctrip.framework.apollo.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.BaseIntegrationTest;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.internals.LegacyMetaServerProvider;
import com.ctrip.framework.apollo.core.spi.MetaServerProvider;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Test;

public class MetaDomainTest extends BaseIntegrationTest {

  @Override
  @After
  public void tearDown() throws Exception {
    super.tearDown();
    MockMetaServerProvider.clear();
  }

  @Test
  public void testGetMetaDomain() {
    assertEquals("http://localhost:8080", MetaDomainConsts.getDomain(Env.LOCAL));
    assertEquals("http://dev:8080", MetaDomainConsts.getDomain(Env.DEV));
    assertEquals(MetaDomainConsts.DEFAULT_META_URL, MetaDomainConsts.getDomain(Env.PRO));
  }

  @Test
  public void testGetValidAddress() throws Exception {
    String someResponse = "some response";
    startServerWithHandlers(mockServerHandler(HttpServletResponse.SC_OK, someResponse));

    String validServer = " http://localhost:" + PORT + " ";
    String invalidServer = "http://localhost:" + findFreePort();

    MockMetaServerProvider.mock(Env.FAT, validServer + "," + invalidServer);
    MockMetaServerProvider.mock(Env.UAT, invalidServer + "," + validServer);

    assertEquals(validServer.trim(), MetaDomainConsts.getDomain(Env.FAT));
    assertEquals(validServer.trim(), MetaDomainConsts.getDomain(Env.UAT));
  }

  @Test
  public void testInvalidAddress() throws Exception {
    String invalidServer = "http://localhost:" + findFreePort() + " ";
    String anotherInvalidServer = "http://localhost:" + findFreePort() + " ";

    MockMetaServerProvider.mock(Env.LPT, invalidServer + "," + anotherInvalidServer);

    String metaServer = MetaDomainConsts.getDomain(Env.LPT);

    assertTrue(
        metaServer.equals(invalidServer.trim()) || metaServer.equals(anotherInvalidServer.trim()));
  }

  public static class MockMetaServerProvider implements MetaServerProvider {

    private static Map<Env, String> mockMetaServerAddress = Maps.newHashMap();

    private static void mock(Env env, String metaServerAddress) {
      mockMetaServerAddress.put(env, metaServerAddress);
    }

    private static void clear() {
      mockMetaServerAddress.clear();
    }

    @Override
    public String getMetaServerAddress(Env targetEnv) {
      return mockMetaServerAddress.get(targetEnv);
    }

    @Override
    public int getOrder() {
      return LegacyMetaServerProvider.ORDER - 1; // just in front of LegacyMetaServerProvider
    }
  }
}
