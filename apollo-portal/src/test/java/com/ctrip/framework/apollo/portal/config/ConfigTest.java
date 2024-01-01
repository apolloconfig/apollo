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
package com.ctrip.framework.apollo.portal.config;


import com.ctrip.framework.apollo.portal.AbstractUnitTest;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.core.env.ConfigurableEnvironment;

import static org.mockito.Mockito.when;

public class ConfigTest extends AbstractUnitTest{

  @Mock
  private ConfigurableEnvironment environment;
  @InjectMocks
  private PortalConfig config;


  @Test
  public void testGetNotExistValue() {
    String testKey = "key";
    String testDefaultValue = "value";

    when(environment.getProperty(testKey, testDefaultValue)).thenReturn(testDefaultValue);

    Assert.assertEquals(testDefaultValue, config.getValue(testKey, testDefaultValue));
  }

  @Test
  public void testGetArrayProperty() {
    String testKey = "key";
    String testValue = "a,b,c";

    when(environment.getProperty(testKey)).thenReturn(testValue);

    String[] result = config.getArrayProperty(testKey, null);

    Assert.assertEquals(3, result.length);
    Assert.assertEquals("a", result[0]);
    Assert.assertEquals("b", result[1]);
    Assert.assertEquals("c", result[2]);
  }

  @Test
  public void testGetBooleanProperty() {
    String testKey = "key";
    String testValue = "true";

    when(environment.getProperty(testKey)).thenReturn(testValue);

    boolean result = config.getBooleanProperty(testKey, false);

    Assert.assertTrue(result);
  }

  @Test
  public void testGetIntProperty() {
    String testKey = "key";
    String testValue = "1024";

    when(environment.getProperty(testKey)).thenReturn(testValue);

    int result = config.getIntProperty(testKey, 0);

    Assert.assertEquals(1024, result);

  }


}
