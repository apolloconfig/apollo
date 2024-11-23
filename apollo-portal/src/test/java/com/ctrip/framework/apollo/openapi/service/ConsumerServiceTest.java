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
package com.ctrip.framework.apollo.openapi.service;

import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.repository.ConsumerAuditRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerTokenRepository;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import org.junit.jupiter.api.BeforeEach;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ConsumerService.class)
public class ConsumerServiceTest {
  @SpyBean
  private ConsumerService consumerService;
  @MockBean
  UserInfoHolder userInfoHolder;
  @MockBean
  ConsumerTokenRepository consumerTokenRepository;
  @MockBean
  ConsumerRepository consumerRepository;
  @MockBean
  ConsumerAuditRepository consumerAuditRepository;
  @MockBean
  ConsumerRoleRepository consumerRoleRepository;
  @MockBean
  PortalConfig portalConfig;
  @MockBean
  RolePermissionService rolePermissionService;
  @MockBean
  UserService userService;
  @MockBean
  RoleRepository roleRepository;

  private final String someTokenSalt = "someTokenSalt";
  private final String testAppId = "testAppId";
  private final String testConsumerName = "testConsumerName";
  private final String testOwner = "testOwner";

  @BeforeEach
  public void setUp() throws Exception {
    when(portalConfig.consumerTokenSalt()).thenReturn(someTokenSalt);
  }

  @Test
  public void testGetConsumerId() throws Exception {
    String someToken = "someToken";
    long someConsumerId = 1;
    ConsumerToken someConsumerToken = new ConsumerToken();
    someConsumerToken.setConsumerId(someConsumerId);

    when(consumerTokenRepository.findTopByTokenAndExpiresAfter(eq(someToken), any(Date.class)))
        .thenReturn(someConsumerToken);

    assertEquals(someConsumerId, consumerService.getConsumerIdByToken(someToken).longValue());
  }

  @Test
  public void testGetConsumerIdWithNullToken() throws Exception {
    Long consumerId = consumerService.getConsumerIdByToken(null);

    assertNull(consumerId);
    verify(consumerTokenRepository, never()).findTopByTokenAndExpiresAfter(anyString(), any(Date
                                                                                                .class));
  }

  @Test
  public void testGetConsumerByConsumerId() throws Exception {
    long someConsumerId = 1;
    Consumer someConsumer = mock(Consumer.class);

    when(consumerRepository.findById(someConsumerId)).thenReturn(Optional.of(someConsumer));

    assertEquals(someConsumer, consumerService.getConsumerByConsumerId(someConsumerId));
    verify(consumerRepository, times(1)).findById(someConsumerId);
  }

  @Test
  public void testCreateConsumerToken() throws Exception {
    ConsumerToken someConsumerToken = mock(ConsumerToken.class);
    ConsumerToken savedConsumerToken = mock(ConsumerToken.class);

    when(consumerTokenRepository.save(someConsumerToken)).thenReturn(savedConsumerToken);

    assertEquals(savedConsumerToken, consumerService.createConsumerToken(someConsumerToken));
  }

  @Test
  public void testGenerateConsumerToken() throws Exception {
    String someConsumerAppId = "100003171";
    Date generationTime = new GregorianCalendar(2016, Calendar.AUGUST, 9, 12, 10, 50).getTime();
    String tokenSalt = "apollo";
    String expectedToken = "151067a53d08d70de161fa06b455623741877ce2f019f6e3018844c1a16dd8c6";

    String actualToken = consumerService.generateToken(someConsumerAppId, generationTime, tokenSalt);

    assertEquals(expectedToken, actualToken);
  }

