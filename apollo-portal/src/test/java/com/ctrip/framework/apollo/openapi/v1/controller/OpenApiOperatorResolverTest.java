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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests operator resolution for OpenAPI identities.
 */
@ExtendWith(MockitoExtension.class)
public class OpenApiOperatorResolverTest {

  @Mock
  private UserInfoHolder userInfoHolder;

  @Mock
  private UserService userService;

  @AfterEach
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void resolveShouldUseCurrentUserTokenUserAndIgnoreOperator() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER_TOKEN);
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("token-user");
    when(userInfoHolder.getUser()).thenReturn(userInfo);
    OpenApiOperatorResolver resolver = new OpenApiOperatorResolver(userInfoHolder, userService);

    assertEquals("token-user", resolver.resolve("spoofed-user"));
    verify(userService, never()).findByUserId("spoofed-user");
  }
}
