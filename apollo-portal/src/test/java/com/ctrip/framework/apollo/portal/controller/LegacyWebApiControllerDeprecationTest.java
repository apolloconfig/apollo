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
package com.ctrip.framework.apollo.portal.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
class LegacyWebApiControllerDeprecationTest {

  private static final List<Class<?>> MIGRATED_LEGACY_WEB_API_CONTROLLERS =
      List.of(AccessKeyController.class, AppController.class, ClusterController.class,
          CommitController.class, ConfigsExportController.class, ConfigsImportController.class,
          ConsumerController.class, EnvController.class, FavoriteController.class,
          GlobalSearchController.class, InstanceController.class, ItemController.class,
          NamespaceBranchController.class, NamespaceController.class, NamespaceLockController.class,
          OrganizationController.class, PageSettingController.class, PermissionController.class,
          ReleaseController.class, ReleaseHistoryController.class, SearchController.class,
          ServerConfigController.class, SystemInfoController.class, UserInfoController.class);

  private static final List<Class<?>> ACTIVE_PORTAL_INFRASTRUCTURE_CONTROLLERS =
      List.of(PrefixPathController.class, SignInController.class, SsoHeartbeatController.class);

  @Test
  void migratedLegacyWebApiControllersShouldBeDeprecated() {
    for (Class<?> controllerClass : MIGRATED_LEGACY_WEB_API_CONTROLLERS) {
      assertThat(controllerClass.getAnnotation(Deprecated.class)).as(controllerClass.getName())
          .isNotNull();
    }
  }

  @Test
  void activePortalInfrastructureControllersShouldNotBeDeprecated() {
    for (Class<?> controllerClass : ACTIVE_PORTAL_INFRASTRUCTURE_CONTROLLERS) {
      assertThat(controllerClass.getAnnotation(Deprecated.class)).as(controllerClass.getName())
          .isNull();
    }
  }
}