  @Test
  public void testGenerateAndEnrichConsumerToken() throws Exception {
    String someConsumerAppId = "someAppId";
    long someConsumerId = 1;
    String someToken = "someToken";
    Date generationTime = new Date();
    Consumer consumer = mock(Consumer.class);

    when(consumerRepository.findById(someConsumerId)).thenReturn(Optional.of(consumer));
    when(consumer.getAppId()).thenReturn(someConsumerAppId);
    when(consumerService.generateToken(someConsumerAppId, generationTime, someTokenSalt))
        .thenReturn(someToken);

    ConsumerToken consumerToken = new ConsumerToken();
    consumerToken.setConsumerId(someConsumerId);
    consumerToken.setDataChangeCreatedTime(generationTime);

    consumerService.generateAndEnrichToken(consumer, consumerToken);

    assertEquals(someToken, consumerToken.getToken());
  }

  @Test
  public void testGenerateAndEnrichConsumerTokenWithConsumerNotFound() throws Exception {
    long someConsumerIdNotExist = 1;

    ConsumerToken consumerToken = new ConsumerToken();
    consumerToken.setConsumerId(someConsumerIdNotExist);

    assertThrows(IllegalArgumentException.class,
        () -> consumerService.generateAndEnrichToken(null, consumerToken)
    );
  }

  @Test
  public void testCreateConsumer() {
    Consumer consumer = createConsumer(testConsumerName, testAppId, testOwner);
    UserInfo owner = createUser(testOwner);

    when(consumerRepository.findByAppId(testAppId)).thenReturn(null);
    when(userService.findByUserId(testOwner)).thenReturn(owner);
    when(userInfoHolder.getUser()).thenReturn(owner);

    consumerService.createConsumer(consumer);

    verify(consumerRepository).save(consumer);
  }

  @Test
  public void testAssignNamespaceRoleToConsumer() {
    long consumerId = 1L;
    String token = "token";

    doReturn(consumerId).when(consumerService).getConsumerIdByToken(token);

    String testNamespace = "namespace";
    String modifyRoleName = RoleUtils.buildModifyNamespaceRoleName(testAppId, testNamespace);
    String releaseRoleName = RoleUtils.buildReleaseNamespaceRoleName(testAppId, testNamespace);
    String envModifyRoleName = RoleUtils.buildModifyNamespaceRoleName(testAppId, testNamespace, Env.DEV.toString());
    String envReleaseRoleName = RoleUtils.buildReleaseNamespaceRoleName(testAppId, testNamespace, Env.DEV.toString());
    long modifyRoleId = 1;
    long releaseRoleId = 2;
    long envModifyRoleId = 3;
    long envReleaseRoleId = 4;
    Role modifyRole = createRole(modifyRoleId, modifyRoleName);
    Role releaseRole = createRole(releaseRoleId, releaseRoleName);
    Role envModifyRole = createRole(envModifyRoleId, modifyRoleName);
    Role envReleaseRole = createRole(envReleaseRoleId, releaseRoleName);
    when(rolePermissionService.findRoleByRoleName(modifyRoleName)).thenReturn(modifyRole);
    when(rolePermissionService.findRoleByRoleName(releaseRoleName)).thenReturn(releaseRole);
    when(rolePermissionService.findRoleByRoleName(envModifyRoleName)).thenReturn(envModifyRole);
    when(rolePermissionService.findRoleByRoleName(envReleaseRoleName)).thenReturn(envReleaseRole);

    when(consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, modifyRoleId)).thenReturn(null);

    UserInfo owner = createUser(testOwner);
    when(userInfoHolder.getUser()).thenReturn(owner);

    ConsumerRole namespaceModifyConsumerRole = createConsumerRole(consumerId, modifyRoleId);
    ConsumerRole namespaceEnvModifyConsumerRole = createConsumerRole(consumerId, envModifyRoleId);
    ConsumerRole namespaceReleaseConsumerRole = createConsumerRole(consumerId, releaseRoleId);
    ConsumerRole namespaceEnvReleaseConsumerRole = createConsumerRole(consumerId, envReleaseRoleId);
    doReturn(namespaceModifyConsumerRole).when(consumerService).createConsumerRole(consumerId, modifyRoleId, testOwner);
    doReturn(namespaceEnvModifyConsumerRole).when(consumerService).createConsumerRole(consumerId, envModifyRoleId, testOwner);
    doReturn(namespaceReleaseConsumerRole).when(consumerService).createConsumerRole(consumerId, releaseRoleId, testOwner);
    doReturn(namespaceEnvReleaseConsumerRole).when(consumerService).createConsumerRole(consumerId, envReleaseRoleId, testOwner);

