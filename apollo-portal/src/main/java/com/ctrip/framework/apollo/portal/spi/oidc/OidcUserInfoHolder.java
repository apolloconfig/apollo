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
package com.ctrip.framework.apollo.portal.spi.oidc;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.configuration.OidcExtendProperties;
import java.security.Principal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class OidcUserInfoHolder implements UserInfoHolder {

  private static final Logger log = LoggerFactory.getLogger(OidcUserInfoHolder.class);

  private final UserService userService;

  private final OidcExtendProperties oidcExtendProperties;

  public OidcUserInfoHolder(UserService userService, OidcExtendProperties oidcExtendProperties) {
    this.userService = userService;
    this.oidcExtendProperties = oidcExtendProperties;
  }

  @Override
  public UserInfo getUser() {
    UserInfo userInfo = this.getUserInternal();
    if (StringUtils.hasText(userInfo.getName())) {
      return userInfo;
    }
    UserInfo userInfoFound = this.userService.findByUserId(userInfo.getUserId());
    if (userInfoFound != null) {
      return userInfoFound;
    }
    return userInfo;
  }

  private UserInfo getUserInternal() {
    Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    if (principal instanceof OidcUser) {
      UserInfo userInfo = new UserInfo();
      OidcUser oidcUser = (OidcUser) principal;
      userInfo.setUserId(oidcUser.getSubject());
      userInfo.setName(
          OidcUserInfoUtil.getOidcUserDisplayName(oidcUser, this.oidcExtendProperties));
      userInfo.setEmail(oidcUser.getEmail());
      return userInfo;
    }
    if (principal instanceof Jwt) {
      Jwt jwt = (Jwt) principal;
      UserInfo userInfo = new UserInfo();
      userInfo.setUserId(jwt.getSubject());
      userInfo.setName(OidcUserInfoUtil.getJwtUserDisplayName(jwt, this.oidcExtendProperties));
      return userInfo;
    }
    log.debug("principal is neither oidcUser nor jwt, principal=[{}]", principal);
    if (principal instanceof OAuth2User) {
      UserInfo userInfo = new UserInfo();
      OAuth2User oAuth2User = (OAuth2User) principal;
      userInfo.setUserId(oAuth2User.getName());
      userInfo.setName(oAuth2User.getAttribute(StandardClaimNames.PREFERRED_USERNAME));
      userInfo.setEmail(oAuth2User.getAttribute(StandardClaimNames.EMAIL));
      return userInfo;
    }
    if (principal instanceof Principal) {
      UserInfo userInfo = new UserInfo();
      Principal userPrincipal = (Principal) principal;
      userInfo.setUserId(userPrincipal.getName());
      return userInfo;
    }
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(String.valueOf(principal));
    return userInfo;
  }
}
