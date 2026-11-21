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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.vo.Number;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.InstanceService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class InstanceControllerTest {

  @Mock
  private InstanceService instanceService;

  @Mock
  private ReleaseService releaseService;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @Mock
  private UserPermissionValidator userPermissionValidator;

  @Mock
  private UserInfoHolder userInfoHolder;

  @InjectMocks
  private InstanceController instanceController;

  @Captor
  private ArgumentCaptor<Set<Long>> releaseIdsCaptor;

  private MockMvc mockMvc;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(instanceController).build();
  }

  @After
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void shouldUseDefaultPageAndSizeForGetByRelease() throws Exception {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    PageDTO<InstanceDTO> page = new PageDTO<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));
    when(instanceService.getByRelease(Env.DEV, 11L, 0, 20)).thenReturn(page);

    mockMvc.perform(MockMvcRequestBuilders.get("/envs/{env}/instances/by-release", "DEV")
        .param("releaseId", "11")).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    verify(instanceService).getByRelease(Env.DEV, 11L, 0, 20);
  }

  @Test
  public void shouldParseReleaseIdsAndDeduplicateWhenQueryingInstances() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    List<InstanceDTO> expected = Arrays.asList(new InstanceDTO(), new InstanceDTO());
    when(instanceService.getByReleasesNotIn(eq(Env.DEV), eq("SampleApp"), eq("default"),
        eq("application"), releaseIdsCaptor.capture())).thenReturn(expected);

    List<InstanceDTO> result = instanceController.getByReleasesNotIn("DEV", "SampleApp", "default",
        "application", "1,2,2,3");

    assertSame(expected, result);
    Set<Long> releaseIds = releaseIdsCaptor.getValue();
    assertEquals(3, releaseIds.size());
    org.junit.Assert.assertTrue(releaseIds.contains(1L));
    org.junit.Assert.assertTrue(releaseIds.contains(2L));
    org.junit.Assert.assertTrue(releaseIds.contains(3L));
  }

  @Test(expected = BadRequestException.class)
  public void shouldRejectEmptyReleaseIds() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    instanceController.getByReleasesNotIn("DEV", "SampleApp", "default", "application", "");
  }

  @Test
  public void shouldReturnInstanceCountByNamespace() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(
        instanceService.getInstanceCountByNamespace("SampleApp", Env.DEV, "default", "application"))
        .thenReturn(8);

    ResponseEntity<Number> response = instanceController.getInstanceCountByNamespace("DEV",
        "SampleApp", "default", "application");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(8, response.getBody().getNum());
  }

  @Test
  public void getByNamespaceShouldReturnEmptyPageWhenPortalUserShouldHideConfig() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    PageDTO<InstanceDTO> result = instanceController.getByNamespace("DEV", "SampleApp", "default",
        "application", null, 1, 10);

    assertTrue(result.getContent().isEmpty());
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(0, result.getTotal());
    verifyNoInteractions(instanceService);
  }

  @Test
  public void getByNamespaceShouldAllowVisiblePortalUser() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    PageDTO<InstanceDTO> page =
        new PageDTO<>(Collections.singletonList(new InstanceDTO()), PageRequest.of(0, 20), 1);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(false);
    when(
        instanceService.getByNamespace(Env.DEV, "SampleApp", "default", "application", null, 0, 20))
        .thenReturn(page);

    PageDTO<InstanceDTO> result = instanceController.getByNamespace("DEV", "SampleApp", "default",
        "application", null, 0, 20);

    assertSame(page, result);
  }

  @Test(expected = AccessDeniedException.class)
  public void getByNamespaceShouldRejectUserTokenWithoutConfigRead() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    instanceController.getByNamespace("DEV", "SampleApp", "default", "application", null, 0, 20);
  }

  @Test(expected = AccessDeniedException.class)
  public void getByNamespaceShouldRejectConsumerWithoutNamespacePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission("SampleApp", "DEV", "default",
        "application")).thenReturn(false);

    instanceController.getByNamespace("DEV", "SampleApp", "default", "application", null, 0, 20);
  }

  @Test
  public void getByNamespaceShouldAllowConsumerWithNamespacePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    PageDTO<InstanceDTO> page =
        new PageDTO<>(Collections.singletonList(new InstanceDTO()), PageRequest.of(0, 20), 1);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission("SampleApp", "DEV", "default",
        "application")).thenReturn(true);
    when(
        instanceService.getByNamespace(Env.DEV, "SampleApp", "default", "application", null, 0, 20))
        .thenReturn(page);

    PageDTO<InstanceDTO> result = instanceController.getByNamespace("DEV", "SampleApp", "default",
        "application", null, 0, 20);

    assertSame(page, result);
  }

  @Test(expected = AccessDeniedException.class)
  public void getByReleaseShouldRejectHiddenPortalUser() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    instanceController.getByRelease("DEV", 11L, 0, 20);
  }

  @Test(expected = AccessDeniedException.class)
  public void getByReleaseShouldRejectHiddenUserToken() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    instanceController.getByRelease("DEV", 11L, 0, 20);
  }

  @Test
  public void getByReleaseShouldEvaluateVisibilityOnlyOnceForAllowedUserToken() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    PageDTO<InstanceDTO> page =
        new PageDTO<>(Collections.singletonList(new InstanceDTO()), PageRequest.of(0, 20), 1);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(false);
    when(instanceService.getByRelease(Env.DEV, 11L, 0, 20)).thenReturn(page);

    PageDTO<InstanceDTO> result = instanceController.getByRelease("DEV", 11L, 0, 20);

    assertSame(page, result);
    // The release is resolved once and its owning namespace is evaluated once: the previous
    // two-step check ran the same visibility check twice for an allowed user token.
    verify(unifiedPermissionValidator, times(1)).shouldHideConfigToCurrentUser("SampleApp", "DEV",
        "default", "application");
    verify(releaseService, times(1)).findReleaseById(Env.DEV, 11L);
    verify(instanceService, times(1)).getByRelease(Env.DEV, 11L, 0, 20);
  }

  @Test(expected = AccessDeniedException.class)
  public void getByReleaseShouldRejectConsumerWithoutReleasePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));
    when(unifiedPermissionValidator.hasReleaseNamespacePermission("SampleApp", "DEV", "default",
        "application")).thenReturn(false);

    instanceController.getByRelease("DEV", 11L, 0, 20);
  }

  @Test(expected = NotFoundException.class)
  public void getByReleaseShouldThrowNotFoundForMissingRelease() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(null);

    instanceController.getByRelease("DEV", 11L, 0, 20);
  }

  @Test
  public void getInstanceCountShouldReturnZeroWhenPortalUserShouldHideConfig() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    ResponseEntity<Number> response = instanceController.getInstanceCountByNamespace("DEV",
        "SampleApp", "default", "application");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(0, response.getBody().getNum());
    verifyNoInteractions(instanceService);
  }

  @Test(expected = AccessDeniedException.class)
  public void getInstanceCountShouldRejectUserTokenWithoutConfigRead() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    instanceController.getInstanceCountByNamespace("DEV", "SampleApp", "default", "application");
  }

  @Test(expected = AccessDeniedException.class)
  public void getInstanceCountShouldRejectConsumerWithoutNamespacePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission("SampleApp", "DEV", "default",
        "application")).thenReturn(false);

    instanceController.getInstanceCountByNamespace("DEV", "SampleApp", "default", "application");
  }

  @Test
  public void getByReleasesNotInShouldReturnEmptyListWhenPortalUserShouldHideConfig() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    List<InstanceDTO> result =
        instanceController.getByReleasesNotIn("DEV", "SampleApp", "default", "application", "1,2");

    assertTrue(result.isEmpty());
    verifyNoInteractions(instanceService);
  }

  @Test(expected = AccessDeniedException.class)
  public void getByReleasesNotInShouldRejectUserTokenWithoutConfigRead() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    instanceController.getByReleasesNotIn("DEV", "SampleApp", "default", "application", "1,2");
  }

  @Test(expected = AccessDeniedException.class)
  public void getByReleasesNotInShouldRejectConsumerWithoutNamespacePermission() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    when(unifiedPermissionValidator.hasReleaseNamespacePermission("SampleApp", "DEV", "default",
        "application")).thenReturn(false);

    instanceController.getByReleasesNotIn("DEV", "SampleApp", "default", "application", "1,2");
  }

  @Test
  public void getByNamespaceShouldReturnEmptyPageWhenAnonymousTrustedUserShouldHideConfig() {
    setAnonymousTrustedUser();
    when(userPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    PageDTO<InstanceDTO> result = instanceController.getByNamespace("DEV", "SampleApp", "default",
        "application", null, 1, 10);

    assertTrue(result.getContent().isEmpty());
    assertEquals(1, result.getPage());
    assertEquals(10, result.getSize());
    assertEquals(0, result.getTotal());
    verifyNoInteractions(instanceService);
  }

  @Test
  public void getByNamespaceShouldAllowVisibleAnonymousTrustedUser() {
    setAnonymousTrustedUser();
    PageDTO<InstanceDTO> page =
        new PageDTO<>(Collections.singletonList(new InstanceDTO()), PageRequest.of(0, 20), 1);
    when(userPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(false);
    when(
        instanceService.getByNamespace(Env.DEV, "SampleApp", "default", "application", null, 0, 20))
        .thenReturn(page);

    PageDTO<InstanceDTO> result = instanceController.getByNamespace("DEV", "SampleApp", "default",
        "application", null, 0, 20);

    assertSame(page, result);
  }

  @Test
  public void getByNamespaceShouldEvaluateVisibilityOnlyOnceForVisibleAnonymousTrustedUser() {
    setAnonymousTrustedUser();
    PageDTO<InstanceDTO> page =
        new PageDTO<>(Collections.singletonList(new InstanceDTO()), PageRequest.of(0, 20), 1);
    when(userPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(false);
    when(
        instanceService.getByNamespace(Env.DEV, "SampleApp", "default", "application", null, 0, 20))
        .thenReturn(page);

    PageDTO<InstanceDTO> result = instanceController.getByNamespace("DEV", "SampleApp", "default",
        "application", null, 0, 20);

    assertSame(page, result);
    // A trusted SSO user who can see the namespace was evaluated twice before: once to decide
    // whether to hide the config, and again to decide whether the read is allowed. The single
    // decision evaluates the user and the visibility policy exactly once.
    verify(userPermissionValidator, times(1)).shouldHideConfigToCurrentUser("SampleApp", "DEV",
        "default", "application");
    verify(userInfoHolder, times(1)).getUser();
    verify(instanceService, times(1)).getByNamespace(Env.DEV, "SampleApp", "default", "application",
        null, 0, 20);
  }

  @Test
  public void getInstanceCountShouldReturnZeroWhenAnonymousTrustedUserShouldHideConfig() {
    setAnonymousTrustedUser();
    when(userPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    ResponseEntity<Number> response = instanceController.getInstanceCountByNamespace("DEV",
        "SampleApp", "default", "application");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(0, response.getBody().getNum());
    verifyNoInteractions(instanceService);
  }

  @Test
  public void getByReleasesNotInShouldReturnEmptyListWhenAnonymousTrustedUserShouldHideConfig() {
    setAnonymousTrustedUser();
    when(userPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    List<InstanceDTO> result =
        instanceController.getByReleasesNotIn("DEV", "SampleApp", "default", "application", "1,2");

    assertTrue(result.isEmpty());
    verifyNoInteractions(instanceService);
  }

  @Test
  public void getByReleaseShouldRejectHiddenAnonymousTrustedUser() {
    setAnonymousTrustedUser();
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));
    when(userPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    assertThrows(AccessDeniedException.class,
        () -> instanceController.getByRelease("DEV", 11L, 0, 20));

    verify(releaseService).findReleaseById(Env.DEV, 11L);
    verifyNoInteractions(instanceService);
  }

  @Test
  public void getByNamespaceShouldRejectAnonymousWithoutUser() {
    setAnonymousWithoutUser();

    assertThrows(AccessDeniedException.class, () -> instanceController.getByNamespace("DEV",
        "SampleApp", "default", "application", null, 0, 20));

    verifyNoInteractions(instanceService);
    verifyNoInteractions(userPermissionValidator);
  }

  @Test
  public void getByNamespaceShouldRejectAnonymousWithAnonymousUserId() {
    setAnonymousUser("anonymousUser");

    assertThrows(AccessDeniedException.class, () -> instanceController.getByNamespace("DEV",
        "SampleApp", "default", "application", null, 0, 20));

    verifyNoInteractions(instanceService);
    verifyNoInteractions(userPermissionValidator);
  }

  @Test
  public void getByNamespaceShouldRejectAnonymousWithBlankUserId() {
    setAnonymousUser(null);

    assertThrows(AccessDeniedException.class, () -> instanceController.getByNamespace("DEV",
        "SampleApp", "default", "application", null, 0, 20));

    verifyNoInteractions(instanceService);
    verifyNoInteractions(userPermissionValidator);
  }

  @Test
  public void getByNamespaceShouldResolveAnonymousIdentityOnlyOnceWhenUntrusted() {
    setAnonymousWithoutUser();

    assertThrows(AccessDeniedException.class, () -> instanceController.getByNamespace("DEV",
        "SampleApp", "default", "application", null, 0, 20));

    // The identity is resolved once for the single decision: the previous two-step check
    // asked the holder for the user once per step.
    verify(userInfoHolder, times(1)).getUser();
    verifyNoInteractions(instanceService);
    verifyNoInteractions(userPermissionValidator);
  }

  @Test
  public void getByReleaseShouldRejectAnonymousWithoutUser() {
    setAnonymousWithoutUser();
    when(releaseService.findReleaseById(Env.DEV, 11L)).thenReturn(release(11L));

    assertThrows(AccessDeniedException.class,
        () -> instanceController.getByRelease("DEV", 11L, 0, 20));

    verify(releaseService).findReleaseById(Env.DEV, 11L);
    verifyNoInteractions(instanceService);
    verifyNoInteractions(userPermissionValidator);
  }

  @Test
  public void getInstanceCountShouldRejectAnonymousWithoutUser() {
    setAnonymousWithoutUser();

    assertThrows(AccessDeniedException.class, () -> instanceController
        .getInstanceCountByNamespace("DEV", "SampleApp", "default", "application"));

    verifyNoInteractions(instanceService);
    verifyNoInteractions(userPermissionValidator);
  }

  @Test
  public void getByReleasesNotInShouldRejectAnonymousWithoutUser() {
    setAnonymousWithoutUser();

    assertThrows(AccessDeniedException.class, () -> instanceController.getByReleasesNotIn("DEV",
        "SampleApp", "default", "application", "1,2"));

    verifyNoInteractions(instanceService);
    verifyNoInteractions(userPermissionValidator);
  }

  @Test
  public void getByNamespaceShouldRejectUnclassifiedIdentity() {
    // No auth type set: the identity resolver did not classify the request.
    assertThrows(AccessDeniedException.class, () -> instanceController.getByNamespace("DEV",
        "SampleApp", "default", "application", null, 0, 20));

    verifyNoInteractions(instanceService);
  }

  @Test
  public void getByNamespaceShouldNormalizePaginationWhenPortalUserShouldHideConfig() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    PageDTO<InstanceDTO> negativePage = instanceController.getByNamespace("DEV", "SampleApp",
        "default", "application", null, -1, 0);
    assertTrue(negativePage.getContent().isEmpty());
    assertEquals(0, negativePage.getPage());
    assertEquals(10, negativePage.getSize());

    PageDTO<InstanceDTO> oversizedPage = instanceController.getByNamespace("DEV", "SampleApp",
        "default", "application", null, -5, 5000);
    assertTrue(oversizedPage.getContent().isEmpty());
    assertEquals(0, oversizedPage.getPage());
    assertEquals(2000, oversizedPage.getSize());

    // An omitted size arrives as 20 through the @RequestParam default and is kept as-is;
    // only an explicit size below one falls back to the AdminService resolver's 10.
    PageDTO<InstanceDTO> defaultPage = instanceController.getByNamespace("DEV", "SampleApp",
        "default", "application", null, 0, 20);
    assertTrue(defaultPage.getContent().isEmpty());
    assertEquals(0, defaultPage.getPage());
    assertEquals(20, defaultPage.getSize());

    verifyNoInteractions(instanceService);
  }

  private void setAnonymousTrustedUser() {
    setAnonymousUser("ssoUser");
  }

  private void setAnonymousUser(String userId) {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.ANONYMOUS);
    UserInfo user = new UserInfo();
    user.setUserId(userId);
    when(userInfoHolder.getUser()).thenReturn(user);
  }

  private void setAnonymousWithoutUser() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.ANONYMOUS);
    when(userInfoHolder.getUser()).thenReturn(null);
  }

  private ReleaseDTO release(long id) {
    ReleaseDTO release = new ReleaseDTO();
    release.setId(id);
    release.setAppId("SampleApp");
    release.setClusterName("default");
    release.setNamespaceName("application");
    release.setName("release-" + id);
    return release;
  }
}
