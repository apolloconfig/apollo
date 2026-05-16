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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerReleaseOpenApiServiceTest {

  @Mock
  private ReleaseService releaseService;

  private ServerReleaseOpenApiService service;

  @BeforeEach
  void setUp() {
    service = new ServerReleaseOpenApiService(releaseService);
  }

  @Test
  void publishNamespaceShouldRejectBlankReleasedBy() {
    NamespaceReleaseDTO dto = new NamespaceReleaseDTO();
    dto.setReleasedBy(" ");

    assertThrows(BadRequestException.class,
        () -> service.publishNamespace("appId", "DEV", "default", "application", dto));

    verify(releaseService, never()).publish(any(NamespaceReleaseModel.class));
  }

  @Test
  void publishNamespaceShouldRejectNullDto() {
    BadRequestException exception = assertThrows(BadRequestException.class,
        () -> service.publishNamespace("appId", "DEV", "default", "application", null));

    assertThat(exception.getMessage()).isEqualTo("Request body can not be empty.");
    verify(releaseService, never()).publish(any(NamespaceReleaseModel.class));
  }
}
