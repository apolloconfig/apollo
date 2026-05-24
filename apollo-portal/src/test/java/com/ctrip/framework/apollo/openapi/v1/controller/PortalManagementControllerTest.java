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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.audit.ApolloAuditProperties;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.RestTemplateFactory;
import com.ctrip.framework.apollo.portal.component.UnifiedPermissionValidator;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.vo.PageSetting;
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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;

@RunWith(MockitoJUnitRunner.class)
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

  @Before
  public void setUp() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.USER);
  }

  @After
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

  @Test(expected = AccessDeniedException.class)
  public void getPageSettingsShouldRejectConsumerToken() {
    UserIdentityContextHolder.setAuthType(UserIdentityConstants.CONSUMER);

    controller.getPageSettings();
  }
}
