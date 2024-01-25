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
package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.Audit;
import com.ctrip.framework.apollo.biz.entity.Cluster;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.repository.AppRepository;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.core.ConfigConsts;
import java.util.Date;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class AdminServiceTest extends AbstractIntegrationTest {

  @Autowired
  private AdminService adminService;

  @Autowired
  private AuditService auditService;

  @Autowired
  private AppRepository appRepository;

  @Autowired
  private ClusterService clusterService;

  @Autowired
  private NamespaceService namespaceService;

  @Autowired
  private AppNamespaceService appNamespaceService;

  @Test
  public void testCreateNewApp() {
    String appId = "someAppId";
    App app = new App();
    app.setAppId(appId);
    app.setName("someAppName");
    String owner = "someOwnerName";
    app.setOwnerName(owner);
    app.setOwnerEmail("someOwnerName@ctrip.com");
    app.setDataChangeCreatedBy(owner);
    app.setDataChangeLastModifiedBy(owner);
    app.setDataChangeCreatedTime(new Date());

    app = adminService.createNewApp(app);
    Assert.assertEquals(appId, app.getAppId());

    List<Cluster> clusters = clusterService.findParentClusters(app.getAppId());
    Assert.assertEquals(1, clusters.size());
    Assert.assertEquals(ConfigConsts.CLUSTER_NAME_DEFAULT, clusters.get(0).getName());

    List<Namespace> namespaces = namespaceService.findNamespaces(appId, clusters.get(0).getName());
    Assert.assertEquals(1, namespaces.size());
    Assert.assertEquals(ConfigConsts.NAMESPACE_APPLICATION, namespaces.get(0).getNamespaceName());

    List<Audit> audits = auditService.findByOwner(owner);
    Assert.assertEquals(4, audits.size());
  }

  @Test(expected = ServiceException.class)
  public void testCreateDuplicateApp() {
    String appId = "someAppId";
    App app = new App();
    app.setAppId(appId);
    app.setName("someAppName");
    String owner = "someOwnerName";
    app.setOwnerName(owner);
    app.setOwnerEmail("someOwnerName@ctrip.com");
    app.setDataChangeCreatedBy(owner);
    app.setDataChangeLastModifiedBy(owner);
    app.setDataChangeCreatedTime(new Date());

    appRepository.save(app);

    adminService.createNewApp(app);
  }

  @Test
  public void testDeleteApp() {
    String appId = "someAppId";
    App app = new App();
    app.setAppId(appId);
    app.setName("someAppName");
    String owner = "someOwnerName";
    app.setOwnerName(owner);
    app.setOwnerEmail("someOwnerName@ctrip.com");
    app.setDataChangeCreatedBy(owner);
    app.setDataChangeLastModifiedBy(owner);
    app.setDataChangeCreatedTime(new Date());

    app = adminService.createNewApp(app);

    Assert.assertEquals(appId, app.getAppId());

    Assert.assertEquals(1, appNamespaceService.findByAppId(appId).size());

    Assert.assertEquals(1, clusterService.findClusters(appId).size());

    Assert.assertEquals(1, namespaceService.findNamespaces(appId, ConfigConsts.CLUSTER_NAME_DEFAULT).size());

    adminService.deleteApp(app, owner);

    Assert.assertEquals(0, appNamespaceService.findByAppId(appId).size());

    Assert.assertEquals(0, clusterService.findClusters(appId).size());

    Assert
        .assertEquals(0, namespaceService.findByAppIdAndNamespaceName(appId, ConfigConsts.CLUSTER_NAME_DEFAULT).size());
  }

}
