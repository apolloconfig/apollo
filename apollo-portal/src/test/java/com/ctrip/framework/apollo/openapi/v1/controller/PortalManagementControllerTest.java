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
package com.ctrip.framework.apollo.openapi.v1.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.common.controller.HttpMessageConverterConfiguration;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.http.SearchResponseEntity;
import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.entity.vo.ItemInfo;
import com.ctrip.framework.apollo.portal.entity.vo.PageSetting;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerCreateRequestVO;
import com.ctrip.framework.apollo.portal.entity.vo.consumer.ConsumerInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.environment.PortalMetaDomainService;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.CommitService;
import com.ctrip.framework.apollo.portal.service.ConfigsExportService;
import com.ctrip.framework.apollo.portal.service.ConfigsImportService;
import com.ctrip.framework.apollo.portal.service.FavoriteService;
import com.ctrip.framework.apollo.portal.service.GlobalSearchService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.service.ReleaseHistoryService;
import com.ctrip.framework.apollo.portal.service.ServerConfigService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

/**
 * Tests Portal Management OpenAPI endpoints that require a portal user session.
 */
@ExtendWith(MockitoExtension.class)
public class PortalManagementControllerTest {

  @InjectMocks
  private PortalManagementController controller;

  @Mock
  private ApolloAuditLogApi auditLogApi;
  @Mock
  private ApolloAuditProperties auditProperties;
  @Mock
  private CommitService commitService;
  @Mock
  private UnifiedPermissionValidator unifiedPermissionValidator;
  @Mock
  private PortalConfig portalConfig;
  @Mock
  private ConsumerService consumerService;
  @Mock
  private FavoriteService favoriteService;
  @Mock
  private GlobalSearchService globalSearchService;
  @Mock
  private ReleaseHistoryService releaseHistoryService;
  @Mock
  private ServerConfigService serverConfigService;
  @Mock
  private UserInfoHolder userInfoHolder;
  @Mock
  private ConfigsExportService configsExportService;
  @Mock
  private ConfigsImportService configsImportService;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private AppService appService;
  @Mock
  private PortalSettings portalSettings;
  @Mock
  private RestTemplateFactory restTemplateFactory;
  @Mock
  private PortalMetaDomainService portalMetaDomainService;
  @Mock
  private ObjectMapper objectMapper;

