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
package com.ctrip.framework.apollo.portal.controller;


import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ServerConfigService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 配置中心本身需要一些配置,这些配置放在数据库里面
 */
@RestController
public class ServerConfigController {
  private final ServerConfigService serverConfigService;

  public ServerConfigController(final ServerConfigService serverConfigService) {
    this.serverConfigService = serverConfigService;
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @PostMapping("/server/portal-db/config")
  @ApolloAuditLog(type = OpType.CREATE, name = "ServerConfig.createOrUpdatePortalDBConfig")
  public ServerConfig createOrUpdatePortalDBConfig(@Valid @RequestBody ServerConfig serverConfig) {
    return serverConfigService.createOrUpdatePortalDBConfig(serverConfig);
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @PostMapping("/server/envs/{env}/config-db/config")
  @ApolloAuditLog(type = OpType.CREATE, name = "ServerConfig.createOrUpdateConfigDBConfig")
  public ServerConfig createOrUpdateConfigDBConfig(@Valid @RequestBody ServerConfig serverConfig, @PathVariable String env) {
    return serverConfigService.createOrUpdateConfigDBConfig(Env.transformEnv(env), serverConfig);
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @GetMapping("/server/portal-db/config/find-all-config")
  public List<ServerConfig> findAllPortalDBServerConfig() {
    return serverConfigService.findAllPortalDBConfig();
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @GetMapping("/server/envs/{env}/config-db/config/find-all-config")
  public List<ServerConfig> findAllConfigDBServerConfig(@PathVariable String env) {
    return serverConfigService.findAllConfigDBConfig(Env.transformEnv(env));
  }

}
