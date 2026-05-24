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

import static com.ctrip.framework.apollo.common.constants.AccessKeyMode.FILTER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.AccessKeyDTO;
import com.ctrip.framework.apollo.openapi.model.OpenAccessKeyDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AccessKeyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerAccessKeyOpenApiServiceTest {

  @Mock
  private AccessKeyService accessKeyService;

  private ServerAccessKeyOpenApiService service;

  @BeforeEach
  void setUp() {
    service = new ServerAccessKeyOpenApiService(accessKeyService);
  }

  @Test
  void createAccessKeyShouldPopulateOperatorAndSecret() {
    when(accessKeyService.createAccessKey(eq(Env.DEV), any(AccessKeyDTO.class)))
        .thenAnswer(invocation -> invocation.getArgument(1));

    OpenAccessKeyDTO result = service.createAccessKey("some-app", "DEV", "operator");

    ArgumentCaptor<AccessKeyDTO> captor = ArgumentCaptor.forClass(AccessKeyDTO.class);
    verify(accessKeyService).createAccessKey(eq(Env.DEV), captor.capture());
    AccessKeyDTO request = captor.getValue();
    assertThat(request.getAppId()).isEqualTo("some-app");
    assertThat(request.getSecret()).isNotBlank();
    assertThat(request.getDataChangeCreatedBy()).isEqualTo("operator");
    assertThat(request.getDataChangeLastModifiedBy()).isEqualTo("operator");
    assertThat(result.getAppId()).isEqualTo("some-app");
    assertThat(result.getDataChangeCreatedBy()).isEqualTo("operator");
  }

  @Test
  void enableAccessKeyShouldDefaultModeToFilter() {
    service.enableAccessKey("some-app", "DEV", 100L, null, "operator");

    verify(accessKeyService).enable(Env.DEV, "some-app", 100L, FILTER, "operator");
  }

  @Test
  void disableAccessKeyShouldDelegateOperator() {
    service.disableAccessKey("some-app", "DEV", 100L, "operator");

    verify(accessKeyService).disable(Env.DEV, "some-app", 100L, "operator");
  }
}
