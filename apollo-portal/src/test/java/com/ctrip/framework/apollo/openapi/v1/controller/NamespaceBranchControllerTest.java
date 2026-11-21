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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

@RunWith(MockitoJUnitRunner.class)
public class NamespaceBranchControllerTest {

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

  @InjectMocks
  private NamespaceBranchController namespaceBranchController;

  @After
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void shouldDenyBranchGrayRulesForPortalUserWhenConfigHidden() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(true);

    try {
      namespaceBranchController.getBranchGrayRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH);
      fail("Should throw");
    } catch (AccessDeniedException e) {
      assertEquals("Access is denied", e.getMessage());
    }

    verifyNoInteractions(namespaceBranchService);
  }

  @Test
  public void shouldReturnBranchGrayRulesForPortalUserWhenConfigVisible() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    GrayReleaseRuleDTO rules = new GrayReleaseRuleDTO(APP_ID, CLUSTER, NAMESPACE, BRANCH);

    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);
    when(namespaceBranchService.findBranchGrayRules(eq(APP_ID), eq(Env.DEV), eq(CLUSTER),
        eq(NAMESPACE), eq(BRANCH))).thenReturn(rules);

    OpenGrayReleaseRuleDTO result =
        namespaceBranchController.getBranchGrayRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH);

    assertNotNull(result);
    assertEquals(APP_ID, result.getAppId());
    verify(namespaceBranchService).findBranchGrayRules(APP_ID, Env.DEV, CLUSTER, NAMESPACE,
        BRANCH);
  }

  @Test
  public void shouldNotGateBranchGrayRulesForConsumerToken() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);
    GrayReleaseRuleDTO rules = new GrayReleaseRuleDTO(APP_ID, CLUSTER, NAMESPACE, BRANCH);

    when(namespaceBranchService.findBranchGrayRules(eq(APP_ID), eq(Env.DEV), eq(CLUSTER),
        eq(NAMESPACE), eq(BRANCH))).thenReturn(rules);

    OpenGrayReleaseRuleDTO result =
        namespaceBranchController.getBranchGrayRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH);

    assertNotNull(result);
    assertEquals(APP_ID, result.getAppId());
    verify(namespaceBranchService).findBranchGrayRules(APP_ID, Env.DEV, CLUSTER, NAMESPACE,
        BRANCH);
  }
}
