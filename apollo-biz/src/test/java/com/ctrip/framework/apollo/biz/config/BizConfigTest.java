/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.biz.config;

import com.ctrip.framework.apollo.biz.service.BizDBPropertySource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class BizConfigTest {

  @Mock
  private ConfigurableEnvironment environment;

  private BizConfig bizConfig;

  @Before
  public void setUp() throws Exception {
    bizConfig = new BizConfig(new BizDBPropertySource());
    ReflectionTestUtils.setField(bizConfig, "environment", environment);
  }

  @Test
  public void testReleaseMessageNotificationBatch() throws Exception {
    int someBatch = 20;
    when(environment.getProperty("apollo.release-message.notification.batch")).thenReturn(String.valueOf(someBatch));

    assertEquals(someBatch, bizConfig.releaseMessageNotificationBatch());
  }

  @Test
  public void testReleaseMessageNotificationBatchWithDefaultValue() throws Exception {
    int defaultBatch = 100;

    assertEquals(defaultBatch, bizConfig.releaseMessageNotificationBatch());
  }

  @Test
  public void testReleaseMessageNotificationBatchWithInvalidNumber() throws Exception {
    int someBatch = -20;
    int defaultBatch = 100;
    when(environment.getProperty("apollo.release-message.notification.batch")).thenReturn(String.valueOf(someBatch));

    assertEquals(defaultBatch, bizConfig.releaseMessageNotificationBatch());
  }

  @Test
  public void testReleaseMessageNotificationBatchWithNAN() throws Exception {
    String someNAN = "someNAN";
    int defaultBatch = 100;
    when(environment.getProperty("apollo.release-message.notification.batch")).thenReturn(someNAN);

    assertEquals(defaultBatch, bizConfig.releaseMessageNotificationBatch());
  }

  @Test
  public void testCheckInt() throws Exception {
    int someInvalidValue = 1;
    int anotherInvalidValue = 2;
    int someValidValue = 3;
    int someDefaultValue = 10;

    int someMin = someInvalidValue + 1;
    int someMax = anotherInvalidValue - 1;

    assertEquals(someDefaultValue, bizConfig.checkInt(someInvalidValue, someMin, Integer.MAX_VALUE, someDefaultValue));
    assertEquals(someDefaultValue, bizConfig.checkInt(anotherInvalidValue, Integer.MIN_VALUE, someMax,
        someDefaultValue));
    assertEquals(someValidValue, bizConfig.checkInt(someValidValue, Integer.MIN_VALUE, Integer.MAX_VALUE,
        someDefaultValue));
  }
}
