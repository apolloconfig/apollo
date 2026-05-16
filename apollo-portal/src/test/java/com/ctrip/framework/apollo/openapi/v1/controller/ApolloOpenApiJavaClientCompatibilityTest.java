/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.v1.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.SkipAuthorizationConfiguration;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.openapi.client.ApolloOpenApiClient;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenOrganizationDto;
import com.ctrip.framework.apollo.openapi.dto.OpenPageDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.server.service.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.server.service.ClusterOpenApiService;
import com.ctrip.framework.apollo.openapi.server.service.ItemOpenApiService;
import com.ctrip.framework.apollo.openapi.server.service.OrganizationOpenApiService;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.PortalApplication;
import com.ctrip.framework.apollo.portal.component.AdminServiceAddressLocator;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Compatibility tests for the published apollo-openapi Java client against Portal OpenAPI routes.
 */
@SpringBootTest(classes = {PortalApplication.class, SkipAuthorizationConfiguration.class},
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("skipAuthorization")
@TestPropertySource(properties = {"api.pool.max.total=100", "api.pool.max.per.route=100",
    "api.connectionTimeToLive=30000", "api.connectTimeout=5000", "api.readTimeout=5000"})
public class ApolloOpenApiJavaClientCompatibilityTest {

  private static final String APP_ID = "legacy-app";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String OPERATOR = "legacy-operator";

  @Value("${local.server.port}")
  private int port;

  @MockitoBean(name = "unifiedPermissionValidator")
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @MockitoBean
  private AppOpenApiService appOpenApiService;

  @MockitoBean
  private ClusterOpenApiService clusterOpenApiService;

  @MockitoBean
  private ItemOpenApiService itemOpenApiService;

  @MockitoBean
  private OrganizationOpenApiService organizationOpenApiService;

  @MockitoBean
  private PortalSettings portalSettings;

  @MockitoBean
  private AdminServiceAddressLocator adminServiceAddressLocator;

  @MockitoBean
  private com.ctrip.framework.apollo.openapi.api.NamespaceOpenApiService namespaceOpenApiService;

  @MockitoBean
  private com.ctrip.framework.apollo.openapi.api.ReleaseOpenApiService releaseOpenApiService;

  @MockitoBean
  private com.ctrip.framework.apollo.openapi.api.InstanceOpenApiService instanceOpenApiService;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private UserInfoHolder userInfoHolder;

  @MockitoBean
  private ConsumerService consumerService;

  @MockitoBean
  private RolePermissionService rolePermissionService;

  @MockitoBean
  private ReleaseService releaseService;

  @MockitoBean
  private NamespaceBranchService namespaceBranchService;

  @MockitoBean
  private ApplicationEventPublisher applicationEventPublisher;

  private ApolloOpenApiClient client;

  @BeforeEach
  public void setUp() {
    client = ApolloOpenApiClient.newBuilder().withPortalUrl("http://localhost:" + port)
        .withToken("legacy-token").build();

    UserInfo operator = new UserInfo();
    operator.setUserId(OPERATOR);
    when(userService.findByUserId(anyString())).thenReturn(operator);
    when(userInfoHolder.getUser()).thenReturn(operator);

    when(unifiedPermissionValidator.hasCreateApplicationPermission()).thenReturn(true);
    when(unifiedPermissionValidator.hasCreateClusterPermission(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.hasCreateNamespacePermission(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.hasModifyNamespacePermission(anyString(), anyString(),
        anyString(), anyString())).thenReturn(true);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission(anyString(), anyString(),
        anyString(), anyString())).thenReturn(true);
    when(unifiedPermissionValidator.isAppAdmin(anyString())).thenReturn(true);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(anyString(), anyString(),
        anyString(), anyString())).thenReturn(false);
  }

  @Test
  public void legacyAppAndOrganizationMethodsShouldRemainCompatible() {
    OpenCreateAppDTO createAppRequest = new OpenCreateAppDTO();
    OpenAppDTO createApp = legacyApp("created-app");
    createApp.setDataChangeCreatedBy(OPERATOR);
    createAppRequest.setApp(createApp);
    createAppRequest.setAssignAppRoleToSelf(false);
    client.createApp(createAppRequest);
    ArgumentCaptor<com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO> createAppCaptor =
        ArgumentCaptor.forClass(com.ctrip.framework.apollo.openapi.model.OpenCreateAppDTO.class);
    verify(appOpenApiService).createApp(createAppCaptor.capture(), eq(OPERATOR));
    assertThat(createAppCaptor.getValue().getApp().getAppId()).isEqualTo("created-app");
    assertThat(createAppCaptor.getValue().getApp().getDataChangeCreatedBy()).isEqualTo(OPERATOR);
    assertThat(createAppCaptor.getValue().getAssignAppRoleToSelf()).isFalse();

    when(appOpenApiService.getAllApps())
        .thenReturn(Collections.singletonList(generatedApp(APP_ID)));
    List<OpenAppDTO> allApps = client.getAllApps();
    assertThat(allApps).extracting(OpenAppDTO::getAppId).containsExactly(APP_ID);
    verify(appOpenApiService).getAllApps();

    when(appOpenApiService.getAppsInfo(anyList()))
        .thenReturn(Collections.singletonList(generatedApp(APP_ID)));
    List<OpenAppDTO> appsByIds = client.getAppsByIds(Arrays.asList(APP_ID));
    assertThat(appsByIds).extracting(OpenAppDTO::getName).containsExactly("Legacy App");
    verify(appOpenApiService).getAppsInfo(Collections.singletonList(APP_ID));

    when(consumerService.findAppIdsAuthorizedByConsumerId(0L))
        .thenReturn(new LinkedHashSet<>(Collections.singletonList(APP_ID)));
    List<OpenAppDTO> authorizedApps = client.getAuthorizedApps();
    assertThat(authorizedApps).extracting(OpenAppDTO::getAppId).containsExactly(APP_ID);

    com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO envCluster =
        new com.ctrip.framework.apollo.openapi.model.OpenEnvClusterDTO();
    envCluster.setEnv(ENV);
    envCluster.setClusters(Collections.singletonList(CLUSTER));
    when(appOpenApiService.getEnvClusters(APP_ID))
        .thenReturn(Collections.singletonList(envCluster));
    List<OpenEnvClusterDTO> envClusters = client.getEnvClusterInfo(APP_ID);
    assertThat(envClusters).hasSize(1);
    assertThat(envClusters.get(0).getEnv()).isEqualTo(ENV);
    assertThat(envClusters.get(0).getClusters()).containsExactly(CLUSTER);
    verify(appOpenApiService).getEnvClusters(APP_ID);

    com.ctrip.framework.apollo.openapi.model.OpenOrganizationDto organization =
        new com.ctrip.framework.apollo.openapi.model.OpenOrganizationDto();
    organization.setOrgId("org-1");
    organization.setOrgName("Organization One");
    when(organizationOpenApiService.getOrganizations())
        .thenReturn(Collections.singletonList(organization));
    List<OpenOrganizationDto> organizations = client.getOrganizations();
    assertThat(organizations).extracting(OpenOrganizationDto::getOrgId).containsExactly("org-1");
    verify(organizationOpenApiService).getOrganizations();
  }

  @Test
  public void legacyClusterAndNamespaceMethodsShouldRemainCompatible() {
    when(clusterOpenApiService.getCluster(APP_ID, ENV, CLUSTER))
        .thenReturn(generatedCluster(APP_ID, CLUSTER));
    OpenClusterDTO loadedCluster = client.getCluster(APP_ID, ENV, null);
    assertThat(loadedCluster.getName()).isEqualTo(CLUSTER);
    verify(clusterOpenApiService).getCluster(APP_ID, ENV, CLUSTER);

    OpenClusterDTO createClusterRequest = new OpenClusterDTO();
    createClusterRequest.setAppId(APP_ID);
    createClusterRequest.setName("legacy-cluster");
    createClusterRequest.setDataChangeCreatedBy(OPERATOR);
    when(clusterOpenApiService.createCluster(eq(ENV),
        any(com.ctrip.framework.apollo.openapi.model.OpenClusterDTO.class), eq(OPERATOR)))
        .thenReturn(generatedCluster(APP_ID, "legacy-cluster"));

    OpenClusterDTO createdCluster = client.createCluster(ENV, createClusterRequest);
    assertThat(createdCluster.getName()).isEqualTo("legacy-cluster");
    ArgumentCaptor<com.ctrip.framework.apollo.openapi.model.OpenClusterDTO> clusterCaptor =
        ArgumentCaptor.forClass(com.ctrip.framework.apollo.openapi.model.OpenClusterDTO.class);
    verify(clusterOpenApiService).createCluster(eq(ENV), clusterCaptor.capture(), eq(OPERATOR));
    assertThat(clusterCaptor.getValue().getDataChangeCreatedBy()).isEqualTo(OPERATOR);

    OpenNamespaceDTO namespace = legacyNamespace();
    when(namespaceOpenApiService.getNamespaces(APP_ID, ENV, CLUSTER, false))
        .thenReturn(Collections.singletonList(namespace));
    List<OpenNamespaceDTO> namespaces = client.getNamespaces(APP_ID, ENV, null, false);
    assertThat(namespaces).extracting(OpenNamespaceDTO::getNamespaceName)
        .containsExactly(NAMESPACE);
    verify(namespaceOpenApiService).getNamespaces(APP_ID, ENV, CLUSTER, false);

    when(namespaceOpenApiService.getNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, true))
        .thenReturn(namespace);
    OpenNamespaceDTO loadedNamespace = client.getNamespace(APP_ID, ENV, null, null, true);
    assertThat(loadedNamespace.getNamespaceName()).isEqualTo(NAMESPACE);
    verify(namespaceOpenApiService).getNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, true);

    OpenNamespaceLockDTO lock = new OpenNamespaceLockDTO();
    lock.setNamespaceName(NAMESPACE);
    lock.setLockedBy(OPERATOR);
    when(namespaceOpenApiService.getNamespaceLock(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(lock);
    OpenNamespaceLockDTO loadedLock = client.getNamespaceLock(APP_ID, ENV, null, null);
    assertThat(loadedLock.getLockedBy()).isEqualTo(OPERATOR);
    verify(namespaceOpenApiService).getNamespaceLock(APP_ID, ENV, CLUSTER, NAMESPACE);

    OpenAppNamespaceDTO appNamespace = new OpenAppNamespaceDTO();
    appNamespace.setAppId(APP_ID);
    appNamespace.setName("legacyNamespace");
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());
    appNamespace.setDataChangeCreatedBy(OPERATOR);
    when(namespaceOpenApiService.createAppNamespace(any(OpenAppNamespaceDTO.class)))
        .thenReturn(appNamespace);
    OpenAppNamespaceDTO createdAppNamespace = client.createAppNamespace(appNamespace);
    assertThat(createdAppNamespace.getName()).isEqualTo("legacyNamespace");
    ArgumentCaptor<OpenAppNamespaceDTO> appNamespaceCaptor =
        ArgumentCaptor.forClass(OpenAppNamespaceDTO.class);
    verify(namespaceOpenApiService).createAppNamespace(appNamespaceCaptor.capture());
    assertThat(appNamespaceCaptor.getValue().getAppId()).isEqualTo(APP_ID);
    assertThat(appNamespaceCaptor.getValue().getName()).isEqualTo("legacyNamespace");
    assertThat(appNamespaceCaptor.getValue().getDataChangeCreatedBy()).isEqualTo(OPERATOR);
  }

  @Test
  public void legacyItemMethodsShouldPreserveOperatorsEncodedKeysAndPaging() {
    when(itemOpenApiService.getItem(APP_ID, ENV, CLUSTER, NAMESPACE, "timeout"))
        .thenReturn(generatedItem("timeout", "100"));
    OpenItemDTO loadedItem = client.getItem(APP_ID, ENV, null, null, "timeout");
    assertThat(loadedItem.getValue()).isEqualTo("100");
    verify(itemOpenApiService).getItem(APP_ID, ENV, CLUSTER, NAMESPACE, "timeout");

    String encodedKey = "feature flag/with space";
    when(itemOpenApiService.getItem(APP_ID, ENV, CLUSTER, NAMESPACE, encodedKey))
        .thenReturn(generatedItem(encodedKey, "enabled"));
    OpenItemDTO loadedEncodedItem = client.getItem(APP_ID, ENV, CLUSTER, NAMESPACE, encodedKey);
    assertThat(loadedEncodedItem.getKey()).isEqualTo(encodedKey);
    verify(itemOpenApiService).getItem(APP_ID, ENV, CLUSTER, NAMESPACE, encodedKey);

    when(itemOpenApiService.getItem(APP_ID, ENV, CLUSTER, NAMESPACE, "missing"))
        .thenThrow(NotFoundException.itemNotFound("missing"));
    assertThat(client.getItem(APP_ID, ENV, CLUSTER, NAMESPACE, "missing")).isNull();

    OpenItemDTO createItem = legacyItem("create-key", "create-value");
    createItem.setDataChangeCreatedBy(OPERATOR);
    when(itemOpenApiService.createItem(eq(APP_ID), eq(ENV), eq(CLUSTER), eq(NAMESPACE),
        any(com.ctrip.framework.apollo.openapi.model.OpenItemDTO.class), eq(OPERATOR)))
        .thenReturn(generatedItem("create-key", "create-value"));
    OpenItemDTO createdItem = client.createItem(APP_ID, ENV, null, null, createItem);
    assertThat(createdItem.getKey()).isEqualTo("create-key");
    ArgumentCaptor<com.ctrip.framework.apollo.openapi.model.OpenItemDTO> createItemCaptor =
        ArgumentCaptor.forClass(com.ctrip.framework.apollo.openapi.model.OpenItemDTO.class);
    verify(itemOpenApiService).createItem(eq(APP_ID), eq(ENV), eq(CLUSTER), eq(NAMESPACE),
        createItemCaptor.capture(), eq(OPERATOR));
    assertThat(createItemCaptor.getValue().getDataChangeCreatedBy()).isEqualTo(OPERATOR);
    assertThat(createItemCaptor.getValue().getDataChangeLastModifiedBy()).isEqualTo(OPERATOR);

    OpenItemDTO updateItem = legacyItem("update-key", "update-value");
    updateItem.setDataChangeLastModifiedBy(OPERATOR);
    client.updateItem(APP_ID, ENV, null, null, updateItem);
    ArgumentCaptor<com.ctrip.framework.apollo.openapi.model.OpenItemDTO> updateItemCaptor =
        ArgumentCaptor.forClass(com.ctrip.framework.apollo.openapi.model.OpenItemDTO.class);
    verify(itemOpenApiService).updateItem(eq(APP_ID), eq(ENV), eq(CLUSTER), eq(NAMESPACE),
        updateItemCaptor.capture(), eq(OPERATOR));
    assertThat(updateItemCaptor.getValue().getKey()).isEqualTo("update-key");

    OpenItemDTO createOrUpdateItem = legacyItem("upsert-key", "upsert-value");
    createOrUpdateItem.setDataChangeCreatedBy(OPERATOR);
    client.createOrUpdateItem(APP_ID, ENV, null, null, createOrUpdateItem);
    ArgumentCaptor<com.ctrip.framework.apollo.openapi.model.OpenItemDTO> upsertItemCaptor =
        ArgumentCaptor.forClass(com.ctrip.framework.apollo.openapi.model.OpenItemDTO.class);
    verify(itemOpenApiService).createOrUpdateItem(eq(APP_ID), eq(ENV), eq(CLUSTER), eq(NAMESPACE),
        upsertItemCaptor.capture(), eq(OPERATOR));
    assertThat(upsertItemCaptor.getValue().getDataChangeCreatedBy()).isEqualTo(OPERATOR);
    assertThat(upsertItemCaptor.getValue().getDataChangeLastModifiedBy()).isEqualTo(OPERATOR);

    client.removeItem(APP_ID, ENV, null, null, "delete-key", OPERATOR);
    verify(itemOpenApiService).removeItem(APP_ID, ENV, CLUSTER, NAMESPACE, "delete-key",
        OPERATOR);

    com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO page =
        new com.ctrip.framework.apollo.openapi.model.OpenItemPageDTO();
    page.setPage(1);
    page.setSize(20);
    page.setTotal(1L);
    page.setContent(Collections.singletonList(generatedItem("page-key", "page-value")));
    when(itemOpenApiService.findItemsByNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, 1, 20))
        .thenReturn(page);
    OpenPageDTO<OpenItemDTO> items = client.findItemsByNamespace(APP_ID, ENV, null, null, 1, 20);
    assertThat(items.getPage()).isEqualTo(1);
    assertThat(items.getSize()).isEqualTo(20);
    assertThat(items.getTotal()).isEqualTo(1);
    assertThat(items.getContent()).extracting(OpenItemDTO::getKey).containsExactly("page-key");
    verify(itemOpenApiService).findItemsByNamespace(APP_ID, ENV, CLUSTER, NAMESPACE, 1, 20);
  }

  @Test
  public void legacyReleaseAndInstanceMethodsShouldRemainCompatible() {
    NamespaceReleaseDTO releaseRequest = new NamespaceReleaseDTO();
    releaseRequest.setReleaseTitle("legacy-release");
    releaseRequest.setReleaseComment("release from old client");
    releaseRequest.setReleasedBy(OPERATOR);
    releaseRequest.setEmergencyPublish(true);

    when(releaseOpenApiService.publishNamespace(eq(APP_ID), eq(ENV), eq(CLUSTER), eq(NAMESPACE),
        any(NamespaceReleaseDTO.class))).thenReturn(legacyRelease(10L));
    OpenReleaseDTO publishedRelease =
        client.publishNamespace(APP_ID, ENV, null, null, releaseRequest);
    assertThat(publishedRelease.getId()).isEqualTo(10L);
    ArgumentCaptor<NamespaceReleaseDTO> releaseCaptor =
        ArgumentCaptor.forClass(NamespaceReleaseDTO.class);
    verify(releaseOpenApiService).publishNamespace(eq(APP_ID), eq(ENV), eq(CLUSTER), eq(NAMESPACE),
        releaseCaptor.capture());
    assertThat(releaseCaptor.getValue().getReleasedBy()).isEqualTo(OPERATOR);
    assertThat(releaseCaptor.getValue().isEmergencyPublish()).isTrue();

    when(releaseOpenApiService.getLatestActiveRelease(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(legacyRelease(11L));
    OpenReleaseDTO latestRelease = client.getLatestActiveRelease(APP_ID, ENV, null, null);
    assertThat(latestRelease.getId()).isEqualTo(11L);
    verify(releaseOpenApiService).getLatestActiveRelease(APP_ID, ENV, CLUSTER, NAMESPACE);

    ReleaseDTO release = new ReleaseDTO();
    release.setId(12L);
    release.setAppId(APP_ID);
    release.setClusterName(CLUSTER);
    release.setNamespaceName(NAMESPACE);
    when(releaseService.findReleaseById(Env.valueOf(ENV), 12L)).thenReturn(release);
    client.rollbackRelease(ENV, 12L, OPERATOR);
    verify(releaseOpenApiService).rollbackRelease(ENV, 12L, OPERATOR);

    when(instanceOpenApiService.getInstanceCountByNamespace(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(3);
    int instanceCount = client.getInstanceCountByNamespace(APP_ID, ENV, null, null);
    assertThat(instanceCount).isEqualTo(3);
    verify(instanceOpenApiService).getInstanceCountByNamespace(APP_ID, ENV, CLUSTER, NAMESPACE);
  }

  private OpenAppDTO legacyApp(String appId) {
    OpenAppDTO app = new OpenAppDTO();
    app.setAppId(appId);
    app.setName("Legacy App");
    app.setOrgId("org-1");
    app.setOrgName("Organization One");
    app.setOwnerName("legacy-owner");
    app.setOwnerEmail("legacy-owner@example.com");
    return app;
  }

  private OpenNamespaceDTO legacyNamespace() {
    OpenNamespaceDTO namespace = new OpenNamespaceDTO();
    namespace.setAppId(APP_ID);
    namespace.setClusterName(CLUSTER);
    namespace.setNamespaceName(NAMESPACE);
    namespace.setFormat(ConfigFileFormat.Properties.getValue());
    namespace.setPublic(false);
    namespace.setItems(Collections.singletonList(legacyItem("timeout", "100")));
    return namespace;
  }

  private OpenItemDTO legacyItem(String key, String value) {
    OpenItemDTO item = new OpenItemDTO();
    item.setKey(key);
    item.setValue(value);
    item.setComment("legacy comment");
    item.setType(0);
    return item;
  }

  private OpenReleaseDTO legacyRelease(long id) {
    OpenReleaseDTO release = new OpenReleaseDTO();
    release.setId(id);
    release.setAppId(APP_ID);
    release.setClusterName(CLUSTER);
    release.setNamespaceName(NAMESPACE);
    release.setName("legacy-release");
    release.setComment("legacy release comment");
    release.setConfigurations(Map.of("timeout", "100"));
    return release;
  }

  private com.ctrip.framework.apollo.openapi.model.OpenAppDTO generatedApp(String appId) {
    com.ctrip.framework.apollo.openapi.model.OpenAppDTO app =
        new com.ctrip.framework.apollo.openapi.model.OpenAppDTO();
    app.setAppId(appId);
    app.setName("Legacy App");
    app.setOrgId("org-1");
    app.setOrgName("Organization One");
    app.setOwnerName("legacy-owner");
    app.setOwnerEmail("legacy-owner@example.com");
    return app;
  }

  private com.ctrip.framework.apollo.openapi.model.OpenClusterDTO generatedCluster(String appId,
      String clusterName) {
    com.ctrip.framework.apollo.openapi.model.OpenClusterDTO cluster =
        new com.ctrip.framework.apollo.openapi.model.OpenClusterDTO();
    cluster.setAppId(appId);
    cluster.setName(clusterName);
    return cluster;
  }

  private com.ctrip.framework.apollo.openapi.model.OpenItemDTO generatedItem(String key,
      String value) {
    com.ctrip.framework.apollo.openapi.model.OpenItemDTO item =
        new com.ctrip.framework.apollo.openapi.model.OpenItemDTO();
    item.setKey(key);
    item.setValue(value);
    item.setType(0);
    item.setComment("legacy comment");
    return item;
  }
}
