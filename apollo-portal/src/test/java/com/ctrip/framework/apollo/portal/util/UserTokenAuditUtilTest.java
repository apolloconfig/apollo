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
package com.ctrip.framework.apollo.portal.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.po.UserTokenAudit;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Unit tests for user token audit queue shutdown behavior.
 */
class UserTokenAuditUtilTest {

  private final UserTokenService userTokenService =
      org.mockito.Mockito.mock(UserTokenService.class);

  @Test
  void stopAuditFlushesQueuedAudits() {
    UserTokenAuditUtil auditUtil = new UserTokenAuditUtil(userTokenService);
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/openapi/v1/apps");
    request.setQueryString("page=1");
    UserToken userToken = new UserToken();
    userToken.setId(8L);
    userToken.setUserId("apollo");

    auditUtil.audit(request, userToken);
    auditUtil.stopAudit();

    @SuppressWarnings({"rawtypes", "unchecked"})
    ArgumentCaptor<Iterable<UserTokenAudit>> captor = ArgumentCaptor.forClass(Iterable.class);
    verify(userTokenService).createUserTokenAudits(captor.capture());
    List<UserTokenAudit> audits = com.google.common.collect.Lists.newArrayList(captor.getValue());
    assertEquals(1, audits.size());
    assertEquals(8L, audits.get(0).getTokenId());
    assertEquals("apollo", audits.get(0).getUserId());
    assertEquals("/openapi/v1/apps?page=1", audits.get(0).getUri());
  }
}
