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
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

@RunWith(MockitoJUnitRunner.class)
public class NamespaceBranchControllerTest {

  private static final String APP_ID = "sample-app";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String NAMESPACE = "application";
  private static final String BRANCH = "gray-release";

  @Mock
  private ReleaseService releaseService;

  @Mock
  private NamespaceBranchService namespaceBranchService;

  @Mock
  private ApplicationEventPublisher publisher;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @InjectMocks
  private NamespaceBranchController namespaceBranchController;

  @Test
  public void shouldDenyBranchGrayRulesWhenConfigHiddenFromCurrentUser() {
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
  public void shouldReturnBranchGrayRulesWhenConfigVisibleToCurrentUser() {
    GrayReleaseRuleDTO rules = new GrayReleaseRuleDTO(APP_ID, CLUSTER, NAMESPACE, BRANCH);

    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER, NAMESPACE))
        .thenReturn(false);
    when(namespaceBranchService.findBranchGrayRules(eq(APP_ID), eq(Env.DEV), eq(CLUSTER),
        eq(NAMESPACE), eq(BRANCH))).thenReturn(rules);

    assertSame(rules,
        namespaceBranchController.getBranchGrayRules(APP_ID, ENV, CLUSTER, NAMESPACE, BRANCH));
  }
}
