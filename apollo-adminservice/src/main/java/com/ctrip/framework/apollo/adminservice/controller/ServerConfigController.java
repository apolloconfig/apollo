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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.ServerConfig;
import com.ctrip.framework.apollo.biz.service.ServerConfigService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author kl (http://kailing.pub)
 * @since 2022/12/13
 */
@RestController
public class ServerConfigController {
  private final ServerConfigService serverConfigService;
  public ServerConfigController(ServerConfigService serverConfigService) {
    this.serverConfigService = serverConfigService;
  }
  @GetMapping("/server/config/find-all-config")
  public List<ServerConfig> findAllServerConfig() {
    return serverConfigService.findAll();
  }

  @PostMapping("/server/config")
  public ServerConfig createOrUpdatePortalDBConfig(@Valid @RequestBody ServerConfig serverConfig) {
    return serverConfigService.createOrUpdateConfig(serverConfig);
  }
}
