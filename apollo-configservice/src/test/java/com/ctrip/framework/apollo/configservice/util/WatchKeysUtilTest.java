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
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collection;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class WatchKeysUtilTest {
  @Mock
  private AppNamespaceServiceWithCache appNamespaceService;
  @Mock
  private AppNamespace someAppNamespace;
  @Mock
  private AppNamespace anotherAppNamespace;
  @Mock
  private AppNamespace somePublicAppNamespace;
  private WatchKeysUtil watchKeysUtil;
  private String someAppId;
  private String someCluster;
  private String someNamespace;
  private String anotherNamespace;
  private String somePublicNamespace;
  private String defaultCluster;
  private String someDC;
  private String somePublicAppId;

  @Before
  public void setUp() throws Exception {
    watchKeysUtil = new WatchKeysUtil(appNamespaceService);

    someAppId = "someId";
    someCluster = "someCluster";
    someNamespace = "someName";
    anotherNamespace = "anotherName";
    somePublicNamespace = "somePublicName";
    defaultCluster = ConfigConsts.CLUSTER_NAME_DEFAULT;
    someDC = "someDC";
    somePublicAppId = "somePublicId";

    when(someAppNamespace.getName()).thenReturn(someNamespace);
    when(anotherAppNamespace.getName()).thenReturn(anotherNamespace);
    when(appNamespaceService.findByAppIdAndNamespaces(someAppId, Sets.newHashSet(someNamespace)))
        .thenReturn(Lists.newArrayList(someAppNamespace));
    when(appNamespaceService
        .findByAppIdAndNamespaces(someAppId, Sets.newHashSet(someNamespace, anotherNamespace)))
        .thenReturn(Lists.newArrayList(someAppNamespace, anotherAppNamespace));
    when(appNamespaceService
        .findByAppIdAndNamespaces(someAppId,
            Sets.newHashSet(someNamespace, anotherNamespace, somePublicNamespace)))
        .thenReturn(Lists.newArrayList(someAppNamespace, anotherAppNamespace));

    when(somePublicAppNamespace.getAppId()).thenReturn(somePublicAppId);
    when(somePublicAppNamespace.getName()).thenReturn(somePublicNamespace);
    when(appNamespaceService.findPublicNamespacesByNames(Sets.newHashSet(somePublicNamespace)))
        .thenReturn(Lists.newArrayList(somePublicAppNamespace));
    when(appNamespaceService.findPublicNamespacesByNames(Sets.newHashSet(someNamespace, somePublicNamespace)))
        .thenReturn(Lists.newArrayList(somePublicAppNamespace));
  }

  @Test
  public void testAssembleAllWatchKeysWithOneNamespaceAndDefaultCluster() throws Exception {
    Set<String> watchKeys =
        watchKeysUtil.assembleAllWatchKeys(someAppId, defaultCluster, someNamespace, null);

    Set<String> clusters = Sets.newHashSet(defaultCluster);

    assertEquals(clusters.size(), watchKeys.size());
    assertWatchKeys(someAppId, clusters, someNamespace, watchKeys);
  }

  @Test
  public void testAssembleAllWatchKeysWithOneNamespaceAndSomeDC() throws Exception {
    Set<String> watchKeys =
        watchKeysUtil.assembleAllWatchKeys(someAppId, someDC, someNamespace, someDC);

    Set<String> clusters = Sets.newHashSet(defaultCluster, someDC);

    assertEquals(clusters.size(), watchKeys.size());
    assertWatchKeys(someAppId, clusters, someNamespace, watchKeys);
  }

  @Test
  public void testAssembleAllWatchKeysWithOneNamespaceAndSomeDCAndSomeCluster() throws Exception {
    Set<String> watchKeys =
        watchKeysUtil.assembleAllWatchKeys(someAppId, someCluster, someNamespace, someDC);

    Set<String> clusters = Sets.newHashSet(defaultCluster, someCluster, someDC);

    assertEquals(clusters.size(), watchKeys.size());
    assertWatchKeys(someAppId, clusters, someNamespace, watchKeys);
  }

  @Test
  public void testAssembleAllWatchKeysWithMultipleNamespaces() throws Exception {
    Multimap<String, String> watchKeysMap =
        watchKeysUtil.assembleAllWatchKeys(someAppId, someCluster,
            Sets.newHashSet(someNamespace, anotherNamespace), someDC);

    Set<String> clusters = Sets.newHashSet(defaultCluster, someCluster, someDC);

    assertEquals(clusters.size() * 2, watchKeysMap.size());
    assertWatchKeys(someAppId, clusters, someNamespace, watchKeysMap.get(someNamespace));
    assertWatchKeys(someAppId, clusters, anotherNamespace, watchKeysMap.get(anotherNamespace));
  }

  @Test
  public void testAssembleAllWatchKeysWithPrivateAndPublicNamespaces() throws Exception {
    Multimap<String, String> watchKeysMap =
        watchKeysUtil.assembleAllWatchKeys(someAppId, someCluster,
            Sets.newHashSet(someNamespace, anotherNamespace, somePublicNamespace), someDC);

    Set<String> clusters = Sets.newHashSet(defaultCluster, someCluster, someDC);

    assertEquals(clusters.size() * 4, watchKeysMap.size());

    assertWatchKeys(someAppId, clusters, someNamespace, watchKeysMap.get(someNamespace));
    assertWatchKeys(someAppId, clusters, anotherNamespace, watchKeysMap.get(anotherNamespace));
    assertWatchKeys(someAppId, clusters, somePublicNamespace, watchKeysMap.get(somePublicNamespace));
    assertWatchKeys(somePublicAppId, clusters, somePublicNamespace, watchKeysMap.get(somePublicNamespace));
  }

  @Test
  public void testAssembleWatchKeysForNoAppIdPlaceHolder() throws Exception {
    Multimap<String, String> watchKeysMap =
        watchKeysUtil.assembleAllWatchKeys(ConfigConsts.NO_APPID_PLACEHOLDER, someCluster,
            Sets.newHashSet(someNamespace, anotherNamespace), someDC);

    assertTrue(watchKeysMap.isEmpty());
  }

  @Test
  public void testAssembleWatchKeysForNoAppIdPlaceHolderAndPublicNamespace() throws Exception {
    Multimap<String, String> watchKeysMap =
        watchKeysUtil.assembleAllWatchKeys(ConfigConsts.NO_APPID_PLACEHOLDER, someCluster,
            Sets.newHashSet(someNamespace, somePublicNamespace), someDC);

    Set<String> clusters = Sets.newHashSet(defaultCluster, someCluster, someDC);

    assertEquals(clusters.size(), watchKeysMap.size());

    assertWatchKeys(somePublicAppId, clusters, somePublicNamespace, watchKeysMap.get(somePublicNamespace));
  }

  private void assertWatchKeys(String appId, Set<String> clusters, String namespaceName,
                               Collection<String> watchedKeys) {
    for (String cluster : clusters) {
      String key =
          Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
              .join(appId, cluster, namespaceName);
      assertTrue(watchedKeys.contains(key));
    }
  }
}
