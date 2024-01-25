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
import com.ctrip.framework.apollo.biz.repository.AppNamespaceRepository;
import com.ctrip.framework.apollo.biz.repository.AppRepository;
import com.ctrip.framework.apollo.biz.repository.ClusterRepository;
import com.ctrip.framework.apollo.biz.repository.NamespaceRepository;
import com.ctrip.framework.apollo.common.entity.App;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.BeforeTransaction;

import java.util.Date;

public class AdminServiceTransactionTest extends AbstractIntegrationTest {

  @Autowired
  AdminService adminService;

  @Autowired
  private AppRepository appRepository;

  @Autowired
  private AppNamespaceRepository appNamespaceRepository;

  @Autowired
  private NamespaceRepository namespaceRepository;

  @Autowired
  private ClusterRepository clusterRepository;

  @BeforeTransaction
  public void verifyInitialDatabaseState() {
    for (App app : appRepository.findAll()) {
      System.out.println(app.getAppId());
    }
    Assert.assertEquals(0, appRepository.count());
    Assert.assertEquals(7, appNamespaceRepository.count());
    Assert.assertEquals(0, namespaceRepository.count());
    Assert.assertEquals(0, clusterRepository.count());
  }

  @Before
  public void setUpTestDataWithinTransaction() {
    Assert.assertEquals(0, appRepository.count());
    Assert.assertEquals(7, appNamespaceRepository.count());
    Assert.assertEquals(0, namespaceRepository.count());
    Assert.assertEquals(0, clusterRepository.count());
  }

  @Test
  @Rollback
  public void modifyDatabaseWithinTransaction() {
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
    adminService.createNewApp(app);
  }

  @After
  public void tearDownWithinTransaction() {
    Assert.assertEquals(1, appRepository.count());
    Assert.assertEquals(8, appNamespaceRepository.count());
    Assert.assertEquals(1, namespaceRepository.count());
    Assert.assertEquals(1, clusterRepository.count());
  }

  @AfterTransaction
  public void verifyFinalDatabaseState() {
    Assert.assertEquals(0, appRepository.count());
    Assert.assertEquals(7, appNamespaceRepository.count());
    Assert.assertEquals(0, namespaceRepository.count());
    Assert.assertEquals(0, clusterRepository.count());
  }
}
