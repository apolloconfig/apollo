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
package com.ctrip.framework.apollo.portal.spi.configuration;

import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.oauth2.core.oidc.StandardClaimNames;

@ConfigurationProperties(prefix = "spring.security.oidc")
public class OidcExtendProperties {

  /**
   * claim name of the userDisplayName {@link UserPO#getUserDisplayName()}. default to
   * {@link StandardClaimNames#PREFERRED_USERNAME} or {@link StandardClaimNames#NAME}
   */
  private String userDisplayNameClaimName;

  /**
   * jwt claim name of the userDisplayName {@link UserPO#getUserDisplayName()}
   */
  private String jwtUserDisplayNameClaimName;

  public String getUserDisplayNameClaimName() {
    return userDisplayNameClaimName;
  }

  public void setUserDisplayNameClaimName(String userDisplayNameClaimName) {
    this.userDisplayNameClaimName = userDisplayNameClaimName;
  }

  public String getJwtUserDisplayNameClaimName() {
    return jwtUserDisplayNameClaimName;
  }

  public void setJwtUserDisplayNameClaimName(String jwtUserDisplayNameClaimName) {
    this.jwtUserDisplayNameClaimName = jwtUserDisplayNameClaimName;
  }
}