    consumerService.assignNamespaceRoleToConsumer(token, testAppId, testNamespace);
    consumerService.assignNamespaceRoleToConsumer(token, testAppId, testNamespace, Env.DEV.toString());

    verify(consumerRoleRepository).save(namespaceModifyConsumerRole);
    verify(consumerRoleRepository).save(namespaceEnvModifyConsumerRole);
    verify(consumerRoleRepository).save(namespaceReleaseConsumerRole);
    verify(consumerRoleRepository).save(namespaceEnvReleaseConsumerRole);


  }

  @Test
  void notAllowCreateApplication() {
    final String appId = "appId-consumer-2023";
    final String token = "token-2023";
    final long consumerId = 2023;
    final long roleId = 202309;

    {
      Consumer consumer = new Consumer();
      consumer.setAppId(appId);
      consumer.setId(consumerId);
      when(consumerRepository.findByAppId(eq(appId)))
          .thenReturn(consumer);

      ConsumerToken consumerToken = new ConsumerToken();
      consumerToken.setToken(token);
      consumerToken.setRateLimit(0);
      when(consumerTokenRepository.findByConsumerId(eq(consumerId)))
          .thenReturn(consumerToken);
    }
    ConsumerInfo consumerInfo = consumerService.getConsumerInfoByAppId(appId);
    assertFalse(consumerInfo.isAllowCreateApplication());
    assertEquals(appId, consumerInfo.getAppId());
    assertEquals(token, consumerInfo.getToken());
  }

  @Test
  void allowCreateApplication() {
    final String appId = "appId-consumer-2023";
    final String token = "token-2023";
    final long consumerId = 2023;
    final long roleId = 202309;

    {
      Consumer consumer = new Consumer();
      consumer.setAppId(appId);
      consumer.setId(consumerId);
      when(consumerRepository.findByAppId(eq(appId)))
          .thenReturn(consumer);

      ConsumerToken consumerToken = new ConsumerToken();
      consumerToken.setToken(token);
      consumerToken.setRateLimit(0);
      when(consumerTokenRepository.findByConsumerId(eq(consumerId)))
          .thenReturn(consumerToken);
    }

    {
      Role role = new Role();
      role.setId(roleId);
      when(rolePermissionService.findRoleByRoleName(any()))
          .thenReturn(role);

      ConsumerRole consumerRole = new ConsumerRole();
      consumerRole.setConsumerId(consumerId);
      when(consumerRoleRepository.findByConsumerIdAndRoleId(eq(consumerId), eq(roleId)))
          .thenReturn(consumerRole);
    }

    ConsumerInfo consumerInfo = consumerService.getConsumerInfoByAppId(appId);
    assertTrue(consumerInfo.isAllowCreateApplication());
    assertEquals(appId, consumerInfo.getAppId());
    assertEquals(token, consumerInfo.getToken());
    assertEquals(consumerId, consumerInfo.getConsumerId());
  }

  private Consumer createConsumer(String name, String appId, String ownerName) {
    Consumer consumer = new Consumer();

    consumer.setName(name);
    consumer.setAppId(appId);
    consumer.setOwnerName(ownerName);

    return consumer;
  }

  private Role createRole(long roleId, String roleName) {
    Role role = new Role();
    role.setId(roleId);
    role.setRoleName(roleName);
    return role;
  }

  private ConsumerRole createConsumerRole(long consumerId, long roleId) {
    ConsumerRole consumerRole = new ConsumerRole();
    consumerRole.setConsumerId(consumerId);
    consumerRole.setRoleId(roleId);
    return consumerRole;
  }

  private UserInfo createUser(String userId) {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId(userId);
    return userInfo;
  }
}
