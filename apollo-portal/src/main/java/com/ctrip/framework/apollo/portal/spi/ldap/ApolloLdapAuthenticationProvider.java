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


package com.ctrip.framework.apollo.portal.spi.ldap;

import com.ctrip.framework.apollo.portal.spi.configuration.LdapExtendProperties;
import org.springframework.ldap.core.DirContextOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.ldap.authentication.LdapAuthenticationProvider;
import org.springframework.security.ldap.authentication.LdapAuthenticator;
import org.springframework.security.ldap.userdetails.LdapAuthoritiesPopulator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Inherited from LdapAuthenticationProvider and rewritten the authenticate method,
 * modified the userId used by the previous user input,
 * changed to use the userId in the LDAP system.
 *
 * @author wuzishu
 */
public class ApolloLdapAuthenticationProvider extends LdapAuthenticationProvider {

  private LdapExtendProperties properties;

  public ApolloLdapAuthenticationProvider(
      LdapAuthenticator authenticator,
      LdapAuthoritiesPopulator authoritiesPopulator) {
    super(authenticator, authoritiesPopulator);
  }

  public ApolloLdapAuthenticationProvider(
      LdapAuthenticator authenticator) {
    super(authenticator);
  }

  public ApolloLdapAuthenticationProvider(
      LdapAuthenticator authenticator,
      LdapAuthoritiesPopulator authoritiesPopulator,
      LdapExtendProperties properties) {
    super(authenticator, authoritiesPopulator);
    this.properties = properties;
  }

  public ApolloLdapAuthenticationProvider(
      LdapAuthenticator authenticator,
      LdapExtendProperties properties) {
    super(authenticator);
    this.properties = properties;
  }

  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication, this.messages
        .getMessage("LdapAuthenticationProvider.onlySupports",
            "Only UsernamePasswordAuthenticationToken is supported"));
    UsernamePasswordAuthenticationToken userToken = (UsernamePasswordAuthenticationToken) authentication;
    String username = userToken.getName();
    String password = (String) authentication.getCredentials();
    if (this.logger.isDebugEnabled()) {
      this.logger.debug("Processing authentication request for user: " + username);
    }

    if (!StringUtils.hasLength(username)) {
      throw new BadCredentialsException(
          this.messages.getMessage("LdapAuthenticationProvider.emptyUsername", "Empty Username"));
    }
    if (!StringUtils.hasLength(password)) {
      throw new BadCredentialsException(this.messages
          .getMessage("AbstractLdapAuthenticationProvider.emptyPassword", "Empty Password"));
    }
    Assert.notNull(password, "Null password was supplied in authentication token");
    DirContextOperations userData = this.doAuthentication(userToken);
    String loginId = userData.getStringAttribute(properties.getMapping().getLoginId());
    UserDetails user = this.userDetailsContextMapper.mapUserFromContext(userData, loginId,
        this.loadUserAuthorities(userData, loginId, (String) authentication.getCredentials()));
    return this.createSuccessfulAuthentication(userToken, user);
  }
}
