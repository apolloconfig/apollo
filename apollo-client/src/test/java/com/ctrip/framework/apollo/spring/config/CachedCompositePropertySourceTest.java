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
package com.ctrip.framework.apollo.spring.config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.assertj.core.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.PropertySource;

/**
 * @author Shawyeok (shawyeok@outlook.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class CachedCompositePropertySourceTest {

  private CachedCompositePropertySource compositeSource;

  @Mock
  private ConfigPropertySource configPropertySource;

  private List<ConfigChangeListener> listeners;

  @Before
  public void setUp() throws Exception {
    compositeSource = new CachedCompositePropertySource("testCompositeSource");
    listeners = new LinkedList<>();
    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ConfigChangeListener listener = invocation.getArgument(0, ConfigChangeListener.class);
        listeners.add(listener);
        return Void.class;
      }
    }).when(configPropertySource).addChangeListener(any(ConfigChangeListener.class));
    compositeSource.addPropertySource(configPropertySource);
  }

  @Test
  public void testGetPropertyNames() {
    String[] propertyNames = Arrays.array("propertyName");
    String[] anotherPropertyNames = Arrays.array("propertyName", "anotherPropertyName");

    when(configPropertySource.getPropertyNames()).thenReturn(propertyNames, anotherPropertyNames);

    String[] returnedPropertyNames = compositeSource.getPropertyNames();
    assertArrayEquals(propertyNames, returnedPropertyNames);
    assertSame(returnedPropertyNames, compositeSource.getPropertyNames());

    listeners.get(0).onChange(new ConfigChangeEvent(null, null));

    returnedPropertyNames = compositeSource.getPropertyNames();
    assertArrayEquals(anotherPropertyNames, returnedPropertyNames);
    assertSame(returnedPropertyNames, compositeSource.getPropertyNames());
  }

  @Test
  public void testAddPropertySource() {
    verify(configPropertySource, times(1))
        .addChangeListener(any(CachedCompositePropertySource.class));
    assertEquals(1, listeners.size());
    assertTrue(compositeSource.getPropertySources().contains(configPropertySource));
  }

  @Test
  public void testAddFirstPropertySource() {
    ConfigPropertySource anotherSource = mock(ConfigPropertySource.class);
    final List<ConfigChangeListener> anotherListenerList = new LinkedList<>();
    Mockito.doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        ConfigChangeListener listener = invocation.getArgument(0, ConfigChangeListener.class);
        anotherListenerList.add(listener);
        return Void.class;
      }
    }).when(anotherSource).addChangeListener(any(ConfigChangeListener.class));
    compositeSource.addFirstPropertySource(anotherSource);

    Collection<PropertySource<?>> propertySources = compositeSource.getPropertySources();
    Iterator<PropertySource<?>> it = propertySources.iterator();

    assertEquals(2, propertySources.size());
    assertEquals(1, anotherListenerList.size());
    assertSame(anotherSource, it.next());
    assertSame(configPropertySource, it.next());
  }
}