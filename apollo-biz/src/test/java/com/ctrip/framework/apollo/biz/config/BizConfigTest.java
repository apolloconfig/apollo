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
package com.ctrip.framework.apollo.biz.config;

import com.ctrip.framework.apollo.biz.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.biz.service.BizDBPropertySource;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class BizConfigTest {

  @Mock
  private ConfigurableEnvironment environment;
  @Mock
  private ServerConfigRepository serverConfigRepository;

  @Mock
  private DataSource dataSource;

  private BizConfig bizConfig;

  @Before
  public void setUp() throws Exception {
    bizConfig = new BizConfig(new BizDBPropertySource(serverConfigRepository, dataSource, environment));
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
  public void testReleaseHistoryRetentionSize() {
    int someLimit = 20;
    when(environment.getProperty("apollo.release-history.retention.size")).thenReturn(String.valueOf(someLimit));

    assertEquals(someLimit, bizConfig.releaseHistoryRetentionSize());
  }

  @Test
  public void testReleaseHistoryRetentionSizeOverride() {
    int someOverrideLimit = 10;
    String overrideValueString = "{'a+b+c+b':10}";
    when(environment.getProperty("apollo.release-history.retention.size.override")).thenReturn(overrideValueString);
    int overrideValue = bizConfig.releaseHistoryRetentionSizeOverride().get("a+b+c+b");
    assertEquals(someOverrideLimit, overrideValue);

    overrideValueString = "{'a+b+c+b':0,'a+b+d+b':2}";
    when(environment.getProperty("apollo.release-history.retention.size.override")).thenReturn(overrideValueString);
    assertEquals(1, bizConfig.releaseHistoryRetentionSizeOverride().size());
    overrideValue = bizConfig.releaseHistoryRetentionSizeOverride().get("a+b+d+b");
    assertEquals(2, overrideValue);

    overrideValueString = "{}";
    when(environment.getProperty("apollo.release-history.retention.size.override")).thenReturn(overrideValueString);
    assertEquals(0, bizConfig.releaseHistoryRetentionSizeOverride().size());
  }

  @Test
  public void testAppIdValueLengthLimitOverride() {
    when(environment.getProperty("appid.value.length.limit.override")).thenReturn(null);
    Map<String, Integer> result = bizConfig.appIdValueLengthLimitOverride();
    assertTrue(result.isEmpty());

    String input = "{}";
    when(environment.getProperty("appid.value.length.limit.override")).thenReturn(input);
    result = bizConfig.appIdValueLengthLimitOverride();
    assertTrue(result.isEmpty());

    input = "invalid json";
    when(environment.getProperty("appid.value.length.limit.override")).thenReturn(input);
    result = bizConfig.appIdValueLengthLimitOverride();
    assertTrue(result.isEmpty());

    input = "{'appid1':555}";
    when(environment.getProperty("appid.value.length.limit.override")).thenReturn(input);
    int overrideValue = bizConfig.appIdValueLengthLimitOverride().get("appid1");
    assertEquals(1, bizConfig.appIdValueLengthLimitOverride().size());
    assertEquals(555, overrideValue);

    input = "{'appid1':555,'appid2':666}";
    when(environment.getProperty("appid.value.length.limit.override")).thenReturn(input);
    overrideValue = bizConfig.appIdValueLengthLimitOverride().get("appid2");
    assertEquals(2, bizConfig.appIdValueLengthLimitOverride().size());
    assertEquals(666, overrideValue);

    input = "{'appid1':555,'appid2':666,'appid3':0,'appid4':-1}";
    when(environment.getProperty("appid.value.length.limit.override")).thenReturn(input);
    result = bizConfig.appIdValueLengthLimitOverride();

    assertTrue(result.containsKey("appid1"));
    assertTrue(result.containsKey("appid2"));
    assertFalse(result.containsKey("appid3"));
    assertFalse(result.containsKey("appid4"));
    assertEquals(2, result.size());

    overrideValue = result.get("appid2");
    assertEquals(666, overrideValue);
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

  @Test
  public void testIsConfigServiceCacheKeyIgnoreCase() {
    assertFalse(bizConfig.isConfigServiceCacheKeyIgnoreCase());
    when(environment.getProperty("config-service.cache.key.ignore-case")).thenReturn("true");
    assertTrue(bizConfig.isConfigServiceCacheKeyIgnoreCase());
  }
}
