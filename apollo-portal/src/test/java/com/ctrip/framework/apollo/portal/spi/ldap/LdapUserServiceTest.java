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
package com.ctrip.framework.apollo.portal.spi.ldap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.ContextMapper;
import org.springframework.ldap.core.DirContextAdapter;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class LdapUserServiceTest {

  @Mock
  private LdapTemplate ldapTemplate;

  private LdapUserService ldapUserService;

  @BeforeEach
  void setUp() {
    ldapUserService = new LdapUserService(ldapTemplate);
    ReflectionTestUtils.setField(ldapUserService, "objectClassAttrName", "person");
    ReflectionTestUtils.setField(ldapUserService, "loginIdAttrName", "uid");
    ReflectionTestUtils.setField(ldapUserService, "userDisplayNameAttrName", "cn");
    ReflectionTestUtils.setField(ldapUserService, "emailAttrName", "mail");
    ReflectionTestUtils.setField(ldapUserService, "accountStatusAttrName", "");
    ReflectionTestUtils.setField(ldapUserService, "memberOf", new String[] {});
    ReflectionTestUtils.setField(ldapUserService, "groupSearch", "");
  }

  @Test
  void findByUserIdReturnsEnabledLdapUser() {
    when(ldapTemplate.searchForObject(any(LdapQuery.class), any(ContextMapper.class)))
        .thenAnswer(invocation -> {
          ContextMapper<UserInfo> mapper = invocation.getArgument(1);
          DirContextAdapter context = new DirContextAdapter();
          context.setAttributeValue("uid", "apollo");
          context.setAttributeValue("cn", "Apollo");
          context.setAttributeValue("mail", "apollo@example.com");
          return mapper.mapFromContext(context);
        });

    UserInfo userInfo = ldapUserService.findByUserId("apollo");

    assertNotNull(userInfo);
    assertEquals("apollo", userInfo.getUserId());
    assertEquals(1, userInfo.getEnabled());
  }

  @Test
  void findByUserIdMapsNsAccountLockToDisabledUser() {
    ReflectionTestUtils.setField(ldapUserService, "accountStatusAttrName", "nsAccountLock");
    when(ldapTemplate.searchForObject(any(LdapQuery.class), any(ContextMapper.class)))
        .thenAnswer(invocation -> {
          ContextMapper<UserInfo> mapper = invocation.getArgument(1);
          DirContextAdapter context = userContext();
          context.setAttributeValue("nsAccountLock", "true");
          return mapper.mapFromContext(context);
        });

    UserInfo userInfo = ldapUserService.findByUserId("apollo");

    assertNotNull(userInfo);
    assertEquals(0, userInfo.getEnabled());
  }

  @Test
  void findByUserIdMapsActiveDirectoryDisabledFlag() {
    ReflectionTestUtils.setField(ldapUserService, "accountStatusAttrName", "userAccountControl");
    when(ldapTemplate.searchForObject(any(LdapQuery.class), any(ContextMapper.class)))
        .thenAnswer(invocation -> {
          ContextMapper<UserInfo> mapper = invocation.getArgument(1);
          DirContextAdapter context = userContext();
          context.setAttributeValue("userAccountControl", "514");
          return mapper.mapFromContext(context);
        });

    UserInfo userInfo = ldapUserService.findByUserId("apollo");

    assertNotNull(userInfo);
    assertEquals(0, userInfo.getEnabled());
  }

  private DirContextAdapter userContext() {
    DirContextAdapter context = new DirContextAdapter();
    context.setAttributeValue("uid", "apollo");
    context.setAttributeValue("cn", "Apollo");
    context.setAttributeValue("mail", "apollo@example.com");
    return context;
  }
}
