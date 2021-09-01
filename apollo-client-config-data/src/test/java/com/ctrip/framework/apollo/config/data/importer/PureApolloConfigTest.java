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
package com.ctrip.framework.apollo.config.data.importer;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.config.data.injector.ApolloMockInjectorCustomizer;
import com.ctrip.framework.apollo.config.data.internals.PureApolloConfigFactory;
import com.ctrip.framework.apollo.spi.ConfigFactory;
import com.ctrip.framework.apollo.spi.DefaultConfigFactory;
import com.github.stefanbirkner.systemlambda.SystemLambda;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class PureApolloConfigTest {

  @Before
  public void before() {
    System.setProperty("env", "local");
  }

  @After
  public void after() {
    System.clearProperty("spring.profiles.active");
    System.clearProperty("env");
    ApolloMockInjectorCustomizer.clear();
  }

  @Test
  public void testDefaultConfigWithSystemProperties() {
    System.setProperty("spring.profiles.active", "test");
    ApolloMockInjectorCustomizer.register(ConfigFactory.class,
        DefaultConfigFactory::new);
    ConfigFactory configFactory = ApolloInjector.getInstance(ConfigFactory.class);
    Config config = configFactory.create("application");
    Assert.assertEquals("test", config.getProperty("spring.profiles.active", null));
  }

  @Test
  public void testPureApolloConfigWithSystemProperties() {
    System.setProperty("spring.profiles.active", "test");
    ApolloMockInjectorCustomizer.register(ConfigFactory.class,
        PureApolloConfigFactory::new);
    ConfigFactory configFactory = ApolloInjector.getInstance(ConfigFactory.class);
    Config config = configFactory.create("application");
    Assert.assertNull(config.getProperty("spring.profiles.active", null));
  }

  @Test
  public void testDefaultConfigWithEnvironmentVariables() throws Exception {
    SystemLambda.withEnvironmentVariable(
        "SPRING_PROFILES_ACTIVE",
        "test-env")
        .execute(() -> {
          ApolloMockInjectorCustomizer.register(ConfigFactory.class,
              DefaultConfigFactory::new);
          ConfigFactory configFactory = ApolloInjector.getInstance(ConfigFactory.class);
          Config config = configFactory.create("application");
          Assert.assertEquals("test-env", config.getProperty("SPRING_PROFILES_ACTIVE", null));
        });
  }

  @Test
  public void testPureApolloConfigWithEnvironmentVariables() throws Exception {
    SystemLambda.withEnvironmentVariable(
        "SPRING_PROFILES_ACTIVE",
        "test-env")
        .execute(() -> {
          ApolloMockInjectorCustomizer.register(ConfigFactory.class,
              PureApolloConfigFactory::new);
          ConfigFactory configFactory = ApolloInjector.getInstance(ConfigFactory.class);
          Config config = configFactory.create("application");
          Assert.assertNull(config.getProperty("SPRING_PROFILES_ACTIVE", null));
        });
  }
}
