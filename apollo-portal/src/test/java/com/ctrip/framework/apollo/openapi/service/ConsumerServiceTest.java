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
package com.ctrip.framework.apollo.openapi.service;

import static com.ctrip.framework.apollo.portal.service.SystemRoleManagerService.CREATE_APPLICATION_ROLE_NAME;
import static com.ctrip.framework.apollo.portal.service.SystemRoleManagerService.MANAGE_USERS_ROLE_NAME;
import com.ctrip.framework.apollo.openapi.model.OpenConsumerRoleDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
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
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.ContextConfiguration;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(classes = ConsumerService.class)
public class ConsumerServiceTest {
  @MockitoSpyBean
  private ConsumerService consumerService;
  @MockitoBean
  ConsumerTokenRepository consumerTokenRepository;
  @MockitoBean
  ConsumerRepository consumerRepository;
  @MockitoBean
  ConsumerAuditRepository consumerAuditRepository;
  @MockitoBean
  ConsumerRoleRepository consumerRoleRepository;
  @MockitoBean
  PortalConfig portalConfig;
  @MockitoBean
  RolePermissionService rolePermissionService;
  @MockitoBean
  UserService userService;
  @MockitoBean
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
    verify(consumerTokenRepository, never()).findTopByTokenAndExpiresAfter(anyString(),
        any(Date.class));
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

