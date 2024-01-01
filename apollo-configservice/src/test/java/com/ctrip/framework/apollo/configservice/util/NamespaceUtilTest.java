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
package com.ctrip.framework.apollo.configservice.util;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.configservice.service.AppNamespaceServiceWithCache;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class NamespaceUtilTest {
  private NamespaceUtil namespaceUtil;

  @Mock
  private AppNamespaceServiceWithCache appNamespaceServiceWithCache;

  @Before
  public void setUp() throws Exception {
    namespaceUtil = new NamespaceUtil(appNamespaceServiceWithCache);
  }

  @Test
  public void testFilterNamespaceName() throws Exception {
    String someName = "a.properties";

    assertEquals("a", namespaceUtil.filterNamespaceName(someName));
  }

  @Test
  public void testFilterNamespaceNameUnchanged() throws Exception {
    String someName = "a.xml";

    assertEquals(someName, namespaceUtil.filterNamespaceName(someName));
  }

  @Test
  public void testFilterNamespaceNameWithMultiplePropertiesSuffix() throws Exception {
    String someName = "a.properties.properties";

    assertEquals("a.properties", namespaceUtil.filterNamespaceName(someName));
  }

  @Test
  public void testFilterNamespaceNameWithRandomCase() throws Exception {
    String someName = "AbC.ProPErties";

    assertEquals("AbC", namespaceUtil.filterNamespaceName(someName));
  }

  @Test
  public void testFilterNamespaceNameWithRandomCaseUnchanged() throws Exception {
    String someName = "AbCD.xMl";

    assertEquals(someName, namespaceUtil.filterNamespaceName(someName));
  }

  @Test
  public void testNormalizeNamespaceWithPrivateNamespace() throws Exception {
    String someAppId = "someAppId";
    String someNamespaceName = "someNamespaceName";
    String someNormalizedNamespaceName = "someNormalizedNamespaceName";
    AppNamespace someAppNamespace = mock(AppNamespace.class);

    when(someAppNamespace.getName()).thenReturn(someNormalizedNamespaceName);
    when(appNamespaceServiceWithCache.findByAppIdAndNamespace(someAppId, someNamespaceName)).thenReturn
        (someAppNamespace);

    assertEquals(someNormalizedNamespaceName, namespaceUtil.normalizeNamespace(someAppId, someNamespaceName));

    verify(appNamespaceServiceWithCache, times(1)).findByAppIdAndNamespace(someAppId, someNamespaceName);
    verify(appNamespaceServiceWithCache, never()).findPublicNamespaceByName(someNamespaceName);
  }

  @Test
  public void testNormalizeNamespaceWithPublicNamespace() throws Exception {
    String someAppId = "someAppId";
    String someNamespaceName = "someNamespaceName";
    String someNormalizedNamespaceName = "someNormalizedNamespaceName";
    AppNamespace someAppNamespace = mock(AppNamespace.class);

    when(someAppNamespace.getName()).thenReturn(someNormalizedNamespaceName);
    when(appNamespaceServiceWithCache.findByAppIdAndNamespace(someAppId, someNamespaceName)).thenReturn(null);
    when(appNamespaceServiceWithCache.findPublicNamespaceByName(someNamespaceName)).thenReturn(someAppNamespace);

    assertEquals(someNormalizedNamespaceName, namespaceUtil.normalizeNamespace(someAppId, someNamespaceName));

    verify(appNamespaceServiceWithCache, times(1)).findByAppIdAndNamespace(someAppId, someNamespaceName);
    verify(appNamespaceServiceWithCache, times(1)).findPublicNamespaceByName(someNamespaceName);
  }

  @Test
  public void testNormalizeNamespaceFailed() throws Exception {
    String someAppId = "someAppId";
    String someNamespaceName = "someNamespaceName";

    when(appNamespaceServiceWithCache.findByAppIdAndNamespace(someAppId, someNamespaceName)).thenReturn(null);
    when(appNamespaceServiceWithCache.findPublicNamespaceByName(someNamespaceName)).thenReturn(null);

    assertEquals(someNamespaceName, namespaceUtil.normalizeNamespace(someAppId, someNamespaceName));

    verify(appNamespaceServiceWithCache, times(1)).findByAppIdAndNamespace(someAppId, someNamespaceName);
    verify(appNamespaceServiceWithCache, times(1)).findPublicNamespaceByName(someNamespaceName);
  }
}
