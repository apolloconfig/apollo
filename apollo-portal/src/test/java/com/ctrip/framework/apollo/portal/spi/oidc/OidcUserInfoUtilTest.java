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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.spi.configuration.OidcExtendProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Tests resolution of the Apollo user id from the configurable OIDC/JWT claim, covering the
 * unconfigured default (token subject), a configured-and-present claim, and the fallback to the
 * subject when the configured claim is missing or blank.
 */
public class OidcUserInfoUtilTest {

  private static final String SUBJECT = "8f3a2c1e-uuid-subject";

  private static Jwt jwtWith(String preferredUsername) {
    Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").subject(SUBJECT);
    if (preferredUsername != null) {
      builder.claim("preferred_username", preferredUsername);
    }
    return builder.build();
  }

  @Test
  public void testGetOidcUserIdDefaultsToSubjectWhenClaimNotConfigured() {
    OidcExtendProperties properties = new OidcExtendProperties();
    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getSubject()).thenReturn(SUBJECT);

    assertEquals(SUBJECT, OidcUserInfoUtil.getOidcUserId(oidcUser, properties));
  }

  @Test
  public void testGetOidcUserIdUsesConfiguredClaim() {
    OidcExtendProperties properties = new OidcExtendProperties();
    properties.setUserIdClaimName("preferred_username");
    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getSubject()).thenReturn(SUBJECT);
    when(oidcUser.getClaimAsString("preferred_username")).thenReturn("alice");

    assertEquals("alice", OidcUserInfoUtil.getOidcUserId(oidcUser, properties));
  }

  @Test
  public void testGetOidcUserIdFallsBackToSubjectWhenConfiguredClaimBlank() {
    OidcExtendProperties properties = new OidcExtendProperties();
    properties.setUserIdClaimName("preferred_username");
    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getSubject()).thenReturn(SUBJECT);
    when(oidcUser.getClaimAsString("preferred_username")).thenReturn(" ");

    assertEquals(SUBJECT, OidcUserInfoUtil.getOidcUserId(oidcUser, properties));
  }

  @Test
  public void testGetOidcUserIdFallsBackToSubjectWhenConfiguredClaimMissing() {
    OidcExtendProperties properties = new OidcExtendProperties();
    properties.setUserIdClaimName("preferred_username");
    OidcUser oidcUser = mock(OidcUser.class);
    when(oidcUser.getSubject()).thenReturn(SUBJECT);
    when(oidcUser.getClaimAsString("preferred_username")).thenReturn(null);

    assertEquals(SUBJECT, OidcUserInfoUtil.getOidcUserId(oidcUser, properties));
  }

  @Test
  public void testGetJwtUserIdDefaultsToSubjectWhenClaimNotConfigured() {
    OidcExtendProperties properties = new OidcExtendProperties();

    assertEquals(SUBJECT, OidcUserInfoUtil.getJwtUserId(jwtWith("alice"), properties));
  }

  @Test
  public void testGetJwtUserIdUsesConfiguredClaim() {
    OidcExtendProperties properties = new OidcExtendProperties();
    properties.setUserIdClaimName("preferred_username");

    assertEquals("alice", OidcUserInfoUtil.getJwtUserId(jwtWith("alice"), properties));
  }

  @Test
  public void testGetJwtUserIdFallsBackToSubjectWhenConfiguredClaimMissing() {
    OidcExtendProperties properties = new OidcExtendProperties();
    properties.setUserIdClaimName("preferred_username");

    assertEquals(SUBJECT, OidcUserInfoUtil.getJwtUserId(jwtWith(null), properties));
  }
}
