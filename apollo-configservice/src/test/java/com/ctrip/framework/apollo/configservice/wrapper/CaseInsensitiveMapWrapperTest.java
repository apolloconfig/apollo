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
package com.ctrip.framework.apollo.configservice.wrapper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class CaseInsensitiveMapWrapperTest {
  private CaseInsensitiveMapWrapper<Object> caseInsensitiveMapWrapper;
  @Mock
  private Map<String, Object> someMap;

  @Before
  public void setUp() throws Exception {
    caseInsensitiveMapWrapper = new CaseInsensitiveMapWrapper<>(someMap);
  }

  @Test
  public void testGet() throws Exception {
    String someKey = "someKey";
    Object someValue = mock(Object.class);

    when(someMap.get(someKey.toLowerCase())).thenReturn(someValue);

    assertEquals(someValue, caseInsensitiveMapWrapper.get(someKey));

    verify(someMap, times(1)).get(someKey.toLowerCase());
  }

  @Test
  public void testPut() throws Exception {
    String someKey = "someKey";
    Object someValue = mock(Object.class);
    Object anotherValue = mock(Object.class);

    when(someMap.put(someKey.toLowerCase(), someValue)).thenReturn(anotherValue);

    assertEquals(anotherValue, caseInsensitiveMapWrapper.put(someKey, someValue));

    verify(someMap, times(1)).put(someKey.toLowerCase(), someValue);
  }

  @Test
  public void testRemove() throws Exception {
    String someKey = "someKey";
    Object someValue = mock(Object.class);

    when(someMap.remove(someKey.toLowerCase())).thenReturn(someValue);

    assertEquals(someValue, caseInsensitiveMapWrapper.remove(someKey));

    verify(someMap, times(1)).remove(someKey.toLowerCase());
  }
}
