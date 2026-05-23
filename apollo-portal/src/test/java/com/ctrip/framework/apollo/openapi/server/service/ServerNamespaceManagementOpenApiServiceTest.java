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
package com.ctrip.framework.apollo.openapi.server.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceLockService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ServerNamespaceManagementOpenApiServiceTest {

  private static final String APP_ID = "sample-app";
  private static final String ENV = "DEV";
  private static final String CLUSTER = "default";
  private static final String LINKED_NAMESPACE = "linked-public";
  private static final String PUBLIC_NAMESPACE = "provider.public-namespace";

  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private ApplicationEventPublisher publisher;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private NamespaceLockService namespaceLockService;
  @Mock
  private RoleInitializationService roleInitializationService;
  @Mock
  private PortalConfig portalConfig;
  @Mock
  private AdminServiceAPI.NamespaceAPI namespaceAPI;
  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;

  private ServerNamespaceManagementOpenApiService service;

  @BeforeEach
  void setUp() {
    service = new ServerNamespaceManagementOpenApiService(appNamespaceService, publisher,
        namespaceService, namespaceLockService, roleInitializationService, portalConfig,
        namespaceAPI, unifiedPermissionValidator);
  }

  @AfterEach
  void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  void findPublicNamespaceForAssociatedNamespaceShouldNotHidePublicNamespaceItems() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
    NamespaceBO publicNamespace = createNamespaceBO();
    when(namespaceService.findPublicNamespaceForAssociatedNamespace(Env.valueOf(ENV), APP_ID,
        CLUSTER, LINKED_NAMESPACE)).thenReturn(publicNamespace);
    lenient().when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER,
        LINKED_NAMESPACE)).thenReturn(true);

    OpenNamespaceDTO result = service.findPublicNamespaceForAssociatedNamespace(ENV, APP_ID,
        CLUSTER, LINKED_NAMESPACE, true);

    assertThat(result.getNamespaceName()).isEqualTo(PUBLIC_NAMESPACE);
    assertThat(result.getItems()).hasSize(1);
    assertThat(result.getItems().get(0).getKey()).isEqualTo("timeout");
    assertThat(result.getExtendInfo().getIsConfigHidden()).isFalse();
    verify(unifiedPermissionValidator, never()).shouldHideConfigToCurrentUser(APP_ID, ENV, CLUSTER,
        LINKED_NAMESPACE);
  }

  private NamespaceBO createNamespaceBO() {
    NamespaceDTO baseInfo = new NamespaceDTO();
    baseInfo.setId(100L);
    baseInfo.setAppId(APP_ID);
    baseInfo.setClusterName(CLUSTER);
    baseInfo.setNamespaceName(PUBLIC_NAMESPACE);

    ItemDTO item = new ItemDTO("timeout", "100", "", 0);
    item.setNamespaceId(100L);
    ItemBO itemBO = new ItemBO();
    itemBO.setItem(item);

    NamespaceBO namespaceBO = new NamespaceBO();
    namespaceBO.setBaseInfo(baseInfo);
    namespaceBO.setPublic(true);
    namespaceBO.setFormat("properties");
    namespaceBO.setItems(Collections.singletonList(itemBO));
    return namespaceBO;
  }
}
