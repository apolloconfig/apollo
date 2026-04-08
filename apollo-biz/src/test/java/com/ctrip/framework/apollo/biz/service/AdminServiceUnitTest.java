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
package com.ctrip.framework.apollo.biz.service;

import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.entity.App;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AdminService#createNewApp}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AdminServiceUnitTest {

  private static final String APP_ID = "unitTestApp";
  private static final String OPERATOR = "operator";

  @Mock
  private AppService appService;
  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private ClusterService clusterService;
  @Mock
  private NamespaceService namespaceService;

  private AdminService adminService;

  @Before
  public void setUp() {
    adminService =
        new AdminService(appService, appNamespaceService, clusterService, namespaceService);
  }

  @Test
  public void createNewApp_createsDefaultAppStructure() {
    App saved = savedApp();
    when(appService.save(any(App.class))).thenReturn(saved);

    adminService.createNewApp(inputApp());

    verify(appNamespaceService).createDefaultAppNamespace(APP_ID, OPERATOR);
    verify(clusterService).createDefaultCluster(APP_ID, OPERATOR);
    verify(namespaceService).instanceOfAppNamespaces(APP_ID, "default", OPERATOR);
  }

  @Test
  public void createNewApp_returnsSavedAppEntity() {
    App saved = savedApp();
    when(appService.save(any(App.class))).thenReturn(saved);

    App created = adminService.createNewApp(inputApp());
    assertSame(saved, created);
  }

  private App inputApp() {
    App app = new App();
    app.setAppId(APP_ID);
    app.setName("n");
    app.setOwnerName(OPERATOR);
    app.setOwnerEmail("o@x.com");
    app.setDataChangeCreatedBy(OPERATOR);
    app.setDataChangeLastModifiedBy(OPERATOR);
    app.setDataChangeCreatedTime(new Date());
    return app;
  }

  private App savedApp() {
    App app = inputApp();
    app.setId(1L);
    return app;
  }
}
