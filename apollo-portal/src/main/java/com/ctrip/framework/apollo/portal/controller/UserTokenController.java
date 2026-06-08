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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenCapability;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenCreateRequest;
import com.ctrip.framework.apollo.portal.entity.vo.usertoken.UserTokenInfo;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user-tokens")
public class UserTokenController {

  private final UserInfoHolder userInfoHolder;
  private final UserTokenService userTokenService;
  private final PortalConfig portalConfig;

  public UserTokenController(final UserInfoHolder userInfoHolder,
      final UserTokenService userTokenService, final PortalConfig portalConfig) {
    this.userInfoHolder = userInfoHolder;
    this.userTokenService = userTokenService;
    this.portalConfig = portalConfig;
  }

  @GetMapping
  public List<UserTokenInfo> list() {
    String userId = requirePortalUserSession();
    return userTokenService.findUserTokens(userId);
  }

  @PostMapping
  public UserTokenInfo create(@RequestBody UserTokenCreateRequest request) {
    String userId = requirePortalUserSession();
    return userTokenService.createToken(request, userId);
  }

  @PostMapping("/{tokenId}/revoke")
  public void revoke(@PathVariable long tokenId) {
    String userId = requirePortalUserSession();
    userTokenService.revokeToken(tokenId, userId);
  }

  @DeleteMapping("/{tokenId}")
  public void delete(@PathVariable long tokenId) {
    String userId = requirePortalUserSession();
    userTokenService.deleteToken(tokenId, userId);
  }

  @PostMapping("/{tokenId}/rotate")
  public UserTokenInfo rotate(@PathVariable long tokenId) {
    String userId = requirePortalUserSession();
    return userTokenService.rotateToken(tokenId, userId);
  }

  @GetMapping("/capabilities")
  public UserTokenCapability capabilities() {
    requirePortalUserSession();
    UserTokenCapability capability = new UserTokenCapability();
    capability.setOperations(userTokenService.findAvailableOperations());
    capability.setDefaultExpireDays(portalConfig.userTokenDefaultExpireDays());
    capability.setMaxExpireDays(portalConfig.userTokenMaxExpireDays());
    return capability;
  }

  @GetMapping("/admin")
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public List<UserTokenInfo> adminList(
      @RequestParam(value = "userId", required = false) String userId,
      @RequestParam(value = "status", required = false, defaultValue = "all") String status) {
    requirePortalUserSession();
    return userTokenService.findUserTokensForAdmin(userId, status);
  }

  @PostMapping("/admin/{tokenId}/revoke")
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public void adminRevoke(@PathVariable long tokenId) {
    String operator = requirePortalUserSession();
    userTokenService.revokeTokenForAdmin(tokenId, operator);
  }

  @DeleteMapping("/admin/{tokenId}")
  @PreAuthorize(value = "@unifiedPermissionValidator.isSuperAdmin()")
  public void adminDelete(@PathVariable long tokenId) {
    String operator = requirePortalUserSession();
    userTokenService.deleteTokenForAdmin(tokenId, operator);
  }

  private String requirePortalUserSession() {
    if (!UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      throw new AccessDeniedException("Portal user session is required");
    }
    return userInfoHolder.getUser().getUserId();
  }
}
