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
package com.ctrip.framework.apollo.spring.boot;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.spring.config.CachedCompositePropertySource;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;

public class ApolloApplicationContextInitializerTest {

  private ApolloApplicationContextInitializer apolloApplicationContextInitializer;

  @Before
  public void setUp() throws Exception {
    apolloApplicationContextInitializer = new ApolloApplicationContextInitializer();
  }

  @After
  public void tearDown() throws Exception {
    System.clearProperty(ApolloClientSystemConsts.APP_ID);
    System.clearProperty(ConfigConsts.APOLLO_CLUSTER_KEY);
    System.clearProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR);
    System.clearProperty(ConfigConsts.APOLLO_META_KEY);

    MockInjector.reset();
  }

  @Test
  public void testFillFromEnvironment() throws Exception {
    String someAppId = "someAppId";
    String someCluster = "someCluster";
    String someCacheDir = "someCacheDir";
    String someApolloMeta = "someApolloMeta";

    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);

    when(environment.getProperty(ApolloClientSystemConsts.APP_ID)).thenReturn(someAppId);
    when(environment.getProperty(ConfigConsts.APOLLO_CLUSTER_KEY)).thenReturn(someCluster);
    when(environment.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR))
        .thenReturn(someCacheDir);
    when(environment.getProperty(ConfigConsts.APOLLO_META_KEY)).thenReturn(someApolloMeta);

    apolloApplicationContextInitializer.initializeSystemProperty(environment);

    assertEquals(someAppId, System.getProperty(ApolloClientSystemConsts.APP_ID));
    assertEquals(someCluster, System.getProperty(ConfigConsts.APOLLO_CLUSTER_KEY));
    assertEquals(someCacheDir, System.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR));
    assertEquals(someApolloMeta, System.getProperty(ConfigConsts.APOLLO_META_KEY));
  }

  @Test
  public void testFillFromEnvironmentWithSystemPropertyAlreadyFilled() throws Exception {
    String someAppId = "someAppId";
    String someCluster = "someCluster";
    String someCacheDir = "someCacheDir";
    String someApolloMeta = "someApolloMeta";

    System.setProperty(ApolloClientSystemConsts.APP_ID, someAppId);
    System.setProperty(ConfigConsts.APOLLO_CLUSTER_KEY, someCluster);
    System.setProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR, someCacheDir);
    System.setProperty(ConfigConsts.APOLLO_META_KEY, someApolloMeta);

    String anotherAppId = "anotherAppId";
    String anotherCluster = "anotherCluster";
    String anotherCacheDir = "anotherCacheDir";
    String anotherApolloMeta = "anotherApolloMeta";

    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);

    when(environment.getProperty(ApolloClientSystemConsts.APP_ID)).thenReturn(anotherAppId);
    when(environment.getProperty(ConfigConsts.APOLLO_CLUSTER_KEY)).thenReturn(anotherCluster);
    when(environment.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR))
        .thenReturn(anotherCacheDir);
    when(environment.getProperty(ConfigConsts.APOLLO_META_KEY)).thenReturn(anotherApolloMeta);

    apolloApplicationContextInitializer.initializeSystemProperty(environment);

    assertEquals(someAppId, System.getProperty(ApolloClientSystemConsts.APP_ID));
    assertEquals(someCluster, System.getProperty(ConfigConsts.APOLLO_CLUSTER_KEY));
    assertEquals(someCacheDir, System.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR));
    assertEquals(someApolloMeta, System.getProperty(ConfigConsts.APOLLO_META_KEY));
  }

  @Test
  public void testFillFromEnvironmentWithNoPropertyFromEnvironment() throws Exception {
    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);

    apolloApplicationContextInitializer.initializeSystemProperty(environment);

    assertNull(System.getProperty(ApolloClientSystemConsts.APP_ID));
    assertNull(System.getProperty(ConfigConsts.APOLLO_CLUSTER_KEY));
    assertNull(System.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR));
    assertNull(System.getProperty(ConfigConsts.APOLLO_META_KEY));
  }

  @Test
  public void testPropertyNamesCacheEnabled() {
    ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
    MutablePropertySources propertySources = new MutablePropertySources();
    when(environment.getPropertySources()).thenReturn(propertySources);
    when(environment.getProperty(
            PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES,
            ConfigConsts.NAMESPACE_APPLICATION))
        .thenReturn("");

    apolloApplicationContextInitializer.initialize(environment);

    assertTrue(
        propertySources.contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME));
    assertFalse(propertySources.iterator().next() instanceof CachedCompositePropertySource);

    ConfigUtil configUtil = new ConfigUtil();
    configUtil = spy(configUtil);
    when(configUtil.isPropertyNamesCacheEnabled()).thenReturn(true);
    MockInjector.setInstance(ConfigUtil.class, configUtil);
    apolloApplicationContextInitializer = new ApolloApplicationContextInitializer();
    propertySources.remove(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME);

    apolloApplicationContextInitializer.initialize(environment);

    assertTrue(
        propertySources.contains(PropertySourcesConstants.APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME));
    assertTrue(propertySources.iterator().next() instanceof CachedCompositePropertySource);
  }
}
