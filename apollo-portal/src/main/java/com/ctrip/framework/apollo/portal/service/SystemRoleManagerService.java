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

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SystemRoleManagerService {
  public static final Logger logger = LoggerFactory.getLogger(SystemRoleManagerService.class);

  public static final String SYSTEM_PERMISSION_TARGET_ID = "SystemRole";

  public static final String CREATE_APPLICATION_ROLE_NAME = RoleUtils.buildCreateApplicationRoleName(PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);

  public static final String CREATE_APPLICATION_LIMIT_SWITCH_KEY = "role.create-application.enabled";
  public static final String MANAGE_APP_MASTER_LIMIT_SWITCH_KEY = "role.manage-app-master.enabled";

  private final RolePermissionService rolePermissionService;

  private final PortalConfig portalConfig;

  private final RoleInitializationService roleInitializationService;

  public SystemRoleManagerService(final RolePermissionService rolePermissionService,
                                  final PortalConfig portalConfig,
                                  final RoleInitializationService roleInitializationService) {
    this.rolePermissionService = rolePermissionService;
    this.portalConfig = portalConfig;
    this.roleInitializationService = roleInitializationService;
  }

  @PostConstruct
  private void init() {
    roleInitializationService.initCreateAppRole();
  }

  private boolean isCreateApplicationPermissionEnabled() {
    return portalConfig.isCreateApplicationPermissionEnabled();
  }

  public boolean isManageAppMasterPermissionEnabled() {
    return portalConfig.isManageAppMasterPermissionEnabled();
  }

  public boolean hasCreateApplicationPermission(String userId) {
    if (!isCreateApplicationPermissionEnabled()) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.CREATE_APPLICATION, SYSTEM_PERMISSION_TARGET_ID);
  }

  public boolean hasManageAppMasterPermission(String userId, String appId) {
    if (!isManageAppMasterPermissionEnabled()) {
      return true;
    }

    return rolePermissionService.userHasPermission(userId, PermissionType.MANAGE_APP_MASTER, appId);
  }
}
