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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ReleaseHistoryService;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReleaseHistoryControllerTest {

  @Mock
  private ReleaseHistoryService releaseHistoryService;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @InjectMocks
  private ReleaseHistoryController releaseHistoryController;

  @Test
  public void shouldReturnEmptyListWhenConfigShouldBeHidden() {
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(true);

    List<ReleaseHistoryBO> result = releaseHistoryController.findReleaseHistoriesByNamespace(
        "SampleApp", "DEV", "default", "application", 0, 10);

    assertTrue(result.isEmpty());
    verify(releaseHistoryService, never())
        .findNamespaceReleaseHistory("SampleApp", Env.DEV, "default", "application", 0, 10);
  }

  @Test
  public void shouldDelegateToServiceWhenConfigIsVisible() {
    ReleaseHistoryBO releaseHistoryBO = new ReleaseHistoryBO();
    List<ReleaseHistoryBO> expected = Collections.singletonList(releaseHistoryBO);
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser("SampleApp", "DEV", "default",
        "application")).thenReturn(false);
    when(releaseHistoryService.findNamespaceReleaseHistory("SampleApp", Env.DEV, "default",
        "application", 1, 20)).thenReturn(expected);

    List<ReleaseHistoryBO> result = releaseHistoryController
        .findReleaseHistoriesByNamespace("SampleApp", "DEV", "default", "application", 1, 20);

    assertSame(expected, result);
    verify(releaseHistoryService).findNamespaceReleaseHistory("SampleApp", Env.DEV, "default",
        "application", 1, 20);
  }
}