    String actualToken =
        consumerService.generateToken(someConsumerAppId, generationTime, tokenSalt);

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
        () -> consumerService.generateAndEnrichToken(null, consumerToken));
  }

  @Test
  public void testCreateConsumer() {
    Consumer consumer = createConsumer(testConsumerName, testAppId, testOwner);
    UserInfo owner = createUser(testOwner);

    when(consumerRepository.findByAppId(testAppId)).thenReturn(null);
    when(userService.findByUserId(testOwner)).thenReturn(owner);
    consumerService.createConsumer(consumer, testOwner);

    verify(consumerRepository).save(consumer);
  }

  @Test
  public void writeMethodsShouldRejectBlankOperator() {
    Consumer consumer = createConsumer(testConsumerName, testAppId, testOwner);

    assertThrows(BadRequestException.class, () -> consumerService.createConsumer(consumer, " "));
    assertThrows(BadRequestException.class,
        () -> consumerService.generateAndSaveConsumerToken(consumer, 0, new Date(), " "));
    assertThrows(BadRequestException.class,
        () -> consumerService.assignNamespaceRoleToConsumer("token", testAppId, "namespace", " "));
    assertThrows(BadRequestException.class,
        () -> consumerService.assignCreateApplicationRoleToConsumer("token", " "));
    assertThrows(BadRequestException.class,
        () -> consumerService.assignManageUsersRoleToConsumer("token", " "));
    assertThrows(BadRequestException.class,
        () -> consumerService.assignAppRoleToConsumer("token", testAppId, " "));
    assertThrows(BadRequestException.class,
        () -> consumerService.assignAppRoleToConsumer(1L, testAppId, " "));
    assertThrows(BadRequestException.class, () -> consumerService.createConsumerRole(1L, 2L, " "));
  }

  @Test
  public void testAssignNamespaceRoleToConsumer() {
    long consumerId = 1L;
    String token = "token";

    doReturn(consumerId).when(consumerService).getConsumerIdByToken(token);

    String testNamespace = "namespace";
    String modifyRoleName = RoleUtils.buildModifyNamespaceRoleName(testAppId, testNamespace);
    String releaseRoleName = RoleUtils.buildReleaseNamespaceRoleName(testAppId, testNamespace);
    String envModifyRoleName =
        RoleUtils.buildModifyNamespaceRoleName(testAppId, testNamespace, Env.DEV.toString());
    String envReleaseRoleName =
        RoleUtils.buildReleaseNamespaceRoleName(testAppId, testNamespace, Env.DEV.toString());
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

    when(consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, modifyRoleId))
        .thenReturn(null);

    ConsumerRole namespaceModifyConsumerRole = createConsumerRole(consumerId, modifyRoleId);
    ConsumerRole namespaceEnvModifyConsumerRole = createConsumerRole(consumerId, envModifyRoleId);
    ConsumerRole namespaceReleaseConsumerRole = createConsumerRole(consumerId, releaseRoleId);
    ConsumerRole namespaceEnvReleaseConsumerRole = createConsumerRole(consumerId, envReleaseRoleId);
    doReturn(namespaceModifyConsumerRole).when(consumerService).createConsumerRole(consumerId,
        modifyRoleId, testOwner);
    doReturn(namespaceEnvModifyConsumerRole).when(consumerService).createConsumerRole(consumerId,
        envModifyRoleId, testOwner);
    doReturn(namespaceReleaseConsumerRole).when(consumerService).createConsumerRole(consumerId,
        releaseRoleId, testOwner);
    doReturn(namespaceEnvReleaseConsumerRole).when(consumerService).createConsumerRole(consumerId,
        envReleaseRoleId, testOwner);

    consumerService.assignNamespaceRoleToConsumer(token, testAppId, testNamespace, testOwner);
    consumerService.assignNamespaceRoleToConsumer(token, testAppId, testNamespace,
        Env.DEV.toString(), testOwner);

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
      when(consumerRepository.findByAppId(eq(appId))).thenReturn(consumer);

      ConsumerToken consumerToken = new ConsumerToken();
      consumerToken.setToken(token);
      consumerToken.setRateLimit(0);
      when(consumerTokenRepository.findByConsumerId(eq(consumerId))).thenReturn(consumerToken);
    }
    ConsumerInfo consumerInfo = consumerService.getConsumerInfoByAppId(appId);
    assertFalse(consumerInfo.isAllowCreateApplication());
    assertFalse(consumerInfo.isAllowManageUsers());
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
      when(consumerRepository.findByAppId(eq(appId))).thenReturn(consumer);

      ConsumerToken consumerToken = new ConsumerToken();
      consumerToken.setToken(token);
      consumerToken.setRateLimit(0);
      when(consumerTokenRepository.findByConsumerId(eq(consumerId))).thenReturn(consumerToken);
    }

    {
      Role role = new Role();
      role.setId(roleId);
      when(rolePermissionService.findRoleByRoleName(CREATE_APPLICATION_ROLE_NAME)).thenReturn(role);

      ConsumerRole consumerRole = new ConsumerRole();
      consumerRole.setConsumerId(consumerId);
      when(consumerRoleRepository
          .findByConsumerIdInAndRoleId(eq(Collections.singletonList(consumerId)), eq(roleId)))
          .thenReturn(Collections.singletonList(consumerRole));
    }

    ConsumerInfo consumerInfo = consumerService.getConsumerInfoByAppId(appId);
    assertTrue(consumerInfo.isAllowCreateApplication());
    assertFalse(consumerInfo.isAllowManageUsers());
    assertEquals(appId, consumerInfo.getAppId());
    assertEquals(token, consumerInfo.getToken());
    assertEquals(consumerId, consumerInfo.getConsumerId());
  }

  @Test
  void allowManageUsers() {
    final String appId = "appId-consumer-manage-users";
    final String token = "token-manage-users";
    final long consumerId = 2024;
    final long roleId = 202406;

    {
      Consumer consumer = new Consumer();
      consumer.setAppId(appId);
      consumer.setId(consumerId);
      when(consumerRepository.findByAppId(eq(appId))).thenReturn(consumer);

      ConsumerToken consumerToken = new ConsumerToken();
      consumerToken.setToken(token);
      consumerToken.setRateLimit(0);
      when(consumerTokenRepository.findByConsumerId(eq(consumerId))).thenReturn(consumerToken);
    }

    {
      Role role = new Role();
      role.setId(roleId);
      when(rolePermissionService.findRoleByRoleName(MANAGE_USERS_ROLE_NAME)).thenReturn(role);

      ConsumerRole consumerRole = new ConsumerRole();
      consumerRole.setConsumerId(consumerId);
      when(consumerRoleRepository
          .findByConsumerIdInAndRoleId(eq(Collections.singletonList(consumerId)), eq(roleId)))
          .thenReturn(Collections.singletonList(consumerRole));
    }

    ConsumerInfo consumerInfo = consumerService.getConsumerInfoByAppId(appId);
    assertFalse(consumerInfo.isAllowCreateApplication());
    assertTrue(consumerInfo.isAllowManageUsers());
    assertEquals(appId, consumerInfo.getAppId());
    assertEquals(token, consumerInfo.getToken());
    assertEquals(consumerId, consumerInfo.getConsumerId());
  }

  @Test
  public void testAssignManageUsersRoleToConsumer() {
    long consumerId = 1L;
    long roleId = 2L;
    String token = "token";
    doReturn(consumerId).when(consumerService).getConsumerIdByToken(token);
    Role role = createRole(roleId, MANAGE_USERS_ROLE_NAME);
    when(rolePermissionService.findRoleByRoleName(MANAGE_USERS_ROLE_NAME)).thenReturn(role);
    ConsumerRole consumerRole = createConsumerRole(consumerId, roleId);
    doReturn(consumerRole).when(consumerService).createConsumerRole(consumerId, roleId, testOwner);
    when(consumerRoleRepository.save(consumerRole)).thenReturn(consumerRole);

    ConsumerRole created = consumerService.assignManageUsersRoleToConsumer(token, testOwner);

    assertEquals(consumerRole, created);
    verify(consumerRoleRepository).save(consumerRole);
  }

  @ParameterizedTest
  @CsvSource(
      value = {"Master+testAppId, AppRole, NULL, NULL",
          "ModifyNamespace+testAppId+application, NamespaceRole, application, NULL",
          "ReleaseNamespace+testAppId+application, NamespaceRole, application, NULL",
          "ModifyNamespace+testAppId+application+LOCAL, NamespaceRole, application, LOCAL",
          "ReleaseNamespace+testAppId+application+LOCAL, NamespaceRole, application, LOCAL",
          "ModifyNamespace+testAppId+apollo.xml+DEV, NamespaceRole, apollo.xml, DEV",
          "ReleaseNamespace+testAppId+apollo.yaml+DEV, NamespaceRole, apollo.yaml, DEV"},
      nullValues = "NULL")
  void testFindConsumerRoleDetailsByConsumerId(String roleName, String type, String namespaceName,
      String env) {
    mockConsumerRoleDetails(roleName);

    List<OpenConsumerRoleDTO> result = consumerService.findConsumerRoleDetailsByConsumerId(1L);

    assertEquals(1, result.size());
    OpenConsumerRoleDTO dto = result.get(0);
    assertEquals(type, dto.getType());
    assertEquals(testAppId, dto.getAppId());
    assertEquals(namespaceName, dto.getNamespaceName());
    assertEquals(env, dto.getEnv());
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "CreateApplication+SystemRole", "ManageUsers+SystemRole",
      "ManageAppMaster+testAppId", "ModifyNamespacesInCluster+testAppId+LOCAL+default",
      "ReleaseNamespacesInCluster+testAppId+LOCAL+default", "Unknown+testAppId", "Master",
      "Master+", "Master++testAppId", "Master+testAppId+extra", "ModifyNamespace+testAppId",
      "ModifyNamespace++application", "ModifyNamespace+testAppId+",
      "ModifyNamespace+testAppId++LOCAL", "ModifyNamespace+testAppId+application+",
      "ModifyNamespace+testAppId+application+LOCAL+extra",
      "ReleaseNamespace+testAppId+application+ ", "ReleaseNamespace+testAppId+ +LOCAL"})
  void testFindConsumerRoleDetailsSkipsUnsupportedOrMalformedRoles(String roleName) {
    mockConsumerRoleDetails(roleName);

    assertTrue(consumerService.findConsumerRoleDetailsByConsumerId(1L).isEmpty());
  }

  @Test
  void testFindConsumerRoleDetailsAggregatesNamespaceRolesAndKeepsScopesSeparate() {
    mockConsumerRoleDetails("Master+testAppId", "ModifyNamespace+testAppId+application",
        "ReleaseNamespace+testAppId+application", "ModifyNamespace+testAppId+application+DEV",
        "ReleaseNamespace+testAppId+application+DEV", "ReleaseNamespace+testAppId+application+FAT",
        CREATE_APPLICATION_ROLE_NAME, MANAGE_USERS_ROLE_NAME);

    List<OpenConsumerRoleDTO> result = consumerService.findConsumerRoleDetailsByConsumerId(1L);

    assertEquals(4, result.size());
    assertTrue(result.contains(
        new OpenConsumerRoleDTO().type("AppRole").appId(testAppId).namespaceName(null).env(null)));
    for (String env : Arrays.asList(null, "DEV", "FAT")) {
      assertTrue(result.contains(new OpenConsumerRoleDTO().type("NamespaceRole").appId(testAppId)
          .namespaceName("application").env(env)));
    }
  }

  @Test
  void testFindConsumerRoleDetailsWithoutBindings() {
    when(consumerRepository.existsById(1L)).thenReturn(true);
    when(consumerRoleRepository.findByConsumerId(1L)).thenReturn(Collections.emptyList());

    assertTrue(consumerService.findConsumerRoleDetailsByConsumerId(1L).isEmpty());
    verifyNoInteractions(roleRepository);
  }

  @Test
  void testFindConsumerRoleDetailsSkipsDeletedRoles() {
    when(consumerRepository.existsById(1L)).thenReturn(true);
    when(consumerRoleRepository.findByConsumerId(1L))
        .thenReturn(Collections.singletonList(createConsumerRole(1L, 2L)));
    Role role = createRole(2L, "Master+testAppId");
    role.setDeleted(true);
    when(roleRepository.findAllById(Collections.singletonList(2L)))
        .thenReturn(Collections.singletonList(role));

    assertTrue(consumerService.findConsumerRoleDetailsByConsumerId(1L).isEmpty());
  }

  @Test
  void testConsumerRoleManagementRejectsMissingConsumer() {
    assertThrows(NotFoundException.class,
        () -> consumerService.findConsumerRoleDetailsByConsumerId(1L));
    assertThrows(NotFoundException.class,
        () -> consumerService.revokeConsumerRole(1L, "AppRole", testAppId, null, null, testOwner));
    verifyNoInteractions(consumerRoleRepository, roleRepository, rolePermissionService);
  }

  @Test
  void testConsumerRoleManagementRejectsInvalidConsumerId() {
    assertThrows(BadRequestException.class,
        () -> consumerService.findConsumerRoleDetailsByConsumerId(0L));
    assertThrows(BadRequestException.class,
        () -> consumerService.revokeConsumerRole(-1L, "AppRole", testAppId, null, null, testOwner));
    verifyNoInteractions(consumerRepository, consumerRoleRepository);
  }

  @Test
  void testRevokeConsumerAppRole() {
    when(consumerRepository.existsById(1L)).thenReturn(true);
    when(rolePermissionService.findRoleByRoleName("Master+testAppId"))
        .thenReturn(createRole(2L, "Master+testAppId"));

    consumerService.revokeConsumerRole(1L, "AppRole", testAppId, null, null, testOwner);

    verify(consumerRoleRepository)
        .batchDeleteByConsumerIdAndRoleIds(1L, Collections.singletonList(2L), testOwner);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"DEV", "dev", "LOCAL", "local"})
  void testRevokeConsumerNamespaceRoles(String env) {
    when(consumerRepository.existsById(1L)).thenReturn(true);
    String normalizedEnv = env == null ? null : Env.transformEnv(env).getName();
    String modifyRoleName = RoleUtils.buildModifyNamespaceRoleName(testAppId, "application",
        normalizedEnv);
    String releaseRoleName = RoleUtils.buildReleaseNamespaceRoleName(testAppId, "application",
        normalizedEnv);
    when(rolePermissionService.findRoleByRoleName(modifyRoleName))
        .thenReturn(createRole(2L, modifyRoleName));
    when(rolePermissionService.findRoleByRoleName(releaseRoleName))
        .thenReturn(createRole(3L, releaseRoleName));

    consumerService.revokeConsumerRole(1L, "NamespaceRole", testAppId, "application", env,
        testOwner);

    verify(consumerRoleRepository)
        .batchDeleteByConsumerIdAndRoleIds(1L, Arrays.asList(2L, 3L), testOwner);
  }

  @Test
  void testRevokeConsumerNamespaceRoleWithOnlyOneExistingRole() {
    when(consumerRepository.existsById(1L)).thenReturn(true);
    String roleName = RoleUtils.buildReleaseNamespaceRoleName(testAppId, "application");
    when(rolePermissionService.findRoleByRoleName(roleName)).thenReturn(createRole(3L, roleName));

    consumerService.revokeConsumerRole(1L, "NamespaceRole", testAppId, "application", null,
        testOwner);

    verify(consumerRoleRepository)
        .batchDeleteByConsumerIdAndRoleIds(1L, Collections.singletonList(3L), testOwner);
  }

  @ParameterizedTest
  @CsvSource(value = {
      "InvalidRole, testAppId, application, NULL",
      "AppRole, NULL, NULL, NULL",
      "NamespaceRole, testAppId, NULL, NULL",
      "NamespaceRole, testAppId, application, invalid-env",
      "NamespaceRole, testAppId, application, ''"
  }, nullValues = "NULL")
  void testRevokeConsumerRoleRejectsInvalidParameters(String type, String appId,
      String namespaceName, String env) {
    when(consumerRepository.existsById(1L)).thenReturn(true);

    assertThrows(BadRequestException.class, () -> consumerService.revokeConsumerRole(1L, type,
        appId, namespaceName, env, testOwner));
    verifyNoInteractions(consumerRoleRepository, rolePermissionService);
  }

  @Test
  void testRevokeConsumerRoleRejectsMissingOrDeletedRole() {
    when(consumerRepository.existsById(1L)).thenReturn(true);
    assertThrows(BadRequestException.class,
        () -> consumerService.revokeConsumerRole(1L, "AppRole", testAppId, null, null, testOwner));

    Role role = createRole(2L, "Master+testAppId");
    role.setDeleted(true);
    when(rolePermissionService.findRoleByRoleName("Master+testAppId")).thenReturn(role);
    assertThrows(BadRequestException.class,
        () -> consumerService.revokeConsumerRole(1L, "AppRole", testAppId, null, null, testOwner));
    verifyNoInteractions(consumerRoleRepository);
  }

  @Test
  void testFindAppIdsAuthorizedByConsumerIdSkipsSystemRoles() {
    mockConsumerRoleDetails(CREATE_APPLICATION_ROLE_NAME, MANAGE_USERS_ROLE_NAME);

    assertTrue(consumerService.findAppIdsAuthorizedByConsumerId(1L).isEmpty());
  }

  private void mockConsumerRoleDetails(String... roleNames) {
    when(consumerRepository.existsById(1L)).thenReturn(true);
    List<ConsumerRole> consumerRoles = new ArrayList<>();
    List<Role> roles = new ArrayList<>();
    List<Long> roleIds = new ArrayList<>();
    for (int i = 0; i < roleNames.length; i++) {
      long roleId = i + 1L;
      consumerRoles.add(createConsumerRole(1L, roleId));
      roles.add(createRole(roleId, roleNames[i]));
      roleIds.add(roleId);
    }
    when(consumerRoleRepository.findByConsumerId(1L)).thenReturn(consumerRoles);
    when(roleRepository.findAllById(roleIds)).thenReturn(roles);
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
