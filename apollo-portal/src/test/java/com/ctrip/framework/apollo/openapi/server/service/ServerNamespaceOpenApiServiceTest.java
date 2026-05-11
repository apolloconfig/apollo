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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.NamespaceLockService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class ServerNamespaceOpenApiServiceTest {

  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private ApplicationEventPublisher publisher;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private NamespaceLockService namespaceLockService;

  private ServerNamespaceOpenApiService service;

  @BeforeEach
  void setUp() {
    service = new ServerNamespaceOpenApiService(appNamespaceService, publisher, namespaceService,
        namespaceLockService);
  }

  @Test
  void createAppNamespaceShouldRejectBlankCreatedBy() {
    OpenAppNamespaceDTO dto = new OpenAppNamespaceDTO();
    dto.setDataChangeCreatedBy(" ");

    assertThrows(BadRequestException.class, () -> service.createAppNamespace(dto));

    verify(appNamespaceService, never()).createAppNamespaceInLocal(any(), anyBoolean(),
        anyString());
  }
}
