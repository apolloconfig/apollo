/*
 * Copyright 2024 Apollo Authors
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

import static org.junit.jupiter.api.Assertions.*;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerCreateRequestVO;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ConsumerControllerTest {

  @Test
  void createWithBadRequest() {
    ConsumerService consumerService = Mockito.mock(ConsumerService.class);
    ConsumerController consumerController = new ConsumerController(consumerService);

    ConsumerCreateRequestVO requestVO = new ConsumerCreateRequestVO();
    // blank appId
    assertThrows(BadRequestException.class, () -> consumerController.create(requestVO, null));
    requestVO.setAppId("appId1");

    // blank name
    assertThrows(BadRequestException.class, () -> consumerController.create(requestVO, null));
    requestVO.setName("app 1");

    // blank ownerName
    assertThrows(BadRequestException.class, () -> consumerController.create(requestVO, null));
    requestVO.setOwnerName("user1");

    // blank orgId
    assertThrows(BadRequestException.class, () -> consumerController.create(requestVO, null));
    requestVO.setOrgId("orgId1");
  }

  @Test
  void createWithCompatibility() {
    ConsumerService consumerService = Mockito.mock(ConsumerService.class);
    ConsumerController consumerController = new ConsumerController(consumerService);
    ConsumerCreateRequestVO requestVO = new ConsumerCreateRequestVO();
    requestVO.setAppId("appId1");
    requestVO.setName("app 1");
    requestVO.setOwnerName("user1");
    requestVO.setOrgId("orgId1");
    consumerController.create(requestVO, null);

    Mockito.verify(consumerService, Mockito.times(1)).createConsumer(Mockito.any());
    Mockito.verify(consumerService, Mockito.times(1))
        .generateAndSaveConsumerToken(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(consumerService, Mockito.times(0))
        .assignCreateApplicationRoleToConsumer(Mockito.any());
    Mockito.verify(consumerService, Mockito.times(1)).getConsumerInfoByAppId(Mockito.any());
  }

  @Test
  void createAndAssignCreateApplicationRoleToConsumer() {
    ConsumerService consumerService = Mockito.mock(ConsumerService.class);
    ConsumerController consumerController = new ConsumerController(consumerService);
    ConsumerCreateRequestVO requestVO = new ConsumerCreateRequestVO();
    requestVO.setAppId("appId1");
    requestVO.setName("app 1");
    requestVO.setOwnerName("user1");
    requestVO.setOrgId("orgId1");
    requestVO.setAllowCreateApplication(true);

    final String token = "token-xxx";
    {
      ConsumerToken ConsumerToken = new ConsumerToken();
      ConsumerToken.setToken(token);
      Mockito.when(consumerService.generateAndSaveConsumerToken(Mockito.any(), Mockito.any(), Mockito.any()))
          .thenReturn(ConsumerToken);
    }
    consumerController.create(requestVO, null);

    Mockito.verify(consumerService, Mockito.times(1)).createConsumer(Mockito.any());
    Mockito.verify(consumerService, Mockito.times(1))
        .generateAndSaveConsumerToken(Mockito.any(), Mockito.any(), Mockito.any());
    Mockito.verify(consumerService, Mockito.times(1))
        .assignCreateApplicationRoleToConsumer(Mockito.eq(token));
    Mockito.verify(consumerService, Mockito.times(1)).getConsumerInfoByAppId(Mockito.any());
  }
}
