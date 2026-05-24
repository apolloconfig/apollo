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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Resolves mutation operators for OpenAPI requests according to the current identity type.
 */
@Component
public class OpenApiOperatorResolver {

  private final UserInfoHolder userInfoHolder;
  private final UserService userService;

  public OpenApiOperatorResolver(UserInfoHolder userInfoHolder, UserService userService) {
    this.userInfoHolder = userInfoHolder;
    this.userService = userService;
  }

  public String resolve(String operator) {
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      UserInfo loginUser = userInfoHolder.getUser();
      if (loginUser == null || !StringUtils.hasText(loginUser.getUserId())) {
        throw new BadRequestException("Current user not found");
      }
      return loginUser.getUserId();
    }

    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      if (!StringUtils.hasText(operator)) {
        throw new BadRequestException("operator should not be null or empty");
      }
      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
      return operator;
    }

    throw new BadRequestException("Unsupported auth type: %s",
        UserIdentityContextHolder.getAuthType());
  }
}
