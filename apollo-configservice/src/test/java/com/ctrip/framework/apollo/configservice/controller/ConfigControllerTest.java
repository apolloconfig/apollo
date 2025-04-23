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
package com.ctrip.framework.apollo.configservice.controller;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.configservice.service.AppNamespaceServiceWithCache;
import com.ctrip.framework.apollo.configservice.service.config.ConfigService;
import com.ctrip.framework.apollo.configservice.service.config.IncrementalSyncService;
import com.ctrip.framework.apollo.configservice.util.InstanceConfigAuditUtil;
import com.ctrip.framework.apollo.configservice.util.NamespaceUtil;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.ctrip.framework.apollo.core.dto.ConfigurationChange;
import com.ctrip.framework.apollo.core.enums.ConfigSyncType;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigControllerTest {

  private ConfigController configController;
  @Mock
  private ConfigService configService;
  @Mock
  private IncrementalSyncService incrementalSyncService;
  @Mock
  private AppNamespaceServiceWithCache appNamespaceService;
  @Mock
  private BizConfig bizConfig;
  private String someAppId;
  private String someClusterName;
  private String defaultClusterName;
  private String defaultNamespaceName;
  private String somePublicNamespaceName;
  private String someDataCenter;
  private String someClientIp;
  private String someClientLabel;
  private String someMessagesAsString;
  @Mock
  private ApolloNotificationMessages someNotificationMessages;
  @Mock
  private Release someRelease;
  @Mock
  private Release somePublicRelease;

  @Mock
  private Release anotherPublicRelease;

  @Mock
  private Release anotherRelease;
  @Mock
  private NamespaceUtil namespaceUtil;
  @Mock
  private InstanceConfigAuditUtil instanceConfigAuditUtil;
  @Mock
  private HttpServletRequest someRequest;
  private Gson gson = new Gson();

  @Before
  public void setUp() throws Exception {
    configController = spy(new ConfigController(
        configService, incrementalSyncService, appNamespaceService, namespaceUtil,
        instanceConfigAuditUtil, gson, bizConfig
    ));

    someAppId = "1";
    someClusterName = "someClusterName";
    defaultClusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    defaultNamespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    somePublicNamespaceName = "somePublicNamespace";
    someDataCenter = "someDC";
    someClientIp = "someClientIp";
    someClientLabel = "someClientLabel";
    String someValidConfiguration = "{\"apollo.bar\": \"foo\"}";
    String somePublicConfiguration = "{\"apollo.public.bar\": \"foo\"}";

    when(someRelease.getAppId()).thenReturn(someAppId);
    when(someRelease.getClusterName()).thenReturn(someClusterName);
    when(someRelease.getConfigurations()).thenReturn(someValidConfiguration);
    when(somePublicRelease.getConfigurations()).thenReturn(somePublicConfiguration);
    when(namespaceUtil.filterNamespaceName(defaultNamespaceName)).thenReturn(defaultNamespaceName);
    when(namespaceUtil.filterNamespaceName(somePublicNamespaceName)).thenReturn(
        somePublicNamespaceName);
    when(namespaceUtil.normalizeNamespace(someAppId, defaultNamespaceName)).thenReturn(
        defaultNamespaceName);
    when(namespaceUtil.normalizeNamespace(someAppId, somePublicNamespaceName)).thenReturn(
        somePublicNamespaceName);

    someMessagesAsString = "someValidJson";
    when(configController.transformMessages(someMessagesAsString)).thenReturn(
        someNotificationMessages);
  }

  @Test
  public void testQueryConfig() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideNewReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, defaultNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideNewReleaseKey);
    when(someRelease.getNamespaceName()).thenReturn(defaultNamespaceName);

    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);

    verify(configService, times(1)).loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName,
        defaultNamespaceName, someDataCenter, someNotificationMessages);
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(defaultNamespaceName, result.getNamespaceName());
    assertEquals(someServerSideNewReleaseKey, result.getReleaseKey());
    verify(instanceConfigAuditUtil, times(1)).audit(someAppId, someClusterName, someDataCenter,
        someClientIp, someAppId, someClusterName, defaultNamespaceName,
        someServerSideNewReleaseKey);
  }

  @Test
  public void testQueryConfigFile() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideNewReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String someNamespaceName = String.format("%s.%s", defaultClusterName, "properties");

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, defaultNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideNewReleaseKey);
    when(namespaceUtil.filterNamespaceName(someNamespaceName)).thenReturn(defaultNamespaceName);
    when(namespaceUtil.normalizeNamespace(someAppId, defaultNamespaceName)).thenReturn(
        defaultNamespaceName);

    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        someNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);

    verify(configService, times(1)).loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName,
        defaultNamespaceName, someDataCenter, someNotificationMessages);
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(someNamespaceName, result.getNamespaceName());
    assertEquals(someServerSideNewReleaseKey, result.getReleaseKey());
  }

  @Test
  public void testQueryConfigFileWithPrivateNamespace() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideNewReleaseKey = "2";
    String somePrivateNamespace = "datasource";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePrivateNamespaceName = String.format("%s.%s", somePrivateNamespace, "xml");
    AppNamespace appNamespace = mock(AppNamespace.class);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, somePrivateNamespace,
        someDataCenter, someNotificationMessages)).thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideNewReleaseKey);
    when(namespaceUtil.filterNamespaceName(somePrivateNamespaceName)).thenReturn(
        somePrivateNamespace);
    when(namespaceUtil.normalizeNamespace(someAppId, somePrivateNamespace)).thenReturn(
        somePrivateNamespace);
    when(appNamespaceService.findByAppIdAndNamespace(someAppId, somePrivateNamespace))
        .thenReturn(appNamespace);

    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        somePrivateNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);

    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(somePrivateNamespaceName, result.getNamespaceName());
    assertEquals(someServerSideNewReleaseKey, result.getReleaseKey());
  }

  @Test
  public void testQueryConfigWithReleaseNotFound() throws Exception {
    String someClientSideReleaseKey = "1";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, defaultNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(null);

    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);

    assertNull(result);
    verify(someResponse, times(1)).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
  }

  @Test
  public void testQueryConfigWithApolloConfigNotModified() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = someClientSideReleaseKey;
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, defaultNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);

    ApolloConfig result =
        configController.queryConfig(someAppId, someClusterName, defaultNamespaceName,
            someDataCenter,
            someClientSideReleaseKey, someClientIp, someClientLabel, someMessagesAsString,
            someRequest, someResponse);

    assertNull(result);
    verify(someResponse, times(1)).setStatus(HttpServletResponse.SC_NOT_MODIFIED);
  }

  @Test
  public void testQueryConfigWithAppOwnNamespace() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = "2";
    String someAppOwnNamespaceName = "someAppOwn";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    AppNamespace someAppOwnNamespace =
        assemblePublicAppNamespace(someAppId, someAppOwnNamespaceName);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, someAppOwnNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(someRelease);
    when(appNamespaceService.findPublicNamespaceByName(someAppOwnNamespaceName))
        .thenReturn(someAppOwnNamespace);
    when(someRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);
    when(namespaceUtil.filterNamespaceName(someAppOwnNamespaceName)).thenReturn(
        someAppOwnNamespaceName);
    when(namespaceUtil.normalizeNamespace(someAppId, someAppOwnNamespaceName)).thenReturn(
        someAppOwnNamespaceName);

    ApolloConfig result =
        configController
            .queryConfig(someAppId, someClusterName, someAppOwnNamespaceName, someDataCenter,
                someClientSideReleaseKey, someClientIp, someClientLabel, someMessagesAsString,
                someRequest, someResponse);

    assertEquals(someServerSideReleaseKey, result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(someAppOwnNamespaceName, result.getNamespaceName());
    assertEquals("foo", result.getConfigurations().get("apollo.bar"));
  }

  @Test
  public void testQueryConfigWithPubicNamespaceAndNoAppOverride() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    String somePublicClusterName = "somePublicClusterName";
    AppNamespace somePublicAppNamespace =
        assemblePublicAppNamespace(somePublicAppId, somePublicNamespaceName);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(null);
    when(appNamespaceService.findPublicNamespaceByName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, somePublicAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(somePublicRelease);
    when(somePublicRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);
    when(somePublicRelease.getAppId()).thenReturn(somePublicAppId);
    when(somePublicRelease.getClusterName()).thenReturn(somePublicClusterName);
    when(somePublicRelease.getNamespaceName()).thenReturn(somePublicNamespaceName);

    ApolloConfig result = configController
        .queryConfig(someAppId, someClusterName, somePublicNamespaceName, someDataCenter,
            someClientSideReleaseKey, someClientIp, someClientLabel, someMessagesAsString,
            someRequest, someResponse);

    assertEquals(someServerSideReleaseKey, result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(somePublicNamespaceName, result.getNamespaceName());
    assertEquals("foo", result.getConfigurations().get("apollo.public.bar"));
    verify(instanceConfigAuditUtil, times(1)).audit(someAppId, someClusterName, someDataCenter,
        someClientIp, somePublicAppId, somePublicClusterName, somePublicNamespaceName,
        someServerSideReleaseKey);
  }

  @Test
  public void testQueryConfigFileWithPublicNamespaceAndNoAppOverride() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    String someNamespace = String.format("%s.%s", somePublicNamespaceName, "properties");
    AppNamespace somePublicAppNamespace =
        assemblePublicAppNamespace(somePublicAppId, somePublicNamespaceName);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(null);
    when(appNamespaceService.findPublicNamespaceByName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, somePublicAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(somePublicRelease);
    when(somePublicRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);
    when(namespaceUtil.filterNamespaceName(someNamespace)).thenReturn(somePublicNamespaceName);
    when(namespaceUtil.normalizeNamespace(someAppId, somePublicNamespaceName)).thenReturn(
        somePublicNamespaceName);
    when(
        appNamespaceService.findByAppIdAndNamespace(someAppId, somePublicNamespaceName)).thenReturn(
        null);

    ApolloConfig result = configController
        .queryConfig(someAppId, someClusterName, someNamespace, someDataCenter,
            someClientSideReleaseKey, someClientIp, someClientLabel, someMessagesAsString,
            someRequest, someResponse);

    assertEquals(someServerSideReleaseKey, result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(someNamespace, result.getNamespaceName());
    assertEquals("foo", result.getConfigurations().get("apollo.public.bar"));
  }

  @Test
  public void testQueryConfigWithPublicNamespaceAndAppOverride() throws Exception {
    String someAppSideReleaseKey = "1";
    String somePublicAppSideReleaseKey = "2";

    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    AppNamespace somePublicAppNamespace =
        assemblePublicAppNamespace(somePublicAppId, somePublicNamespaceName);

    when(someRelease.getConfigurations()).thenReturn("{\"apollo.public.foo\": \"foo-override\"}");
    when(somePublicRelease.getConfigurations())
        .thenReturn("{\"apollo.public.foo\": \"foo\", \"apollo.public.bar\": \"bar\"}");

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(someRelease);
    when(someRelease.getReleaseKey()).thenReturn(someAppSideReleaseKey);
    when(someRelease.getNamespaceName()).thenReturn(somePublicNamespaceName);
    when(appNamespaceService.findPublicNamespaceByName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, somePublicAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(somePublicRelease);
    when(somePublicRelease.getReleaseKey()).thenReturn(somePublicAppSideReleaseKey);
    when(somePublicRelease.getAppId()).thenReturn(somePublicAppId);
    when(somePublicRelease.getClusterName()).thenReturn(someDataCenter);
    when(somePublicRelease.getNamespaceName()).thenReturn(somePublicNamespaceName);

    ApolloConfig result =
        configController
            .queryConfig(someAppId, someClusterName, somePublicNamespaceName, someDataCenter,
                someAppSideReleaseKey, someClientIp, someClientLabel, someMessagesAsString,
                someRequest, someResponse);

    assertEquals(Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
            .join(someAppSideReleaseKey, somePublicAppSideReleaseKey),
        result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(somePublicNamespaceName, result.getNamespaceName());
    assertEquals("foo-override", result.getConfigurations().get("apollo.public.foo"));
    assertEquals("bar", result.getConfigurations().get("apollo.public.bar"));
    verify(instanceConfigAuditUtil, times(1)).audit(someAppId, someClusterName, someDataCenter,
        someClientIp, someAppId, someClusterName, somePublicNamespaceName, someAppSideReleaseKey);
    verify(instanceConfigAuditUtil, times(1)).audit(someAppId, someClusterName, someDataCenter,
        someClientIp, somePublicAppId, someDataCenter, somePublicNamespaceName,
        somePublicAppSideReleaseKey);
  }

  @Test
  public void testMergeConfigurations() throws Exception {
    Gson gson = new Gson();
    String key1 = "key1";
    String value1 = "value1";
    String anotherValue1 = "anotherValue1";

    String key2 = "key2";
    String value2 = "value2";

    Map<String, String> config = ImmutableMap.of(key1, anotherValue1);
    Map<String, String> anotherConfig = ImmutableMap.of(key1, value1, key2, value2);

    Release releaseWithHighPriority = new Release();
    releaseWithHighPriority.setConfigurations(gson.toJson(config));

    Release releaseWithLowPriority = new Release();
    releaseWithLowPriority.setConfigurations(gson.toJson(anotherConfig));

    Map<String, String> result =
        configController.mergeReleaseConfigurations(
            Lists.newArrayList(releaseWithHighPriority, releaseWithLowPriority));

    assertEquals(2, result.keySet().size());
    assertEquals(anotherValue1, result.get(key1));
    assertEquals(value2, result.get(key2));
  }

  @Test(expected = JsonSyntaxException.class)
  public void testTransformConfigurationToMapFailed() throws Exception {
    String someInvalidConfiguration = "xxx";
    Release someRelease = new Release();
    someRelease.setConfigurations(someInvalidConfiguration);

    configController.mergeReleaseConfigurations(Lists.newArrayList(someRelease));
  }

  @Test
  public void testQueryConfigForNoAppIdPlaceHolder() throws Exception {
    String someClientSideReleaseKey = "1";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String appId = ConfigConsts.NO_APPID_PLACEHOLDER;

    ApolloConfig result = configController.queryConfig(appId, someClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);

    verify(configService, never()).loadConfig(appId, someClientIp, someAppId, someClientLabel,
        someClusterName, defaultNamespaceName,
        someDataCenter, someNotificationMessages);
    verify(appNamespaceService, never()).findPublicNamespaceByName(defaultNamespaceName);
    assertNull(result);
    verify(someResponse, times(1)).sendError(eq(HttpServletResponse.SC_NOT_FOUND), anyString());
  }

  @Test
  public void testQueryConfigForNoAppIdPlaceHolderWithPublicNamespace() throws Exception {
    String someClientSideReleaseKey = "1";
    String someServerSideReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    AppNamespace somePublicAppNamespace =
        assemblePublicAppNamespace(somePublicAppId, somePublicNamespaceName);
    String appId = ConfigConsts.NO_APPID_PLACEHOLDER;

    when(appNamespaceService.findPublicNamespaceByName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.loadConfig(appId, someClientIp, someClientLabel, somePublicAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(somePublicRelease);
    when(somePublicRelease.getReleaseKey()).thenReturn(someServerSideReleaseKey);
    when(namespaceUtil.normalizeNamespace(appId, somePublicNamespaceName)).thenReturn(
        somePublicNamespaceName);

    ApolloConfig result = configController.queryConfig(appId, someClusterName,
        somePublicNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);

    verify(configService, never()).loadConfig(appId, someClientIp, someClientLabel, appId,
        someClusterName,
        somePublicNamespaceName, someDataCenter, someNotificationMessages);
    assertEquals(someServerSideReleaseKey, result.getReleaseKey());
    assertEquals(appId, result.getAppId());
    assertEquals(someClusterName, result.getCluster());
    assertEquals(somePublicNamespaceName, result.getNamespaceName());
    assertEquals("foo", result.getConfigurations().get("apollo.public.bar"));
  }

  @Test
  public void testTransformMessages() throws Exception {
    String someKey = "someKey";
    long someNotificationId = 1;
    String anotherKey = "anotherKey";
    long anotherNotificationId = 2;
    ApolloNotificationMessages notificationMessages = new ApolloNotificationMessages();
    notificationMessages.put(someKey, someNotificationId);
    notificationMessages.put(anotherKey, anotherNotificationId);

    String someMessagesAsString = gson.toJson(notificationMessages);

    ApolloNotificationMessages result = configController.transformMessages(someMessagesAsString);

    assertEquals(notificationMessages.getDetails(), result.getDetails());
  }

  @Test
  public void testTransformInvalidMessages() throws Exception {
    String someInvalidMessages = "someInvalidMessages";

    assertNull(configController.transformMessages(someInvalidMessages));
  }

  private AppNamespace assemblePublicAppNamespace(String appId, String namespace) {
    return assembleAppNamespace(appId, namespace, true);
  }

  private AppNamespace assembleAppNamespace(String appId, String namespace, boolean isPublic) {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setAppId(appId);
    appNamespace.setName(namespace);
    appNamespace.setPublic(isPublic);
    return appNamespace;
  }

  @Test
  public void testQueryConfigWithIncrementalSync() throws Exception {
    when(bizConfig.isConfigServiceIncrementalChangeEnabled())
        .thenReturn(true);
    String clientSideReleaseKey = "1";
    String someConfigurations = "{\"apollo.public.foo\": \"foo\"}";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    ImmutableMap<String, Release> someReleaseMap = mock(ImmutableMap.class);

    String anotherConfigurations = "{\"apollo.public.foo\": \"foo\", \"apollo.public.bar\": \"bar\"}";

    when(configService.findReleasesByReleaseKeys(Sets.newHashSet(clientSideReleaseKey))).thenReturn(
        someReleaseMap);
    when(someReleaseMap.get(clientSideReleaseKey)).thenReturn(someRelease);
    when(someRelease.getConfigurations()).thenReturn(someConfigurations);
    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, defaultNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(anotherRelease);
    when(anotherRelease.getNamespaceName()).thenReturn(defaultNamespaceName);
    when(anotherRelease.getConfigurations()).thenReturn(anotherConfigurations);

    List<ConfigurationChange> configurationChanges = new ArrayList<>();
    configurationChanges.add(new ConfigurationChange("apollo.public.bar", "bar", "ADDED"));
    when(incrementalSyncService.getConfigurationChanges("someServerSideNewReleaseKey",
        gson.fromJson(anotherConfigurations, configurationTypeReference),
        "someServerSideNewReleaseKey",
        gson.fromJson(someConfigurations, configurationTypeReference)))
        .thenReturn(configurationChanges);

    ApolloConfig anotherResult = configController.queryConfig(someAppId, someClusterName,
        defaultNamespaceName, someDataCenter, clientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);
    assertEquals(ConfigSyncType.INCREMENTAL_SYNC.getValue(), anotherResult.getConfigSyncType());
    assertEquals(configurationChanges, anotherResult.getConfigurationChanges());

  }

  @Test
  public void testQueryConfigWithIncrementalSyncNotFound() throws Exception {
    when(bizConfig.isConfigServiceIncrementalChangeEnabled())
        .thenReturn(true);

    String someClientSideReleaseKey = "1";
    String someServerSideNewReleaseKey = "2";
    HttpServletResponse someResponse = mock(HttpServletResponse.class);

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, defaultNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(someRelease);
    when(configService.findReleasesByReleaseKeys(
        Sets.newHashSet(someClientSideReleaseKey))).thenReturn(null);

    when(someRelease.getReleaseKey()).thenReturn(someServerSideNewReleaseKey);
    when(someRelease.getNamespaceName()).thenReturn(defaultNamespaceName);
    String configurations = "{\"apollo.public.foo\": \"foo\"}";
    when(someRelease.getConfigurations()).thenReturn(configurations);

    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        defaultNamespaceName, someDataCenter, someClientSideReleaseKey,
        someClientIp, someClientLabel, someMessagesAsString, someRequest, someResponse);
    assertEquals(1, result.getConfigurations().size());
    assertEquals("foo", result.getConfigurations().get("apollo.public.foo"));
  }

  @Test
  public void testQueryConfigWithIncrementalSyncPublicNamespaceAndAppOverride() throws Exception {
    when(bizConfig.isConfigServiceIncrementalChangeEnabled())
        .thenReturn(true);
    String someAppClientSideReleaseKey = "1";
    String somePublicAppClientSideReleaseKey = "2";
    String someConfigurations = "{\"apollo.public.foo.client\": \"foo.override\"}";
    String somePublicConfigurations = "{\"apollo.public.foo.client\": \"foo\"}";
    ImmutableMap<String, Release> someReleaseMap = mock(ImmutableMap.class);
    Release somePublicRelease = mock(Release.class);

    when(configService.findReleasesByReleaseKeys(Sets.newHashSet(someAppClientSideReleaseKey,
        somePublicAppClientSideReleaseKey))).thenReturn(someReleaseMap);
    when(someReleaseMap.get(someAppClientSideReleaseKey)).thenReturn(someRelease);
    when(someReleaseMap.get(somePublicAppClientSideReleaseKey)).thenReturn(somePublicRelease);
    when(someRelease.getConfigurations()).thenReturn(someConfigurations);
    when(somePublicRelease.getConfigurations()).thenReturn(somePublicConfigurations);

    String someAppServerSideReleaseKey = "3";
    String somePublicAppSideReleaseKey = "4";

    HttpServletResponse someResponse = mock(HttpServletResponse.class);
    String somePublicAppId = "somePublicAppId";
    AppNamespace somePublicAppNamespace =
        assemblePublicAppNamespace(somePublicAppId, somePublicNamespaceName);

    when(anotherRelease.getConfigurations()).thenReturn(
        "{\"apollo.public.foo\": \"foo-override\"}");
    when(anotherPublicRelease.getConfigurations())
        .thenReturn("{\"apollo.public.foo\": \"foo\", \"apollo.public.bar\": \"bar\"}");

    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, someAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(anotherRelease);
    when(anotherRelease.getReleaseKey()).thenReturn(someAppServerSideReleaseKey);
    when(anotherRelease.getNamespaceName()).thenReturn(somePublicNamespaceName);
    when(appNamespaceService.findPublicNamespaceByName(somePublicNamespaceName))
        .thenReturn(somePublicAppNamespace);
    when(configService.loadConfig(someAppId, someClientIp, someClientLabel, somePublicAppId,
        someClusterName, somePublicNamespaceName,
        someDataCenter, someNotificationMessages)).thenReturn(anotherPublicRelease);
    when(anotherPublicRelease.getReleaseKey()).thenReturn(somePublicAppSideReleaseKey);
    when(anotherPublicRelease.getAppId()).thenReturn(somePublicAppId);
    when(anotherPublicRelease.getClusterName()).thenReturn(someDataCenter);
    when(anotherPublicRelease.getNamespaceName()).thenReturn(somePublicNamespaceName);

    String mergeServerSideConfigurations = "{\"apollo.public.bar\": \"bar\",\"apollo.public.foo\": \"foo-override\"}";
    String mergeClientSideConfigurations = "{\"apollo.public.foo.client\": \"foo.override\"}";
    List<ConfigurationChange> configurationChanges = new ArrayList<>();
    configurationChanges.add(new ConfigurationChange("apollo.public.bar", "bar", "ADDED"));
    configurationChanges.add(new ConfigurationChange("apollo.public.foo", "foo-override", "ADDED"));
    configurationChanges.add(new ConfigurationChange("apollo.public.foo.client", null, "DELETED"));
    when(incrementalSyncService.getConfigurationChanges("",
        gson.fromJson(mergeServerSideConfigurations, configurationTypeReference),
        "",
        gson.fromJson(mergeClientSideConfigurations, configurationTypeReference)))
        .thenReturn(configurationChanges);

    String mergeClientSideReleaseKey = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
        .join(someAppClientSideReleaseKey, somePublicAppClientSideReleaseKey);
    ApolloConfig result = configController.queryConfig(someAppId, someClusterName,
        somePublicNamespaceName, someDataCenter,
        mergeClientSideReleaseKey, someClientIp, someClientLabel, someMessagesAsString, someRequest,
        someResponse);

    assertEquals(Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
            .join(someAppServerSideReleaseKey, somePublicAppSideReleaseKey),
        result.getReleaseKey());
    assertEquals(ConfigSyncType.INCREMENTAL_SYNC.getValue(), result.getConfigSyncType());
    assertEquals(configurationChanges, result.getConfigurationChanges());
  }


  private static final Type configurationTypeReference = new TypeToken<Map<String, String>>() {
  }.getType();
}
