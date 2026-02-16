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

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.dto.OpenUserDTO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserService;
import com.ctrip.framework.apollo.portal.util.checker.AuthUserPasswordChecker;
import com.ctrip.framework.apollo.portal.util.checker.CheckResult;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * OpenAPI User Management Controller Provides RESTful APIs for user management operations through
 * OpenAPI
 *
 * @author dreamweaver
 */
@RestController("openapiUserController")
@RequestMapping("/openapi/v1")
public class UserController {

  private static final int DEFAULT_USER_ENABLED = 1;

  private final UserService userService;
  private final AuthUserPasswordChecker passwordChecker;

  public UserController(final UserService userService,
      final AuthUserPasswordChecker passwordChecker) {
    this.userService = userService;
    this.passwordChecker = passwordChecker;
  }

  /**
   * Create a new user
   *
   * @param openUserDTO user information to create
   * @return ResponseEntity with created user information
   */
  @ApolloAuditLog(name = "OpenAPI.createUser", type = OpType.CREATE, description = "Create user via OpenAPI")
  @PostMapping("/users")
  public ResponseEntity<UserInfo> createUser(@RequestBody OpenUserDTO openUserDTO) {
    // Validate required fields
    if (StringUtils.isContainEmpty(openUserDTO.getUsername(), openUserDTO.getPassword())) {
      throw new BadRequestException("Username and password cannot be empty.");
    }

    if (StringUtils.isEmpty(openUserDTO.getEmail())) {
      throw new BadRequestException("Email cannot be empty.");
    }

    // Check password strength
    CheckResult pwdCheckRes = passwordChecker.checkWeakPassword(openUserDTO.getPassword());
    if (!pwdCheckRes.isSuccess()) {
      throw new BadRequestException(pwdCheckRes.getMessage());
    }

    // Check if UserService supports user creation
    if (!(userService instanceof SpringSecurityUserService)) {
      throw new UnsupportedOperationException(
          "Create user operation is not supported with current user service implementation");
    }

    // Convert DTO to PO and set defaults
    UserPO userPO = new UserPO();
    userPO.setUsername(openUserDTO.getUsername());
    userPO.setPassword(openUserDTO.getPassword());
    userPO.setEmail(openUserDTO.getEmail());
    userPO.setUserDisplayName(openUserDTO.getUserDisplayName() != null
        ? openUserDTO.getUserDisplayName()
        : openUserDTO.getUsername());
    userPO.setEnabled(openUserDTO.getEnabled() != null
        ? openUserDTO.getEnabled()
        : DEFAULT_USER_ENABLED);

    // Create user
    ((SpringSecurityUserService) userService).create(userPO);

    // Retrieve and return the created user information
    UserInfo createdUser = userService.findByUserId(openUserDTO.getUsername());
    return ResponseEntity.ok(createdUser);
  }

  /**
   * Get user information by user ID
   *
   * @param userId the user ID to query
   * @return UserInfo object
   */
  @GetMapping("/users/{userId}")
  public ResponseEntity<UserInfo> getUserByUserId(@PathVariable String userId) {
    UserInfo userInfo = userService.findByUserId(userId);
    if (userInfo == null) {
      throw new BadRequestException("User not found: " + userId);
    }
    return ResponseEntity.ok(userInfo);
  }

  /**
   * Search users by keyword
   *
   * @param keyword              keyword to search (searches in username and display name)
   * @param includeInactiveUsers whether to include inactive users
   * @param offset               pagination offset
   * @param limit                pagination limit
   * @return list of UserInfo objects
   */
  @GetMapping("/users")
  public ResponseEntity<List<UserInfo>> searchUsers(
      @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword,
      @RequestParam(value = "includeInactiveUsers", defaultValue = "false") boolean includeInactiveUsers,
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "10") int limit) {

    if (limit <= 0 || limit > 100) {
      throw new BadRequestException("Limit must be between 1 and 100");
    }

    if (offset < 0) {
      throw new BadRequestException("Offset must be non-negative");
    }

    List<UserInfo> users = userService.searchUsers(keyword, offset, limit, includeInactiveUsers);
    return ResponseEntity.ok(users);
  }
}

