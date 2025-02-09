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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.AbstractUnitTest;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;

import org.assertj.core.util.Files;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author lepdou 2021-08-30
 */
public class ConfigsExportServiceTest extends AbstractUnitTest {

  @Mock
  private AppService                appService;
  @Mock
  private ClusterService            clusterService;
  @Mock
  private NamespaceService          namespaceService;
  @Mock
  private UserPermissionValidator userPermissionValidator;
  @Mock
  private UserInfoHolder            userInfoHolder;
  @Mock
  private AppNamespaceService       appNamespaceService;
  @InjectMocks
  private ConfigsExportService      configsExportService;
  @Mock
  private ItemService               itemService;
  @Mock
  private ApplicationEventPublisher applicationEventPublisher;
  @Mock
  private RoleInitializationService roleInitializationService;
  @InjectMocks
  private ConfigsImportService      configsImportService;

  @Test
  public void testNamespaceExportImport() throws FileNotFoundException {
    // Test with fillItemDetail = true
    testExportImportScenario(true);
  }

  @Test
  public void testNamespaceExportImportWithFillItemDetail() throws FileNotFoundException {
    // Test with fillItemDetail = false
    testExportImportScenario(false);
  }

  private void testExportImportScenario(boolean fillItemDetail) throws FileNotFoundException {

    File temporaryFolder = Files.newTemporaryFolder();
    temporaryFolder.deleteOnExit();
    String filePath = temporaryFolder + File.separator + "export.zip";

    //export config
    UserInfo userInfo = genUser();
    when(userInfoHolder.getUser()).thenReturn(userInfo);

    Env env = Env.DEV;
    String appId1 = "app1";
    String appId2 = "app2";

    App app1 = genApp(appId1, appId1, "org1", "org2");
    App app2 = genApp(appId2, appId2, "org1", "org2");

    List<App> exportApps = Lists.newArrayList(app1, app2);

    String appNamespaceName1 = "ns1";
    String appNamespaceName2 = "ns2";
    AppNamespace app1Namespace1 = genAppNamespace(appId1, appNamespaceName1, false);
    AppNamespace app1Namespace2 = genAppNamespace(appId1, appNamespaceName2, true);
    AppNamespace app2Namespace1 = genAppNamespace(appId2, appNamespaceName1, false);

    List<AppNamespace> appNamespaces = Lists.newArrayList(app1Namespace1, app1Namespace2, app2Namespace1);

    String clusterName1 = "c1";
    String clusterName2 = "c2";
    ClusterDTO app1Cluster1 = genCluster(clusterName1, appId1);
    ClusterDTO app1Cluster2 = genCluster(clusterName2, appId1);

    ClusterDTO app2Cluster1 = genCluster(clusterName1, appId2);
    ClusterDTO app2Cluster2 = genCluster(clusterName2, appId2);

    List<ClusterDTO>
        app1Clusters =
        Lists.newArrayList(app1Cluster1, app1Cluster2);
    List<ClusterDTO>
        app2Clusters =
        Lists.newArrayList(app2Cluster1, app2Cluster2);

    ItemBO item1 = genItem("k1", "v1");
    ItemBO item2 = genItem("k2", "v2");
    List<ItemBO> items = Lists.newArrayList(item1, item2);

    String namespaceName1 = "namespace1";
    String namespaceName2 = "namespace2";
    NamespaceBO app1Cluster1Namespace1 = genNamespace(app1, app1Cluster1, items, namespaceName1);
    NamespaceBO app1Cluster1Namespace2 = genNamespace(app1, app1Cluster1, items, namespaceName2);
    List<NamespaceBO> app1Cluster1Namespace = Lists.newArrayList(app1Cluster1Namespace1, app1Cluster1Namespace2);
    NamespaceBO app1Cluster2Namespace1 = genNamespace(app1, app1Cluster2, items, namespaceName1);
    List<NamespaceBO> app1Cluster2Namespace = Lists.newArrayList(app1Cluster2Namespace1);

    NamespaceBO app2Cluster1Namespace1 = genNamespace(app2, app1Cluster1, items, namespaceName1);
    List<NamespaceBO> app2Cluster1Namespace = Lists.newArrayList(app2Cluster1Namespace1);
    NamespaceBO app2Cluster2Namespace1 = genNamespace(app2, app1Cluster2, items, namespaceName1);
    NamespaceBO app2Cluster2Namespace2 = genNamespace(app2, app1Cluster2, items, namespaceName2);
    List<NamespaceBO> app2Cluster2Namespace = Lists.newArrayList(app2Cluster2Namespace1, app2Cluster2Namespace2);

    when(appService.findAll()).thenReturn(exportApps);
    when(appNamespaceService.findAll()).thenReturn(appNamespaces);
    when(userPermissionValidator.isAppAdmin(any())).thenReturn(true);
    when(clusterService.findClusters(env, appId1)).thenReturn(app1Clusters);
    when(clusterService.findClusters(env, appId2)).thenReturn(app2Clusters);
    when(namespaceService.findNamespaceBOs(appId1, Env.DEV, clusterName1, fillItemDetail, false)).thenReturn(app1Cluster1Namespace);
    when(namespaceService.findNamespaceBOs(appId1, Env.DEV, clusterName2, fillItemDetail, false)).thenReturn(app1Cluster2Namespace);
    when(namespaceService.findNamespaceBOs(appId2, Env.DEV, clusterName1, fillItemDetail, false)).thenReturn(app2Cluster1Namespace);
    when(namespaceService.findNamespaceBOs(appId2, Env.DEV, clusterName2, fillItemDetail, false)).thenReturn(app2Cluster2Namespace);

    FileOutputStream fileOutputStream = new FileOutputStream(filePath);

    configsExportService.exportData(fileOutputStream, Lists.newArrayList(Env.DEV));

    //import config
    when(appNamespaceService.findByAppIdAndName(any(), any())).thenReturn(null);
    when(appNamespaceService.importAppNamespaceInLocal(any())).thenReturn(app1Namespace1);
    when(appService.load(any())).thenReturn(null);
    when(appService.load(any(), any())).thenThrow(new RuntimeException());

    when(clusterService.loadCluster(any(), any(), any())).thenThrow(new RuntimeException());

    when(namespaceService.loadNamespaceBaseInfo(any(), any(), any(), any())).thenThrow(new RuntimeException());
    when(namespaceService.createNamespace(any(), any())).thenReturn(genNamespaceDTO(1));

    when(itemService.findItems(any(), any(), any(), any())).thenReturn(Lists.newArrayList());
    HttpStatusCodeException itemNotFoundException = new HttpClientErrorException(HttpStatus.NOT_FOUND);
    when(itemService.loadItem(any(), any(), any(), any(), anyString())).thenThrow(itemNotFoundException);

    FileInputStream fileInputStream = new FileInputStream(filePath);
    ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);

