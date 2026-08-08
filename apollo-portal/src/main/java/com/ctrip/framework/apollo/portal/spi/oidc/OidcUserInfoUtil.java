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
package com.ctrip.framework.apollo.portal.spi.oidc;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.spi.configuration.OidcExtendProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

public class OidcUserInfoUtil {

  private static final Logger log = LoggerFactory.getLogger(OidcUserInfoUtil.class);

  private OidcUserInfoUtil() {
    throw new UnsupportedOperationException("util class");
  }

  /**
   * resolve the Apollo user id from an oidc (interactive) user. When
   * {@link OidcExtendProperties#getUserIdClaimName()} is configured, the value of that claim is
   * used; otherwise, or when the configured claim is missing or blank, it falls back to the token
   * subject so a user is never created with an empty id.
   *
   * @param oidcUser             the user
   * @param oidcExtendProperties claimName properties
   * @return the Apollo user id
   */
  public static String getOidcUserId(OidcUser oidcUser, OidcExtendProperties oidcExtendProperties) {
    String userIdClaimName = oidcExtendProperties.getUserIdClaimName();
    String subject = oidcUser.getSubject();
    if (StringUtils.isBlank(userIdClaimName)) {
      return subject;
    }
    String userId = oidcUser.getClaimAsString(userIdClaimName);
    if (StringUtils.isBlank(userId)) {
      log.warn(
          "oidc user id claim [{}] is missing or blank, falling back to subject as the Apollo user id",
          userIdClaimName);
      return subject;
    }
    return userId;
  }

  /**
   * resolve the Apollo user id from a jwt. Behaves like
   * {@link #getOidcUserId(OidcUser, OidcExtendProperties)}, reading the same configured claim and
   * falling back to the jwt subject when it is missing or blank.
   *
   * @param jwt                  the user
   * @param oidcExtendProperties claimName properties
   * @return the Apollo user id
   */
  public static String getJwtUserId(Jwt jwt, OidcExtendProperties oidcExtendProperties) {
    String userIdClaimName = oidcExtendProperties.getUserIdClaimName();
    String subject = jwt.getSubject();
    if (StringUtils.isBlank(userIdClaimName)) {
      return subject;
    }
    String userId = jwt.getClaimAsString(userIdClaimName);
    if (StringUtils.isBlank(userId)) {
      log.warn(
          "jwt user id claim [{}] is missing or blank, falling back to subject as the Apollo user id",
          userIdClaimName);
      return subject;
    }
    return userId;
  }

  /**
   * get userDisplayName from oidcUser
   *
   * @param oidcUser             the user
   * @param oidcExtendProperties claimName properties
   * @return userDisplayName
   */
  public static String getOidcUserDisplayName(OidcUser oidcUser,
      OidcExtendProperties oidcExtendProperties) {
    String userDisplayNameClaimName = oidcExtendProperties.getUserDisplayNameClaimName();
    if (!StringUtils.isBlank(userDisplayNameClaimName)) {
      return oidcUser.getClaimAsString(userDisplayNameClaimName);
    }
    String preferredUsername = oidcUser.getPreferredUsername();
    if (!StringUtils.isBlank(preferredUsername)) {
      return preferredUsername;
    }
    return oidcUser.getFullName();
  }

  /**
   * get userDisplayName from jwt
   *
   * @param jwt                  the user
   * @param oidcExtendProperties claimName properties
   * @return userDisplayName
   */
  public static String getJwtUserDisplayName(Jwt jwt, OidcExtendProperties oidcExtendProperties) {
    String jwtUserDisplayNameClaimName = oidcExtendProperties.getJwtUserDisplayNameClaimName();
    if (!StringUtils.isBlank(jwtUserDisplayNameClaimName)) {
      return jwt.getClaimAsString(jwtUserDisplayNameClaimName);
    }
    return null;
  }
}
