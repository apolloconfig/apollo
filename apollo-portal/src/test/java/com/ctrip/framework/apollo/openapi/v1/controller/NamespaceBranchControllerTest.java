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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NamespaceBranchControllerTest {

  private static final String APP_ID = "sample-app";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String BRANCH = "gray-release";

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @Mock
  private ReleaseService releaseService;

  @Mock
  private NamespaceBranchService namespaceBranchService;

  @Mock
  private UserService userService;

  @Mock
  private UserInfoHolder userInfoHolder;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private ApplicationEventPublisher publisher;

  private NamespaceBranchController namespaceBranchController;

  @BeforeEach
  void setUp() {
    namespaceBranchController =
        new NamespaceBranchController(unifiedPermissionValidator, releaseService,
            namespaceBranchService, userService, userInfoHolder, portalConfig, publisher);
  }

  @AfterEach
  void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  void shouldDenyBranchGrayRulesForPortalUserWhenConfigHidden() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(true);

    assertThatThrownBy(
        () -> namespaceBranchController.getBranchGrayRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH))
        .isInstanceOf(AccessDeniedException.class).hasMessage("Access is denied");

    verifyNoInteractions(namespaceBranchService);
  }

  @Test
  void shouldReturnBranchGrayRulesForPortalUserWhenConfigVisible() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    GrayReleaseRuleDTO rules = new GrayReleaseRuleDTO(APP_ID, CLUSTER, NAMESPACE, BRANCH);

    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);
    when(namespaceBranchService.findBranchGrayRules(eq(APP_ID), eq(Env.DEV), eq(CLUSTER),
        eq(NAMESPACE), eq(BRANCH))).thenReturn(rules);

    ResponseEntity<OpenGrayReleaseRuleDTO> response =
        namespaceBranchController.getBranchGrayRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getAppId()).isEqualTo(APP_ID);
    verify(namespaceBranchService).findBranchGrayRules(APP_ID, Env.DEV, CLUSTER, NAMESPACE, BRANCH);
  }

  @Test
  void shouldDenyBranchGrayRulesForUserTokenWhenConfigHidden() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(true);

    assertThatThrownBy(
        () -> namespaceBranchController.getBranchGrayRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH))
        .isInstanceOf(AccessDeniedException.class).hasMessage("Access is denied");

    verifyNoInteractions(namespaceBranchService);
  }
}
