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
package com.ctrip.framework.apollo.config.data.system;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.spring.boot.ApolloApplicationContextInitializer;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = ApolloClientPropertyCompatibleTestConfiguration.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test-compatible")
public class ApolloClientApplicationPropertiesCompatibleTest {

  @Autowired
  private ConfigurableEnvironment environment;

  @Test
  public void testApplicationPropertiesCompatible() {
    Assert.assertEquals("test-1/cacheDir",
        this.environment.getProperty(ApolloClientSystemConsts.APOLLO_CACHE_DIR));
    Assert.assertEquals("test-1-secret",
        this.environment.getProperty(ApolloClientSystemConsts.APOLLO_ACCESS_KEY_SECRET));
    Assert.assertEquals("https://test-1-config-service",
        this.environment.getProperty(ApolloClientSystemConsts.APOLLO_CONFIG_SERVICE));
  }

  @After
  public void clearProperty() {
    for (String propertyName : ApolloApplicationContextInitializer.APOLLO_SYSTEM_PROPERTIES) {
      System.clearProperty(propertyName);
    }
  }
}
