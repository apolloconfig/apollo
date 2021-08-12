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
package com.ctrip.framework.foundation.internals.provider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import java.io.File;
import java.io.FileInputStream;
import org.junit.Before;
import org.junit.Test;

public class DefaultApplicationProviderTest {
  private DefaultApplicationProvider defaultApplicationProvider;
  String PREDEFINED_APP_ID = "110402";

  @Before
  public void setUp() throws Exception {
    defaultApplicationProvider = new DefaultApplicationProvider();
  }

  @Test
  public void testLoadAppProperties() throws Exception {
    defaultApplicationProvider.initialize();

    assertEquals(PREDEFINED_APP_ID, defaultApplicationProvider.getAppId());
    assertTrue(defaultApplicationProvider.isAppIdSet());
  }

  @Test
  public void testLoadAppPropertiesWithUTF8Bom() throws Exception {
    File baseDir = new File("src/test/resources/META-INF");
    File appProperties = new File(baseDir, "app-with-utf8bom.properties");

    defaultApplicationProvider.initialize(new FileInputStream(appProperties));

    assertEquals(PREDEFINED_APP_ID, defaultApplicationProvider.getAppId());
    assertTrue(defaultApplicationProvider.isAppIdSet());
  }

  @Test
  public void testLoadAppPropertiesWithSystemProperty() throws Exception {
    String someAppId = "someAppId";
    String someSecret = "someSecret";
    System.setProperty(ApolloClientSystemConsts.APP_ID, someAppId);
    System.setProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET, someSecret);
    defaultApplicationProvider.initialize();
    System.clearProperty(ApolloClientSystemConsts.APP_ID);
    System.clearProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET);

    assertEquals(someAppId, defaultApplicationProvider.getAppId());
    assertTrue(defaultApplicationProvider.isAppIdSet());
    assertEquals(someSecret, defaultApplicationProvider.getAccessKeySecret());
  }

  @Test
  public void testLoadAppPropertiesFailed() throws Exception {
    File baseDir = new File("src/test/resources/META-INF");
    File appProperties = new File(baseDir, "some-invalid-app.properties");

    defaultApplicationProvider.initialize(new FileInputStream(appProperties));

    assertEquals(null, defaultApplicationProvider.getAppId());
    assertFalse(defaultApplicationProvider.isAppIdSet());
  }
}