  @BeforeEach
  public void setUp() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
  }

  @AfterEach
  public void tearDown() {
    UserIdentityContextHolder.clear();
  }

  @Test
  public void getPageSettingsShouldReturnPortalConfigValues() {
    when(portalConfig.wikiAddress()).thenReturn("https://wiki.example");
    when(portalConfig.canAppAdminCreatePrivateNamespace()).thenReturn(true);

    ResponseEntity<Object> response = controller.getPageSettings();

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    PageSetting setting = (PageSetting) response.getBody();
    assertEquals("https://wiki.example", setting.getWikiAddress());
    assertTrue(setting.isCanAppAdminCreatePrivateNamespace());
  }

  @Test
  public void findCommitsShouldHideConfigWithoutNamespaceReadPermission() {
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(
        "someApp", "DEV", "default", "application")).thenReturn(true);

    ResponseEntity<List<Object>> response =
        controller.findCommits("someApp", "DEV", "default", "application", null, 0, 10);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
    verify(unifiedPermissionValidator)
        .shouldHideConfigToCurrentUser("someApp", "DEV", "default", "application");
    verifyNoInteractions(commitService);
  }

  @Test
  public void findCommitsShouldRejectInvalidEnv() {
    assertThrows(BadRequestException.class,
        () -> controller.findCommits("someApp", "invalid", "default", "application", null, 0, 10));

    verifyNoInteractions(unifiedPermissionValidator, commitService);
  }

  @Test
  public void findCommitsShouldAcceptLowercaseEnv() {
    when(commitService.find("someApp", Env.DEV, "default", "application", 0, 10))
        .thenReturn(Collections.emptyList());

    ResponseEntity<List<Object>> response =
        controller.findCommits("someApp", "dev", "default", "application", null, 0, 10);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertTrue(response.getBody().isEmpty());
    verify(commitService).find("someApp", Env.DEV, "default", "application", 0, 10);
  }

  @Test
  public void findCommitsShouldDefaultPaginationWhenOmitted() {
    when(commitService.find("someApp", Env.DEV, "default", "application", 0, 10))
        .thenReturn(Collections.emptyList());

    ResponseEntity<List<Object>> response =
        controller.findCommits("someApp", "DEV", "default", "application", null, null, null);

    assertEquals(200, response.getStatusCode().value());
    verify(commitService).find("someApp", Env.DEV, "default", "application", 0, 10);
  }

  @Test
  public void findCommitsByKeyShouldDefaultPaginationWhenOmitted() {
    when(commitService.findByKey("someApp", Env.DEV, "default", "application", "timeout", 0, 10))
        .thenReturn(Collections.emptyList());

    ResponseEntity<List<Object>> response = controller.findCommits("someApp", "DEV", "default",
        "application", "timeout", null, null);

    assertEquals(200, response.getStatusCode().value());
    verify(commitService).findByKey("someApp", Env.DEV, "default", "application", "timeout", 0,
        10);
  }

  @Test
  public void findReleaseHistoriesShouldDefaultPaginationWhenOmitted() {
    when(releaseHistoryService.findNamespaceReleaseHistory("someApp", Env.DEV, "default",
        "application", 0, 10)).thenReturn(Collections.emptyList());

    ResponseEntity<List<Object>> response = controller.findReleaseHistoriesByNamespace("someApp",
        "DEV", "default", "application", null, null);

    assertEquals(200, response.getStatusCode().value());
    verify(releaseHistoryService).findNamespaceReleaseHistory("someApp", Env.DEV, "default",
        "application", 0, 10);
  }

  @Test
  public void createOrUpdatePortalDBConfigShouldRejectBlankKey() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey(" ");
    serverConfig.setValue("value");

    assertThrows(BadRequestException.class,
        () -> controller.createOrUpdatePortalDBConfig(serverConfig));

    verifyNoInteractions(serverConfigService);
  }

  @Test
  public void createOrUpdateConfigDBConfigShouldRejectBlankValue() {
    ServerConfig serverConfig = new ServerConfig();
    serverConfig.setKey("timeout");
    serverConfig.setValue(" ");

    assertThrows(BadRequestException.class,
        () -> controller.createOrUpdateConfigDBConfig("DEV", serverConfig));

    verifyNoInteractions(serverConfigService);
  }

  @Test
  public void searchItemInfoByKeyOrValueShouldRejectNullCriteria() {
    assertThrows(BadRequestException.class,
        () -> controller.searchItemInfoByKeyOrValue(null, null));

    verifyNoInteractions(globalSearchService);
  }

  @Test
  public void searchItemInfoByKeyOrValueShouldAllowOneNullCriterion() {
    when(portalConfig.getPerEnvSearchMaxResults()).thenReturn(10);
    SearchResponseEntity<List<ItemInfo>> searchResponse =
        SearchResponseEntity.ok(Collections.emptyList());
    when(globalSearchService.getAllEnvItemInfoBySearch(null, "timeout", 0, 10))
        .thenReturn(searchResponse);

    ResponseEntity<Object> response = controller.searchItemInfoByKeyOrValue(null, "timeout");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(searchResponse, response.getBody());
    verify(globalSearchService).getAllEnvItemInfoBySearch(null, "timeout", 0, 10);
  }

  @Test
  public void createConsumerShouldAssignManageUsersRole() {
    ConsumerCreateRequestVO request = new ConsumerCreateRequestVO();
    request.setAppId("consumer-app");
    request.setName("consumer");
    request.setOwnerName("apollo");
    request.setOrgId("org");
    request.setRateLimit(0);
    request.setAllowManageUsers(true);
    UserInfo currentUser = new UserInfo("apollo");
    when(userInfoHolder.getUser()).thenReturn(currentUser);
    Consumer createdConsumer = new Consumer();
    createdConsumer.setId(1);
    createdConsumer.setAppId("consumer-app");
    when(consumerService.createConsumer(any(Consumer.class), eq("apollo")))
        .thenReturn(createdConsumer);
    ConsumerToken consumerToken = new ConsumerToken();
    consumerToken.setToken("token");
    when(consumerService.generateAndSaveConsumerToken(eq(createdConsumer), eq(0), any(),
        eq("apollo"))).thenReturn(consumerToken);
    ConsumerInfo consumerInfo = new ConsumerInfo();
    consumerInfo.setConsumerId(1);
    consumerInfo.setAppId("consumer-app");
    consumerInfo.setName("consumer");
    consumerInfo.setOwnerName("apollo");
    consumerInfo.setOrgId("org");
    consumerInfo.setToken("token");
    consumerInfo.setAllowManageUsers(true);
    when(consumerService.getConsumerInfoByAppId("consumer-app")).thenReturn(consumerInfo);

    ResponseEntity<Object> response = controller.createConsumer(request, null);

    assertEquals(200, response.getStatusCode().value());
    ConsumerInfo responseBody = (ConsumerInfo) response.getBody();
    assertNotNull(responseBody);
    assertSame(consumerInfo, responseBody);
    assertEquals("consumer-app", responseBody.getAppId());
    assertTrue(responseBody.isAllowManageUsers());
    verify(consumerService).assignManageUsersRoleToConsumer("token", "apollo");
  }

  @Test
  public void getConsumerTokenByAppIdShouldReturnConsumerToken() {
    Date expires = new Date(1893456000000L);
    Date createdTime = new Date(1717200000000L);
    Date lastModifiedTime = new Date(1717286400000L);
    ConsumerToken consumerToken = new ConsumerToken();
    consumerToken.setConsumerId(1);
    consumerToken.setToken("token");
    consumerToken.setRateLimit(100);
    consumerToken.setExpires(expires);
    consumerToken.setDataChangeCreatedBy("apollo");
    consumerToken.setDataChangeCreatedTime(createdTime);
    consumerToken.setDataChangeLastModifiedBy("apollo");
    consumerToken.setDataChangeLastModifiedTime(lastModifiedTime);
    when(consumerService.getConsumerTokenByAppId("consumer-app")).thenReturn(consumerToken);

    ResponseEntity<Object> response = controller.getConsumerTokenByAppId("consumer-app");

    assertEquals(200, response.getStatusCode().value());
    ConsumerToken responseBody = (ConsumerToken) response.getBody();
    assertNotNull(responseBody);
    assertSame(consumerToken, responseBody);
    assertEquals(1L, responseBody.getConsumerId());
    assertEquals("token", responseBody.getToken());
    assertEquals(100, responseBody.getRateLimit());
    assertEquals(expires, responseBody.getExpires());
    assertEquals("apollo", responseBody.getDataChangeCreatedBy());
    assertEquals(createdTime, responseBody.getDataChangeCreatedTime());
    assertEquals("apollo", responseBody.getDataChangeLastModifiedBy());
    assertEquals(lastModifiedTime, responseBody.getDataChangeLastModifiedTime());
    String json = new HttpMessageConverterConfiguration().gson().toJson(response.getBody());
    assertTrue(json.contains("\"token\":\"token\""));
    assertTrue(json.contains("\"expires\""));
    verify(consumerService).getConsumerTokenByAppId("consumer-app");
  }

  @Test
  public void getConsumerListShouldReturnConsumerInfos() {
    ConsumerInfo consumerInfo = new ConsumerInfo();
    consumerInfo.setConsumerId(1);
    consumerInfo.setAppId("consumer-app");
    consumerInfo.setName("consumer");
    consumerInfo.setOwnerName("apollo");
    consumerInfo.setOwnerEmail("apollo@example.com");
    consumerInfo.setOrgId("org");
    consumerInfo.setOrgName("Org");
    consumerInfo.setRateLimit(100);
    consumerInfo.setToken(null);
    consumerInfo.setAllowCreateApplication(true);
    consumerInfo.setAllowManageUsers(true);
    when(consumerService.findConsumerInfoList(any()))
        .thenReturn(Collections.singletonList(consumerInfo));

    ResponseEntity<List<Object>> response = controller.getConsumerList(0, 10);

    assertEquals(200, response.getStatusCode().value());
    assertNotNull(response.getBody());
    assertEquals(1, response.getBody().size());
    ConsumerInfo responseBody = (ConsumerInfo) response.getBody().get(0);
    assertSame(consumerInfo, responseBody);
    assertEquals("consumer-app", responseBody.getAppId());
    assertTrue(responseBody.isAllowCreateApplication());
    assertTrue(responseBody.isAllowManageUsers());
    assertEquals(100, responseBody.getRateLimit());
  }

  @Test
  public void exportAllConfigsShouldReturnFileBackedResource() throws Exception {
    byte[] exported = {1, 2, 3};
    doAnswer(invocation -> {
      OutputStream outputStream = invocation.getArgument(0);
      outputStream.write(exported);
      return null;
    }).when(configsExportService).exportData(any(OutputStream.class), anyList());

    ResponseEntity<Resource> response = controller.exportAllConfigs("DEV");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(3, response.getHeaders().getContentLength());
    assertNotNull(response.getBody());
    try (InputStream inputStream = response.getBody().getInputStream()) {
      assertArrayEquals(exported, inputStream.readAllBytes());
    }
    verify(configsExportService).exportData(any(OutputStream.class), anyList());
  }

  @Test
  public void exportAppConfigShouldReturnFileBackedResource() throws Exception {
    byte[] exported = {4, 5};
    doAnswer(invocation -> {
      OutputStream outputStream = invocation.getArgument(3);
      outputStream.write(exported);
      return null;
    }).when(configsExportService).exportAppConfigByEnvAndCluster(eq("someApp"), eq(Env.DEV),
        eq("default"), any(OutputStream.class));

    ResponseEntity<Resource> response = controller.exportAppConfig("someApp", "DEV", "default");

    assertEquals(200, response.getStatusCode().value());
    assertEquals(2, response.getHeaders().getContentLength());
    assertNotNull(response.getBody());
    try (InputStream inputStream = response.getBody().getInputStream()) {
      assertArrayEquals(exported, inputStream.readAllBytes());
    }
    verify(configsExportService).exportAppConfigByEnvAndCluster(eq("someApp"), eq(Env.DEV),
        eq("default"), any(OutputStream.class));
  }

  @Test
  public void importAllConfigsShouldStreamMultipartInput() throws Exception {
    when(userInfoHolder.getUser()).thenReturn(new UserInfo("operator"));
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    controller.importAllConfigs("DEV", "ignore", file);

    verify(file).getInputStream();
    verify(file, never()).getBytes();
    verify(configsImportService).importDataFromZipFile(eq(Collections.singletonList(Env.DEV)),
        any(ZipInputStream.class), eq(true), eq("operator"));
  }

  @Test
  public void importAllConfigsShouldDefaultMissingConflictActionToIgnore() throws Exception {
    when(userInfoHolder.getUser()).thenReturn(new UserInfo("operator"));
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    controller.importAllConfigs("DEV", null, file);

    verify(configsImportService).importDataFromZipFile(eq(Collections.singletonList(Env.DEV)),
        any(ZipInputStream.class), eq(true), eq("operator"));
  }

  @Test
  public void importAppConfigShouldStreamMultipartInput() throws Exception {
    when(userInfoHolder.getUser()).thenReturn(new UserInfo("operator"));
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    controller.importAppConfig("someApp", "DEV", "default", "cover", file);

    verify(file).getInputStream();
    verify(file, never()).getBytes();
    verify(configsImportService).importAppConfigFromZipFile(eq("someApp"), eq(Env.DEV),
        eq("default"), any(ZipInputStream.class), eq(false), eq("operator"));
  }

  @Test
  public void importAppConfigShouldDefaultMissingConflictActionToIgnore() throws Exception {
    when(userInfoHolder.getUser()).thenReturn(new UserInfo("operator"));
    MultipartFile file = mock(MultipartFile.class);
    when(file.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));

    controller.importAppConfig("someApp", "DEV", "default", null, file);

    verify(configsImportService).importAppConfigFromZipFile(eq("someApp"), eq(Env.DEV),
        eq("default"), any(ZipInputStream.class), eq(true), eq("operator"));
  }

  @Test
  public void exportNamespaceItemsShouldReturnConfigContentAsResource() throws Exception {
    ItemDTO item = new ItemDTO();
    item.setKey("timeout");
    item.setValue("100");
    ItemBO itemBO = new ItemBO();
    itemBO.setItem(item);
    NamespaceBO namespaceBO = new NamespaceBO();
    namespaceBO.setFormat("properties");
    namespaceBO.setItems(Collections.singletonList(itemBO));
    when(
        namespaceService.loadNamespaceBO("someApp", Env.DEV, "default", "application", true, false))
        .thenReturn(namespaceBO);

    ResponseEntity<Resource> response =
        controller.exportNamespaceItems("someApp", "DEV", "default", "application");

    assertEquals(200, response.getStatusCode().value());
    assertEquals("attachment;filename=application.properties",
        response.getHeaders().getFirst("Content-Disposition"));
    assertNotNull(response.getBody());
    String content =
        new String(response.getBody().getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertTrue(content.contains("\"key\":\"timeout\""));
    assertTrue(content.contains("\"value\":\"100\""));
    verify(namespaceService).loadNamespaceBO("someApp", Env.DEV, "default", "application", true,
        false);
  }

  @Test
  public void exportNamespaceItemsShouldRejectConsumerTokenBeforeConfigVisibilityCheck() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);

    assertThrows(AccessDeniedException.class,
        () -> controller.exportNamespaceItems("someApp", "DEV", "default", "application"));

    verifyNoInteractions(unifiedPermissionValidator, namespaceService);
  }

  @Test
  public void exportNamespaceItemsShouldRejectHiddenConfig() {
    when(unifiedPermissionValidator.shouldHideConfigToCurrentUser(
        "someApp", "DEV", "default", "application")).thenReturn(true);

    assertThrows(AccessDeniedException.class,
        () -> controller.exportNamespaceItems("someApp", "DEV", "default", "application"));

    verify(unifiedPermissionValidator)
        .shouldHideConfigToCurrentUser("someApp", "DEV", "default", "application");
    verifyNoInteractions(namespaceService);
  }

  @Test
  public void getPageSettingsShouldRejectConsumerToken() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);

    assertThrows(AccessDeniedException.class, () -> controller.getPageSettings());
  }
}