    try {
      configsImportService.importDataFromZipFile(Lists.newArrayList(Env.DEV), zipInputStream, false);
    } catch (Exception e) {
      e.printStackTrace();
    }

    verify(appNamespaceService, times(3)).importAppNamespaceInLocal(any());
    verify(applicationEventPublisher, times(3)).publishEvent(any());

    verify(appService, times(2)).createAppInRemote(any(), any());

    verify(clusterService, times(4)).createCluster(any(), any());

    if(fillItemDetail){
      verify(namespaceService, times(6)).createNamespace(any(), any());
      verify(roleInitializationService,times(6)).initNamespaceRoles(any(), any(), anyString());
      verify(roleInitializationService,times(6)).initNamespaceEnvRoles(any(), any(), anyString());
      verify(itemService, times(12)).createItem(any(), any(), any(), any(), any());
    }
  }

  private App genApp(String name, String appId, String orgId, String orgName) {
    App app = new App();
    app.setAppId(appId);
    app.setName(name);
    app.setOrgName("apollo");
    app.setOrgId(orgId);
    app.setOrgName(orgName);
    return app;
  }

  private ClusterDTO genCluster(String name, String appId) {
    ClusterDTO clusterDTO = new ClusterDTO();
    clusterDTO.setAppId(appId);
    clusterDTO.setName(name);
    return clusterDTO;
  }

  private AppNamespace genAppNamespace(String appId, String name, boolean isPublic) {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setAppId(appId);
    appNamespace.setPublic(isPublic);
    appNamespace.setName(name);
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());
    return appNamespace;
  }

  private NamespaceBO genNamespace(App app, ClusterDTO clusterDTO, List<ItemBO> itemBOS, String namespaceName) {
    NamespaceBO namespaceBO = new NamespaceBO();

    NamespaceDTO baseInfo = new NamespaceDTO();
    baseInfo.setNamespaceName(namespaceName);
    baseInfo.setAppId(app.getAppId());
    baseInfo.setClusterName(clusterDTO.getName());

    namespaceBO.setBaseInfo(baseInfo);
    namespaceBO.setFormat(ConfigFileFormat.Properties.getValue());
    namespaceBO.setItems(itemBOS);

    return namespaceBO;
  }

  private ItemBO genItem(String key, String value) {
    ItemBO itemBO = new ItemBO();

    ItemDTO itemDTO = new ItemDTO();
    itemDTO.setKey(key);
    itemDTO.setValue(value);

    itemBO.setItem(itemDTO);

    return itemBO;
  }

  private NamespaceDTO genNamespaceDTO(long id) {
    NamespaceDTO dto = new NamespaceDTO();
    dto.setId(id);
    return dto;
  }

  private UserInfo genUser() {
    UserInfo userInfo = new UserInfo();
    userInfo.setUserId("apollo");
    userInfo.setName("apollo");
    userInfo.setEmail("apollo@apollo.com");
    return userInfo;
  }
}
