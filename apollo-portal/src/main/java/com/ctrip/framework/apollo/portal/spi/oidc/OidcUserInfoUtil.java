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

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.spi.configuration.OidcExtendProperties;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

public class OidcUserInfoUtil {

  private OidcUserInfoUtil() {
    throw new UnsupportedOperationException("util class");
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
  public static String getJwtUserDisplayName(Jwt jwt,
      OidcExtendProperties oidcExtendProperties) {
    String jwtUserDisplayNameClaimName = oidcExtendProperties.getJwtUserDisplayNameClaimName();
    if (!StringUtils.isBlank(jwtUserDisplayNameClaimName)) {
      return jwt.getClaimAsString(jwtUserDisplayNameClaimName);
    }
    return null;
  }
}
