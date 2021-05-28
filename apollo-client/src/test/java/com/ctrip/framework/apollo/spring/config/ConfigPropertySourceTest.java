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
package com.ctrip.framework.apollo.spring.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

@RunWith(MockitoJUnitRunner.class)
public class ConfigPropertySourceTest {

  private ConfigPropertySource configPropertySource;

  @Mock
  private Config someConfig;

  @Before
  public void setUp() throws Exception {
    String someName = "someName";
    configPropertySource = new ConfigPropertySource(someName, someConfig);
  }

  @Test
  public void testGetPropertyNames() throws Exception {
    String somePropertyName = "somePropertyName";
    String anotherPropertyName = "anotherPropertyName";
    Set<String> somePropertyNames = Sets.newHashSet(somePropertyName, anotherPropertyName);

    when(someConfig.getPropertyNames()).thenReturn(somePropertyNames);

    String[] result = configPropertySource.getPropertyNames();

    verify(someConfig, times(1)).getPropertyNames();

    assertArrayEquals(somePropertyNames.toArray(), result);
  }

  @Test
  public void testGetEmptyPropertyNames() throws Exception {
    when(someConfig.getPropertyNames()).thenReturn(Sets.<String>newHashSet());

    assertEquals(0, configPropertySource.getPropertyNames().length);
  }

  @Test
  public void testGetProperty() throws Exception {
    String somePropertyName = "somePropertyName";

    String someValue = "someValue";

    when(someConfig.getProperty(somePropertyName, null)).thenReturn(someValue);

    assertEquals(someValue, configPropertySource.getProperty(somePropertyName));

    verify(someConfig, times(1)).getProperty(somePropertyName, null);
  }

  @Test
  public void testAddChangeListener() throws Exception {
    ConfigChangeListener someListener = mock(ConfigChangeListener.class);
    ConfigChangeListener anotherListener = mock(ConfigChangeListener.class);

    final List<ConfigChangeListener> listeners = Lists.newArrayList();

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        listeners.add(invocation.getArgumentAt(0, ConfigChangeListener.class));

        return Void.class;
      }
    }).when(someConfig).addChangeListener(any(ConfigChangeListener.class));

    configPropertySource.addChangeListener(someListener);
    configPropertySource.addChangeListener(anotherListener);

    assertEquals(2, listeners.size());
    assertTrue(listeners.containsAll(Lists.newArrayList(someListener, anotherListener)));
  }
}
