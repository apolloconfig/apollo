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
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.UserManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenUserDTO;
import com.ctrip.framework.apollo.openapi.model.OpenUserInfoDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiModelConverters;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserService;
import com.ctrip.framework.apollo.portal.util.checker.AuthUserPasswordChecker;
import com.ctrip.framework.apollo.portal.util.checker.CheckResult;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.RestController;

/**
 * OpenAPI v1 controller for portal user management.
 */
@RestController("openapiPortalUserController")
public class PortalUserController implements UserManagementApi {
  private static final int USER_ENABLED = 1;

  private final UserInfoHolder userInfoHolder;
  private final UserService userService;
  private final AuthUserPasswordChecker passwordChecker;
  private final UnifiedPermissionValidator unifiedPermissionValidator;
  private final OpenApiOperatorResolver operatorResolver;

  public PortalUserController(UserInfoHolder userInfoHolder, UserService userService,
      AuthUserPasswordChecker passwordChecker,
      UnifiedPermissionValidator unifiedPermissionValidator,
      OpenApiOperatorResolver operatorResolver) {
    this.userInfoHolder = userInfoHolder;
    this.userService = userService;
    this.passwordChecker = passwordChecker;
    this.unifiedPermissionValidator = unifiedPermissionValidator;
    this.operatorResolver = operatorResolver;
  }

  @Override
  public ResponseEntity<OpenUserInfoDTO> getCurrentUser() {
    requirePortalUserRequest();
    return ResponseEntity.ok(OpenApiModelConverters.fromUserInfo(userInfoHolder.getUser()));
  }

  @Override
  public ResponseEntity<List<OpenUserInfoDTO>> searchUsers(String keyword,
      Boolean includeInactiveUsers, Integer offset, Integer limit) {
    requireUserManagementReadPermission();
    List<OpenUserInfoDTO> users = OpenApiModelConverters
        .fromUserInfos(userService.searchUsers(keyword, offset == null ? 0 : offset,
            limit == null ? 10 : limit, Boolean.TRUE.equals(includeInactiveUsers)));
    return ResponseEntity.ok(users);
  }

  @Override
  public ResponseEntity<OpenUserInfoDTO> getUserByUserId(String userId) {
    requireUserManagementReadPermission();
    UserInfo user = userService.findByUserId(userId);
    if (user == null) {
      throw BadRequestException.userNotExists(userId);
    }
    return ResponseEntity.ok(OpenApiModelConverters.fromUserInfo(user));
  }

  @Override
  public ResponseEntity<Void> createOrUpdateUser(OpenUserDTO openUserDTO, Boolean isCreate,
      String operator) {
    boolean consumerRequest = requireUserManagementMutationPermission(operator);
    UserPO user = OpenApiModelConverters.toUserPO(openUserDTO);
    if (StringUtils.isContainEmpty(user.getUsername(), user.getPassword())) {
      throw new BadRequestException("Username and password can not be empty.");
    }

    if (!consumerRequest && !unifiedPermissionValidator.isSuperAdmin()
        && (!user.getUsername().equals(userInfoHolder.getUser().getUserId())
            || user.getEnabled() != USER_ENABLED)) {
      throw new AccessDeniedException("Create or update user operation is forbidden");
    }

    CheckResult pwdCheckRes = passwordChecker.checkWeakPassword(user.getPassword());
    if (!pwdCheckRes.isSuccess()) {
      throw new BadRequestException(pwdCheckRes.getMessage());
    }

    if (userService instanceof SpringSecurityUserService) {
      if (Boolean.TRUE.equals(isCreate)) {
        ((SpringSecurityUserService) userService).create(user);
      } else {
        ((SpringSecurityUserService) userService).update(user);
      }
    } else {
      throw new UnsupportedOperationException("Create or update user operation is unsupported");
    }
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> changeUserEnabled(OpenUserDTO openUserDTO, String operator) {
    boolean consumerRequest = requireUserManagementMutationPermission(operator);
    if (!consumerRequest && !unifiedPermissionValidator.isSuperAdmin()) {
      throw new AccessDeniedException("Change user enabled operation is forbidden");
    }
    UserPO user = OpenApiModelConverters.toUserPO(openUserDTO);
    if (userService instanceof SpringSecurityUserService) {
      ((SpringSecurityUserService) userService).changeEnabled(user);
    } else {
      throw new UnsupportedOperationException("change user enabled is unsupported");
    }
    return ResponseEntity.ok().build();
  }

  private void requirePortalUserRequest() {
    if (!UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      throw new AccessDeniedException("Portal user session is required");
    }
  }

  private void requireUserManagementReadPermission() {
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      return;
    }
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())
        && unifiedPermissionValidator.hasManageUsersPermission()) {
      return;
    }
    throw new AccessDeniedException("Manage users permission is required");
  }

  private boolean requireUserManagementMutationPermission(String operator) {
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      return false;
    }
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      if (!unifiedPermissionValidator.hasManageUsersPermission()) {
        throw new AccessDeniedException("Manage users permission is required");
      }
      operatorResolver.resolve(operator);
      return true;
    }
    throw new AccessDeniedException("Unsupported auth type");
  }
}
