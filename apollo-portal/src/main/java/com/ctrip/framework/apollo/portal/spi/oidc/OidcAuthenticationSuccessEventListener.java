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
import com.ctrip.framework.apollo.portal.spi.configuration.OidcExtendProperties;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class OidcAuthenticationSuccessEventListener implements
    ApplicationListener<AuthenticationSuccessEvent> {

  private static final Logger log = LoggerFactory
      .getLogger(OidcAuthenticationSuccessEventListener.class);

  private static final Logger oidcLog = LoggerFactory.getLogger(
      OidcAuthenticationSuccessEventListener.class.getName() + ".oidc");

  private static final Logger jwtLog = LoggerFactory.getLogger(
      OidcAuthenticationSuccessEventListener.class.getName() + ".jwt");

  private final OidcLocalUserService oidcLocalUserService;

  private final OidcExtendProperties oidcExtendProperties;

  private final ConcurrentMap<String, String> userIdCache = new ConcurrentHashMap<>();

  public OidcAuthenticationSuccessEventListener(
      OidcLocalUserService oidcLocalUserService, OidcExtendProperties oidcExtendProperties) {
    this.oidcLocalUserService = oidcLocalUserService;
    this.oidcExtendProperties = oidcExtendProperties;
  }

  @Override
  public void onApplicationEvent(AuthenticationSuccessEvent event) {
    Object principal = event.getAuthentication().getPrincipal();
    if (principal instanceof OidcUser) {
      this.oidcUserLogin((OidcUser) principal);
      return;
    }
    if (principal instanceof Jwt) {
      this.jwtLogin((Jwt) principal);
      return;
    }
    log.warn("principal is neither oidcUser nor jwt, principal=[{}]", principal);
  }

  private void oidcUserLogin(OidcUser oidcUser) {
    String subject = oidcUser.getSubject();
    String userDisplayName = OidcUserInfoUtil.getOidcUserDisplayName(oidcUser,
        this.oidcExtendProperties);
    String email = oidcUser.getEmail();

    this.logOidc(oidcUser, subject, userDisplayName, email);

    UserInfo newUserInfo = new UserInfo();
    newUserInfo.setUserId(subject);
    newUserInfo.setName(userDisplayName);
    newUserInfo.setEmail(email);
    if (this.contains(subject)) {
      this.oidcLocalUserService.updateUserInfo(newUserInfo);
      return;
    }
    this.oidcLocalUserService.createLocalUser(newUserInfo);
  }

  private void logOidc(OidcUser oidcUser, String subject, String userDisplayName,
      String email) {
    oidcLog.debug("oidc authentication success, sub=[{}] userDisplayName=[{}] email=[{}]", subject,
        userDisplayName, email);
    if (oidcLog.isTraceEnabled()) {
      Map<String, Object> claims = oidcUser.getClaims();
      for (Entry<String, Object> entry : claims.entrySet()) {
        oidcLog.trace("oidc authentication claims [{}={}]", entry.getKey(), entry.getValue());
      }
    }
  }

  private boolean contains(String userId) {
    if (this.userIdCache.containsKey(userId)) {
      return true;
    }
    UserInfo userInfo = this.oidcLocalUserService.findByUserId(userId);
    if (userInfo != null) {
      this.userIdCache.put(userId, userId);
      return true;
    }
    return false;
  }

  private void jwtLogin(Jwt jwt) {
    String subject = jwt.getSubject();
    String userDisplayName = OidcUserInfoUtil.getJwtUserDisplayName(jwt,
        this.oidcExtendProperties);

    this.logJwt(jwt, subject, userDisplayName);

    if (this.contains(subject)) {
      return;
    }
    UserInfo newUserInfo = new UserInfo();
    newUserInfo.setUserId(subject);
    newUserInfo.setName(userDisplayName);
    this.oidcLocalUserService.createLocalUser(newUserInfo);
  }

  private void logJwt(Jwt jwt, String subject, String userDisplayName) {
    jwtLog.debug("jwt authentication success, sub=[{}] userDisplayName=[{}]", subject,
        userDisplayName);
    if (jwtLog.isTraceEnabled()) {
      Map<String, Object> claims = jwt.getClaims();
      for (Entry<String, Object> entry : claims.entrySet()) {
        jwtLog.trace("jwt authentication claims [{}={}]", entry.getKey(), entry.getValue());
      }
    }
  }
}
