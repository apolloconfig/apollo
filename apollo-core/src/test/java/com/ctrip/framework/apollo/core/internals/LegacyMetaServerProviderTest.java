/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.core.internals;

import static org.junit.Assert.assertEquals;

import com.ctrip.framework.apollo.core.enums.Env;
import org.junit.After;
import org.junit.Test;

public class LegacyMetaServerProviderTest {

  @After
  public void tearDown() throws Exception {
    System.clearProperty("dev_meta");
  }

  @Test
  public void testFromPropertyFile() {
    LegacyMetaServerProvider legacyMetaServerProvider = new LegacyMetaServerProvider();
    assertEquals("http://localhost:8080", legacyMetaServerProvider.getMetaServerAddress(Env.LOCAL));
    assertEquals("http://dev:8080", legacyMetaServerProvider.getMetaServerAddress(Env.DEV));
    assertEquals(null, legacyMetaServerProvider.getMetaServerAddress(Env.PRO));
  }

  @Test
  public void testWithSystemProperty() throws Exception {
    String someDevMetaAddress = "someMetaAddress";
    String someFatMetaAddress = "someFatMetaAddress";
    System.setProperty("dev_meta", someDevMetaAddress);
    System.setProperty("fat_meta", someFatMetaAddress);

    LegacyMetaServerProvider legacyMetaServerProvider = new LegacyMetaServerProvider();

    assertEquals(someDevMetaAddress, legacyMetaServerProvider.getMetaServerAddress(Env.DEV));
    assertEquals(someFatMetaAddress, legacyMetaServerProvider.getMetaServerAddress(Env.FAT));
  }
}
