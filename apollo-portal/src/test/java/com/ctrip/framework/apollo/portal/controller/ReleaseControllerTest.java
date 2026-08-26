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

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ReleaseControllerTest {

  private static final String ENV = "DEV";

  @Mock
  private ReleaseService releaseService;

  @Mock
  private ApplicationEventPublisher publisher;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @Mock
  private UserInfoHolder userInfoHolder;

  @InjectMocks
  private ReleaseController releaseController;

  @Test
  void shouldCompareReleasesWhenBothAreVisible() {
    ReleaseDTO baseRelease = release(1, "BaseApp", "base-cluster", "base-namespace");
    ReleaseDTO toCompareRelease = release(2, "TargetApp", "target-cluster", "target-namespace");
    ReleaseCompareResult expected = new ReleaseCompareResult();
    when(releaseService.findReleaseById(Env.DEV, 1)).thenReturn(baseRelease);
    when(releaseService.findReleaseById(Env.DEV, 2)).thenReturn(toCompareRelease);
    when(releaseService.compare(baseRelease, toCompareRelease)).thenReturn(expected);

    ReleaseCompareResult result = releaseController.compareRelease(ENV, 1, 2);

    assertSame(expected, result);
    verify(unifiedPermissionValidator).shouldHideConfigToCurrentUser("BaseApp", ENV, "base-cluster",
        "base-namespace");
    verify(unifiedPermissionValidator).shouldHideConfigToCurrentUser("TargetApp", ENV,
        "target-cluster", "target-namespace");
  }

  @Test
  void shouldRejectCompareWhenBaseReleaseIsHidden() {
    ReleaseDTO baseRelease = release(1, "BaseApp", "default", "application");
    when(releaseService.findReleaseById(Env.DEV, 1)).thenReturn(baseRelease);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("BaseApp", ENV, "default",
        "application")).thenReturn(true);

    assertThrows(AccessDeniedException.class, () -> releaseController.compareRelease(ENV, 1, 2));

    verify(releaseService, never()).findReleaseById(Env.DEV, 2);
    verify(releaseService, never()).compare(baseRelease, null);
  }

  @Test
  void shouldRejectCompareWhenTargetReleaseIsHidden() {
    ReleaseDTO baseRelease = release(1, "BaseApp", "default", "application");
    ReleaseDTO toCompareRelease = release(2, "TargetApp", "default", "application");
    when(releaseService.findReleaseById(Env.DEV, 1)).thenReturn(baseRelease);
    when(releaseService.findReleaseById(Env.DEV, 2)).thenReturn(toCompareRelease);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("BaseApp", ENV, "default",
        "application")).thenReturn(false);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("TargetApp", ENV, "default",
        "application")).thenReturn(true);

    assertThrows(AccessDeniedException.class, () -> releaseController.compareRelease(ENV, 1, 2));

    verify(releaseService, never()).compare(baseRelease, toCompareRelease);
  }

  @Test
  void shouldRejectMissingBaseRelease() {
    when(releaseService.findReleaseById(Env.DEV, 1)).thenReturn(null);

    assertThrows(NotFoundException.class,
        () -> releaseController.compareRelease(ENV, 1, 2));

    verifyNoInteractions(unifiedPermissionValidator);
    verify(releaseService, never()).findReleaseById(Env.DEV, 2);
  }

  @Test
  void shouldRejectMissingTargetRelease() {
    ReleaseDTO baseRelease = release(1, "BaseApp", "default", "application");
    when(releaseService.findReleaseById(Env.DEV, 1)).thenReturn(baseRelease);
    when(releaseService.findReleaseById(Env.DEV, 2)).thenReturn(null);

    assertThrows(NotFoundException.class, () -> releaseController.compareRelease(ENV, 1, 2));

    verify(releaseService, never()).compare(baseRelease, null);
  }

  @Test
  void shouldPreserveZeroBaseRelease() {
    ReleaseDTO toCompareRelease = release(2, "TargetApp", "default", "application");
    ReleaseCompareResult expected = new ReleaseCompareResult();
    when(releaseService.findReleaseById(Env.DEV, 2)).thenReturn(toCompareRelease);
    when(releaseService.compare(null, toCompareRelease)).thenReturn(expected);

    ReleaseCompareResult result = releaseController.compareRelease(ENV, 0, 2);

    assertSame(expected, result);
    verify(releaseService, never()).findReleaseById(Env.DEV, 0);
  }

  @Test
  void shouldPreserveZeroTargetRelease() {
    ReleaseDTO baseRelease = release(1, "BaseApp", "default", "application");
    ReleaseCompareResult expected = new ReleaseCompareResult();
    when(releaseService.findReleaseById(Env.DEV, 1)).thenReturn(baseRelease);
    when(releaseService.compare(baseRelease, null)).thenReturn(expected);

    ReleaseCompareResult result = releaseController.compareRelease(ENV, 1, 0);

    assertSame(expected, result);
    verify(releaseService, never()).findReleaseById(Env.DEV, 0);
  }

  @Test
  void shouldPreserveTwoZeroReleases() {
    ReleaseCompareResult expected = new ReleaseCompareResult();
    when(releaseService.compare(null, null)).thenReturn(expected);

    ReleaseCompareResult result = releaseController.compareRelease(ENV, 0, 0);

    assertSame(expected, result);
    verifyNoInteractions(unifiedPermissionValidator);
    verify(releaseService, never()).findReleaseById(Env.DEV, 0);
  }

  private static ReleaseDTO release(long id, String appId, String clusterName,
      String namespaceName) {
    ReleaseDTO release = new ReleaseDTO();
    release.setId(id);
    release.setAppId(appId);
    release.setClusterName(clusterName);
    release.setNamespaceName(namespaceName);
    return release;
  }
}
