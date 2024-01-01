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
package com.ctrip.framework.apollo.portal.spi.configuration;

import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.repository.PermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RolePermissionRepository;
import com.ctrip.framework.apollo.portal.repository.RoleRepository;
import com.ctrip.framework.apollo.portal.repository.UserRoleRepository;
import com.ctrip.framework.apollo.portal.service.RoleInitializationService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultRoleInitializationService;
import com.ctrip.framework.apollo.portal.spi.defaultimpl.DefaultRolePermissionService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Timothy Liu(timothy.liu@cvte.com)
 */
@Configuration
public class RoleConfiguration {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;
    private final PortalConfig portalConfig;
    private final ConsumerRoleRepository consumerRoleRepository;
    private final UserService userService;

    public RoleConfiguration(final RoleRepository roleRepository,
        final RolePermissionRepository rolePermissionRepository,
        final UserRoleRepository userRoleRepository,
        final PermissionRepository permissionRepository,
        final PortalConfig portalConfig,
        final ConsumerRoleRepository consumerRoleRepository,
        final UserService userService) {
      this.roleRepository = roleRepository;
      this.rolePermissionRepository = rolePermissionRepository;
      this.userRoleRepository = userRoleRepository;
      this.permissionRepository = permissionRepository;
      this.portalConfig = portalConfig;
      this.consumerRoleRepository = consumerRoleRepository;
      this.userService = userService;
    }

    @Bean
    public RoleInitializationService roleInitializationService() {
        return new DefaultRoleInitializationService(rolePermissionService(), portalConfig,
            permissionRepository);
    }

    @Bean
    public RolePermissionService rolePermissionService() {
        return new DefaultRolePermissionService(roleRepository, rolePermissionRepository,
            userRoleRepository, permissionRepository, portalConfig, consumerRoleRepository,
            userService);
    }
}
