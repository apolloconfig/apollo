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
package com.ctrip.framework.apollo.portal.auth;

import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

/**
 * Spring Security authentication for a successfully validated user access token.
 */
public class UserTokenAuthenticationToken extends AbstractAuthenticationToken {

  private final String userId;
  private final long tokenId;
  private final String tokenPrefix;

  private UserTokenAuthenticationToken(UserToken userToken,
      Collection<? extends GrantedAuthority> authorities) {
    super(authorities);
    this.userId = userToken.getUserId();
    this.tokenId = userToken.getId();
    this.tokenPrefix = userToken.getTokenPrefix();
  }

  public static UserTokenAuthenticationToken authenticated(UserToken userToken,
      Collection<? extends GrantedAuthority> authorities) {
    UserTokenAuthenticationToken authentication =
        new UserTokenAuthenticationToken(userToken, authorities);
    authentication.setAuthenticated(true);
    return authentication;
  }

  @Override
  public Object getCredentials() {
    return null;
  }

  @Override
  public Object getPrincipal() {
    return userId;
  }

  public long getTokenId() {
    return tokenId;
  }

  public String getTokenPrefix() {
    return tokenPrefix;
  }
}
