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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.portal.AbstractUnitTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultPortalMetaServerProviderTest extends AbstractUnitTest {

  private DefaultPortalMetaServerProvider defaultPortalMetaServerProvider;

  @Before
  public void setUp() throws Exception {
    defaultPortalMetaServerProvider = new DefaultPortalMetaServerProvider();
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty("dev_meta");
    System.clearProperty("fat_meta");
  }

  @Test
  public void testFromPropertyFile() {
    assertEquals("http://localhost:8080", defaultPortalMetaServerProvider.getMetaServerAddress(Env.LOCAL));
    assertEquals("${dev_meta}", defaultPortalMetaServerProvider.getMetaServerAddress(Env.DEV));
    assertEquals("${pro_meta}", defaultPortalMetaServerProvider.getMetaServerAddress(Env.PRO));
  }

  /**
   * testing the environment dynamic added from system property
   */
  @Test
  public void testDynamicEnvironmentFromSystemProperty() {
    String someDevMetaAddress = "someMetaAddress";
    String someFatMetaAddress = "someFatMetaAddress";
    System.setProperty("dev_meta", someDevMetaAddress);
    System.setProperty("fat_meta", someFatMetaAddress);
    // reload above added
    defaultPortalMetaServerProvider.reload();
    assertEquals(someDevMetaAddress, defaultPortalMetaServerProvider.getMetaServerAddress(Env.DEV));
    assertEquals(someFatMetaAddress, defaultPortalMetaServerProvider.getMetaServerAddress(Env.FAT));

    String randomAddress = "randomAddress";
    String randomEnvironment = "randomEnvironment";
    System.setProperty(randomEnvironment + "_meta", randomAddress);
    assertFalse(defaultPortalMetaServerProvider.exists(Env.addEnvironment(randomEnvironment)));
    // reload above added
    defaultPortalMetaServerProvider.reload();
    assertEquals(randomAddress,
        defaultPortalMetaServerProvider.getMetaServerAddress(Env.valueOf(randomEnvironment)));
    assertTrue(defaultPortalMetaServerProvider.exists(Env.addEnvironment(randomEnvironment)));

    // clear the property
    System.clearProperty(randomEnvironment + "_meta");
  }

}