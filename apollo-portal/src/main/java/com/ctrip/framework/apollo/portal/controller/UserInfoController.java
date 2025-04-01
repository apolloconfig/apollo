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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.spi.LogoutHandler;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.springsecurity.SpringSecurityUserService;
import com.ctrip.framework.apollo.portal.util.checker.AuthUserPasswordChecker;
import com.ctrip.framework.apollo.portal.util.checker.CheckResult;
import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserInfoController {

  private final UserInfoHolder userInfoHolder;
  private final LogoutHandler logoutHandler;
  private final UserService userService;
  private final AuthUserPasswordChecker passwordChecker;

  public UserInfoController(
      final UserInfoHolder userInfoHolder,
      final LogoutHandler logoutHandler,
      final UserService userService,
      final AuthUserPasswordChecker passwordChecker) {
    this.userInfoHolder = userInfoHolder;
    this.logoutHandler = logoutHandler;
    this.userService = userService;
    this.passwordChecker = passwordChecker;
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @PostMapping("/users")
  public void createOrUpdateUser(
      @RequestParam(value = "isCreate", defaultValue = "false") boolean isCreate,
      @RequestBody UserPO user) {
    if (StringUtils.isContainEmpty(user.getUsername(), user.getPassword())) {
      throw new BadRequestException("Username and password can not be empty.");
    }

    CheckResult pwdCheckRes = passwordChecker.checkWeakPassword(user.getPassword());
    if (!pwdCheckRes.isSuccess()) {
      throw new BadRequestException(pwdCheckRes.getMessage());
    }

    if (userService instanceof SpringSecurityUserService) {
      if (isCreate) {
        ((SpringSecurityUserService) userService).create(user);
      } else {
        ((SpringSecurityUserService) userService).update(user);
      }
    } else {
      throw new UnsupportedOperationException("Create or update user operation is unsupported");
    }
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @PutMapping("/users/enabled")
  public void changeUserEnabled(@RequestBody UserPO user) {
    if (userService instanceof SpringSecurityUserService) {
      ((SpringSecurityUserService) userService).changeEnabled(user);
    } else {
      throw new UnsupportedOperationException("change user enabled is unsupported");
    }
  }

  @GetMapping("/user")
  public UserInfo getCurrentUserName() {
    return userInfoHolder.getUser();
  }

  @GetMapping("/user/logout")
  public void logout(HttpServletRequest request, HttpServletResponse response) throws IOException {
    logoutHandler.logout(request, response);
  }

  @GetMapping("/users")
  public List<UserInfo> searchUsersByKeyword(@RequestParam(value = "keyword") String keyword,
      @RequestParam(value = "includeInactiveUsers", defaultValue = "false") boolean includeInactiveUsers,
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "10") int limit) {
    return userService.searchUsers(keyword, offset, limit, includeInactiveUsers);
  }

  @GetMapping("/users/{userId}")
  public UserInfo getUserByUserId(@PathVariable String userId) {
    return userService.findByUserId(userId);
  }

}
