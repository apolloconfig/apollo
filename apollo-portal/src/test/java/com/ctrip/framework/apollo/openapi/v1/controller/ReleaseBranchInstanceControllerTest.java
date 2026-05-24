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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstanceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenInstancePageDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDiffDTO;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.KVEntity;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.enums.ChangeType;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

/**
 * Tests the Portal OpenAPI release, branch, and instance controllers against generated contracts
 * and legacy token-client compatibility requirements.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReleaseBranchInstanceControllerTest {

  private static final String APP_ID = "sample-app";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String BRANCH = "gray-release";
  private static final String PORTAL_USER = "portal-user";
  private static final String CONSUMER_OPERATOR = "legacy-operator";

  @Mock
  private ReleaseService releaseService;

  @Mock
  private UserService userService;

  @Mock
  private NamespaceBranchService namespaceBranchService;

  @Mock
  private ApplicationEventPublisher publisher;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private UserInfoHolder userInfoHolder;

  @Mock
  private InstanceService instanceService;

  private ReleaseController releaseController;
  private NamespaceBranchController namespaceBranchController;
  private InstanceController instanceController;

  @BeforeEach
  void setUp() {
    releaseController = new ReleaseController(releaseService, userService, namespaceBranchService,
        unifiedPermissionValidator, portalConfig, userInfoHolder, publisher);
    namespaceBranchController =
        new NamespaceBranchController(unifiedPermissionValidator, releaseService,
            namespaceBranchService, userService, userInfoHolder, portalConfig, publisher);
    instanceController = new InstanceController(instanceService);

    UserInfo userInfo = userInfo(PORTAL_USER);
    when(userInfoHolder.getUser()).thenReturn(userInfo);
    when(userService.findByUserId(anyString())).thenReturn(userInfo(CONSUMER_OPERATOR));
    when(portalConfig.isEmergencyPublishAllowed(Env.DEV)).thenReturn(true);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission(anyString(), anyString(),
        anyString(), anyString())).thenReturn(true);
  }

  @AfterEach
  void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  void createReleaseShouldUseCurrentPortalUserAndIgnorePayloadReleasedBy() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.publish(any(NamespaceReleaseModel.class))).thenReturn(release(100L));

    NamespaceReleaseDTO request = releaseRequest("release title", "spoofed-user", false);

    ResponseEntity<com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO> response =
        releaseController.createRelease(APP_ID, ENV, CLUSTER, NAMESPACE, request, null);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(100L);

    ArgumentCaptor<NamespaceReleaseModel> modelCaptor =
        ArgumentCaptor.forClass(NamespaceReleaseModel.class);
    verify(releaseService).publish(modelCaptor.capture());
    assertThat(modelCaptor.getValue().getReleasedBy()).isEqualTo(PORTAL_USER);
    assertThat(modelCaptor.getValue().getAppId()).isEqualTo(APP_ID);
    assertThat(modelCaptor.getValue().getClusterName()).isEqualTo(CLUSTER);
    assertThat(modelCaptor.getValue().getNamespaceName()).isEqualTo(NAMESPACE);
    verify(userService, never()).findByUserId("spoofed-user");
  }

  @Test
  void createReleaseShouldKeepConsumerPayloadReleasedByForLegacyClients() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(releaseService.publish(any(NamespaceReleaseModel.class))).thenReturn(release(101L));

    NamespaceReleaseDTO request = releaseRequest("release title", CONSUMER_OPERATOR, false);

    releaseController.createRelease(APP_ID, ENV, CLUSTER, NAMESPACE, request, null);

    ArgumentCaptor<NamespaceReleaseModel> modelCaptor =
        ArgumentCaptor.forClass(NamespaceReleaseModel.class);
    verify(releaseService).publish(modelCaptor.capture());
    verify(userService).findByUserId(CONSUMER_OPERATOR);
    assertThat(modelCaptor.getValue().getReleasedBy()).isEqualTo(CONSUMER_OPERATOR);
  }

  @Test
  void createReleaseShouldRejectConsumerWithoutPayloadOrQueryOperator() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);

    NamespaceReleaseDTO request = releaseRequest("release title", null, false);

    assertThatThrownBy(
        () -> releaseController.createRelease(APP_ID, ENV, CLUSTER, NAMESPACE, request, null))
        .isInstanceOf(BadRequestException.class);

    verify(releaseService, never()).publish(any(NamespaceReleaseModel.class));
  }

  @Test
  void rollbackShouldUseCurrentPortalUserAndToReleaseId() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));

    releaseController.rollback(ENV, 11L, null, 7L);

    verify(releaseService).rollbackTo(Env.DEV, 11L, 7L, PORTAL_USER);
    verify(releaseService, never()).rollback(eq(Env.DEV), eq(11L), anyString());
  }

  @Test
  void rollbackShouldRequireConsumerOperator() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));

    assertThatThrownBy(() -> releaseController.rollback(ENV, 11L, null, null))
        .isInstanceOf(BadRequestException.class);

    verify(releaseService, never()).rollback(eq(Env.DEV), eq(11L), anyString());
  }

  @Test
  void rollbackShouldRejectPermissionDeniedRelease() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));
    when(unifiedPermissionValidator.hasReleaseNamespacePermission(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);

    assertThatThrownBy(() -> releaseController.rollback(ENV, 11L, null, null))
        .isInstanceOf(AccessDeniedException.class);

    verify(releaseService, never()).rollback(eq(Env.DEV), eq(11L), anyString());
  }

  @Test
  void createReleaseShouldRejectAnonymousWriteRequest() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.ANONYMOUS);

    NamespaceReleaseDTO request = releaseRequest("release title", PORTAL_USER, false);

    assertThatThrownBy(
        () -> releaseController.createRelease(APP_ID, ENV, CLUSTER, NAMESPACE, request, null))
        .isInstanceOf(BadRequestException.class);

    verify(releaseService, never()).publish(any(NamespaceReleaseModel.class));
  }

  @Test
  void createGrayReleaseShouldUseBranchClusterAndCurrentPortalUser() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.publish(any(NamespaceReleaseModel.class))).thenReturn(release(102L));

    NamespaceReleaseDTO request = releaseRequest("gray release", "spoofed-user", false);

    releaseController.createGrayRelease(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH, request, null);

    ArgumentCaptor<NamespaceReleaseModel> modelCaptor =
        ArgumentCaptor.forClass(NamespaceReleaseModel.class);
    verify(releaseService).publish(modelCaptor.capture());
    assertThat(modelCaptor.getValue().getClusterName()).isEqualTo(BRANCH);
    assertThat(modelCaptor.getValue().getReleasedBy()).isEqualTo(PORTAL_USER);
  }

  @Test
  void compareReleaseShouldReturnGeneratedDiffShape() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.findReleaseById(Env.DEV, 1L)).thenReturn(release(1L));
    when(releaseService.findReleaseById(Env.DEV, 2L)).thenReturn(release(2L));
    ReleaseCompareResult compareResult = new ReleaseCompareResult();
    compareResult.addEntityPair(ChangeType.MODIFIED, new KVEntity("timeout", "100"),
        new KVEntity("timeout", "200"));
    when(releaseService.compare(Env.DEV, 1L, 2L)).thenReturn(compareResult);

    ResponseEntity<OpenReleaseDiffDTO> response = releaseController.compareRelease(ENV, 1L, 2L);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getChanges()).hasSize(1);
    assertThat(response.getBody().getChanges().get(0).getKey()).isEqualTo("timeout");
    assertThat(response.getBody().getChanges().get(0).getOldValue()).isEqualTo("100");
    assertThat(response.getBody().getChanges().get(0).getNewValue()).isEqualTo("200");
  }

  @Test
  void compareReleaseShouldRejectHiddenPortalRelease() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.findReleaseById(Env.DEV, 1L)).thenReturn(release(1L));
    when(releaseService.findReleaseById(Env.DEV, 2L)).thenReturn(release(2L));
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(true);

    assertThatThrownBy(() -> releaseController.compareRelease(ENV, 1L, 2L))
        .isInstanceOf(AccessDeniedException.class);

    verify(releaseService, never()).compare(Env.DEV, 1L, 2L);
  }

  @Test
  void compareReleaseShouldRejectConsumerWithoutReleasePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(releaseService.findReleaseById(Env.DEV, 1L)).thenReturn(release(1L));
    when(releaseService.findReleaseById(Env.DEV, 2L)).thenReturn(release(2L));
    when(unifiedPermissionValidator.hasReleaseNamespacePermission(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);

    assertThatThrownBy(() -> releaseController.compareRelease(ENV, 1L, 2L))
        .isInstanceOf(AccessDeniedException.class);

    verify(releaseService, never()).compare(Env.DEV, 1L, 2L);
  }

  @Test
  void getReleaseByIdShouldAcceptUnsignedDatabaseReleaseIdRange() {
    long releaseId = 3_000_000_000L;
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.findReleaseById(Env.DEV, releaseId)).thenReturn(release(releaseId));

    ResponseEntity<com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO> response =
        releaseController.getReleaseById(ENV, releaseId);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getId()).isEqualTo(releaseId);
  }

  @Test
  void findActiveReleasesShouldRejectConsumerWithoutReleasePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);

    assertThatThrownBy(
        () -> releaseController.findActiveReleases(APP_ID, ENV, CLUSTER, NAMESPACE, 0, 10))
        .isInstanceOf(AccessDeniedException.class);

    verify(releaseService, never()).findActiveReleases(eq(APP_ID), eq(Env.DEV), eq(CLUSTER),
        eq(NAMESPACE), eq(0), eq(10));
  }

  @Test
  void loadLatestActiveReleaseShouldReturnEmptyBodyWhenNoActiveReleaseExists() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.loadLatestRelease(APP_ID, Env.DEV, CLUSTER, NAMESPACE)).thenReturn(null);

    ResponseEntity<com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO> response =
        releaseController.loadLatestActiveRelease(APP_ID, ENV, CLUSTER, NAMESPACE);

    assertThat(response.getBody()).isNull();
  }

  @Test
  void loadLatestActiveReleaseShouldRejectConsumerWithoutReleasePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);

    assertThatThrownBy(
        () -> releaseController.loadLatestActiveRelease(APP_ID, ENV, CLUSTER, NAMESPACE))
        .isInstanceOf(AccessDeniedException.class);

    verify(releaseService, never()).loadLatestRelease(eq(APP_ID), eq(Env.DEV), eq(CLUSTER),
        eq(NAMESPACE));
  }

  @Test
  void createBranchShouldUseCurrentPortalUser() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(namespaceBranchService.createBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, PORTAL_USER))
        .thenReturn(namespace(BRANCH));

    ResponseEntity<OpenNamespaceDTO> response =
        namespaceBranchController.createBranch(APP_ID, ENV, CLUSTER, NAMESPACE, null);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getClusterName()).isEqualTo(BRANCH);
    verify(namespaceBranchService).createBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, PORTAL_USER);
  }

  @Test
  void createBranchShouldAcceptLowercaseEnv() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(namespaceBranchService.createBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, PORTAL_USER))
        .thenReturn(namespace(BRANCH));

    ResponseEntity<OpenNamespaceDTO> response =
        namespaceBranchController.createBranch(APP_ID, "dev", CLUSTER, NAMESPACE, null);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getClusterName()).isEqualTo(BRANCH);
    verify(namespaceBranchService).createBranch(APP_ID, Env.DEV, CLUSTER, NAMESPACE, PORTAL_USER);
  }

  @Test
  void mergeBranchShouldUseCurrentPortalUserAndDeleteFlag() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(namespaceBranchService.merge(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH, "merge title",
        "release comment", false, false, PORTAL_USER)).thenReturn(release(103L));

    NamespaceReleaseDTO request = releaseRequest("merge title", "spoofed-user", false);

    com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO response = namespaceBranchController
        .merge(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH, false, request).getBody();

    assertThat(response).isNotNull();
    assertThat(response.getId()).isEqualTo(103L);
    verify(namespaceBranchService).merge(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH, "merge title",
        "release comment", false, false, PORTAL_USER);
  }

  @Test
  void updateBranchRulesShouldUseCurrentPortalUserAndPathFields() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);

    OpenGrayReleaseRuleDTO request = new OpenGrayReleaseRuleDTO();
    OpenGrayReleaseRuleItemDTO ruleItem = new OpenGrayReleaseRuleItemDTO();
    ruleItem.setClientAppId("client-app");
    ruleItem.setClientIpList(Collections.singleton("10.0.0.1"));
    request.setRuleItems(Collections.singleton(ruleItem));

    namespaceBranchController.updateBranchRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH, request,
        "spoofed-user");

    ArgumentCaptor<GrayReleaseRuleDTO> rulesCaptor =
        ArgumentCaptor.forClass(GrayReleaseRuleDTO.class);
    verify(namespaceBranchService).updateBranchGrayRules(eq(APP_ID), eq(Env.DEV), eq(CLUSTER),
        eq(NAMESPACE), eq(BRANCH), rulesCaptor.capture(), eq(PORTAL_USER));
    assertThat(rulesCaptor.getValue().getAppId()).isEqualTo(APP_ID);
    assertThat(rulesCaptor.getValue().getClusterName()).isEqualTo(CLUSTER);
    assertThat(rulesCaptor.getValue().getNamespaceName()).isEqualTo(NAMESPACE);
    assertThat(rulesCaptor.getValue().getBranchName()).isEqualTo(BRANCH);
    assertThat(rulesCaptor.getValue().getRuleItems()).hasSize(1);
  }

  @Test
  void canUpdateBranchRulesShouldAllowPortalUserWithOperatePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.hasModifyNamespacePermission(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(true);
    when(unifiedPermissionValidator.hasOperateNamespacePermission(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenCallRealMethod();

    assertThat(namespaceBranchController.canUpdateBranchRules(APP_ID, ENV, CLUSTER, NAMESPACE))
        .isTrue();
  }

  @Test
  void getByNamespaceShouldReturnGeneratedInstancePage() {
    when(instanceService.getByNamespace(Env.DEV, APP_ID, CLUSTER, NAMESPACE, "client-app", 0, 20))
        .thenReturn(instancePage());

    ResponseEntity<OpenInstancePageDTO> response =
        instanceController.getByNamespace(ENV, APP_ID, CLUSTER, NAMESPACE, 0, 20, "client-app");

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getInstances()).hasSize(1);
    assertThat(response.getBody().getInstances().get(0).getAppId()).isEqualTo("client-app");
  }

  @Test
  void getByReleasesNotInShouldReturnGeneratedInstances() {
    InstanceDTO instance = instance();
    when(instanceService.getByReleasesNotIn(eq(Env.DEV), eq(APP_ID), eq(CLUSTER), eq(NAMESPACE),
        any())).thenReturn(Collections.singletonList(instance));

    ResponseEntity<java.util.List<OpenInstanceDTO>> response =
        instanceController.getByReleasesAndNamespaceNotIn(ENV, APP_ID, CLUSTER, NAMESPACE, "1,2");

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody()).hasSize(1);
    assertThat(response.getBody().get(0).getIp()).isEqualTo("10.0.0.1");
  }

  @Test
  void getByReleasesNotInShouldRejectInvalidReleaseIds() {
    assertThatThrownBy(() -> instanceController.getByReleasesAndNamespaceNotIn(ENV, APP_ID, CLUSTER,
        NAMESPACE, "1,not-a-number")).isInstanceOf(BadRequestException.class);

    verify(instanceService, never()).getByReleasesNotIn(eq(Env.DEV), eq(APP_ID), eq(CLUSTER),
        eq(NAMESPACE), any());
  }

  private NamespaceReleaseDTO releaseRequest(String title, String releasedBy,
      boolean emergencyPublish) {
    NamespaceReleaseDTO request = new NamespaceReleaseDTO();
    request.setReleaseTitle(title);
    request.setReleaseComment("release comment");
    request.setReleasedBy(releasedBy);
    request.setIsEmergencyPublish(emergencyPublish);
    return request;
  }

  private ReleaseDTO release(long id) {
    ReleaseDTO release = new ReleaseDTO();
    release.setId(id);
    release.setAppId(APP_ID);
    release.setClusterName(CLUSTER);
    release.setNamespaceName(NAMESPACE);
    release.setName("release-" + id);
    release.setConfigurations("{\"timeout\":\"200\"}");
    return release;
  }

  private NamespaceDTO namespace(String branchName) {
    NamespaceDTO namespace = new NamespaceDTO();
    namespace.setAppId(APP_ID);
    namespace.setClusterName(branchName);
    namespace.setNamespaceName(NAMESPACE);
    return namespace;
  }

  private PageDTO<InstanceDTO> instancePage() {
    return new PageDTO<>(Collections.singletonList(instance()), PageRequest.of(0, 20), 1L);
  }

  private InstanceDTO instance() {
    InstanceDTO instance = new InstanceDTO();
    instance.setId(1L);
    instance.setAppId("client-app");
    instance.setClusterName(CLUSTER);
    instance.setIp("10.0.0.1");
    return instance;
  }

  private UserInfo userInfo(String userId) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    return userInfo;
  }
}
