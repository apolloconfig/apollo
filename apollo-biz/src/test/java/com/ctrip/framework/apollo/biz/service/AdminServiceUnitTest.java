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

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.AccessKey;
import com.ctrip.framework.apollo.common.constants.AccessKeyMode;
import com.ctrip.framework.apollo.common.entity.App;
import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link AdminService#createNewApp} access-key auto-provision behavior.
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
  @Mock
  private BizConfig bizConfig;
  @Mock
  private AccessKeyService accessKeyService;

  private AdminService adminService;

  @Before
  public void setUp() {
    adminService = new AdminService(appService, appNamespaceService, clusterService,
        namespaceService, accessKeyService, bizConfig);
  }

  @Test
  public void createNewApp_whenAutoProvisionDisabled_doesNotCreateAccessKey() {
    when(bizConfig.isAccessKeyAutoProvisionEnabled()).thenReturn(false);
    App saved = savedApp();
    when(appService.save(any(App.class))).thenReturn(saved);

    adminService.createNewApp(inputApp());

    verify(accessKeyService, never()).create(any(String.class), any(AccessKey.class));
  }

  @Test
  public void createNewApp_whenAutoProvisionEnabled_createsEnabledFilterModeAccessKey() {
    when(bizConfig.isAccessKeyAutoProvisionEnabled()).thenReturn(true);
    App saved = savedApp();
    when(appService.save(any(App.class))).thenReturn(saved);

    adminService.createNewApp(inputApp());

    ArgumentCaptor<AccessKey> captor = ArgumentCaptor.forClass(AccessKey.class);
    verify(accessKeyService).create(eq(APP_ID), captor.capture());
    AccessKey created = captor.getValue();
    assertEquals(AccessKeyMode.FILTER, created.getMode());
    assertEquals(true, created.isEnabled());
    assertEquals(OPERATOR, created.getDataChangeCreatedBy());
    assertEquals(32, created.getSecret().length());
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
