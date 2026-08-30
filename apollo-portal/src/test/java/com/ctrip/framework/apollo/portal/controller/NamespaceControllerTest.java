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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceCreationModel;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.Collections;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

@RunWith(MockitoJUnitRunner.class)
public class NamespaceControllerTest {

  @Mock
  private ApplicationEventPublisher publisher;

  @Mock
  private UserInfoHolder userInfoHolder;

  @Mock
  private NamespaceService namespaceService;

  @Mock
  private AppNamespaceService appNamespaceService;

  @Mock
  private RoleInitializationService roleInitializationService;

  @Mock
  private PortalConfig portalConfig;

  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  @Mock
  private AdminServiceAPI.NamespaceAPI namespaceAPI;

  @InjectMocks
  private NamespaceController namespaceController;

  @Test
  public void shouldRejectCreateNamespaceWhenPathAppIdDiffersFromPayload() {
    NamespaceDTO namespace = new NamespaceDTO();
    namespace.setAppId("AnotherApp");
    namespace.setClusterName("default");
    namespace.setNamespaceName("application");

    NamespaceCreationModel model = new NamespaceCreationModel();
    model.setEnv("DEV");
    model.setNamespace(namespace);

    when(userInfoHolder.getUser()).thenReturn(new UserInfo("apollo"));

    try {
      namespaceController.createNamespace("SampleApp", Collections.singletonList(model));
      fail("Should throw");
    } catch (BadRequestException e) {
      assertEquals("AppId not equal. AppId in path = SampleApp, AppId in payload = AnotherApp",
          e.getMessage());
    }

    verifyNoInteractions(namespaceService, appNamespaceService, roleInitializationService,
        publisher);
  }

  @Test
  public void shouldRejectCreateAppNamespaceWhenPathAppIdDiffersFromPayload() {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setAppId("AnotherApp");
    appNamespace.setName("application");
    appNamespace.setFormat("properties");

    try {
      namespaceController.createAppNamespace("SampleApp", true, appNamespace);
      fail("Should throw");
    } catch (BadRequestException e) {
      assertEquals("AppId not equal. AppId in path = SampleApp, AppId in payload = AnotherApp",
          e.getMessage());
    }

    verifyNoInteractions(namespaceService, appNamespaceService, roleInitializationService,
        publisher);
  }
}
