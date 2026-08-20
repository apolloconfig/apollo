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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

public class OidcUserInfoUtil {

  private OidcUserInfoUtil() {
    throw new UnsupportedOperationException("util class");
  }

  /**
   * resolve the Apollo user id from an oidc (interactive) user. When
   * {@link OidcExtendProperties#getUserIdClaimName()} is configured, the value of that claim is
   * used as the Apollo login identity. If a claim is configured but missing or blank in the token,
   * login is rejected rather than falling back to the subject: falling back could resolve the same
   * principal to a different id across the oidc and jwt paths and provision two local accounts. When
   * no claim is configured, the token subject is used (default behavior).
   *
   * @param oidcUser             the user
   * @param oidcExtendProperties claimName properties
   * @return the Apollo user id
   * @throws BadCredentialsException if a claim is configured but absent or blank in the token
   */
  public static String getOidcUserId(OidcUser oidcUser, OidcExtendProperties oidcExtendProperties) {
    String userIdClaimName = oidcExtendProperties.getUserIdClaimName();
    if (StringUtils.isBlank(userIdClaimName)) {
      return oidcUser.getSubject();
    }
    String userId = oidcUser.getClaimAsString(userIdClaimName);
    if (StringUtils.isBlank(userId)) {
      throw new BadCredentialsException(
          String.format("the configured oidc user id claim [%s] is missing or blank in the token",
              userIdClaimName));
    }
    return userId;
  }

  /**
   * resolve the Apollo user id from a jwt. Behaves like
   * {@link #getOidcUserId(OidcUser, OidcExtendProperties)}, reading the same configured claim,
   * using the jwt subject only when no claim is configured, and rejecting the login when a
   * configured claim is missing or blank.
   *
   * @param jwt                  the user
   * @param oidcExtendProperties claimName properties
   * @return the Apollo user id
   * @throws BadCredentialsException if a claim is configured but absent or blank in the token
   */
  public static String getJwtUserId(Jwt jwt, OidcExtendProperties oidcExtendProperties) {
    String userIdClaimName = oidcExtendProperties.getUserIdClaimName();
    if (StringUtils.isBlank(userIdClaimName)) {
      return jwt.getSubject();
    }
    String userId = jwt.getClaimAsString(userIdClaimName);
    if (StringUtils.isBlank(userId)) {
      throw new BadCredentialsException(
          String.format("the configured jwt user id claim [%s] is missing or blank in the token",
              userIdClaimName));
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
