/*
 * Copyright 2022 Apollo Authors
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
import com.ctrip.framework.apollo.common.dto.ServerConfigDTO;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import java.util.List;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ServiceConfigController {

  private final ServerConfigService serverConfigService;

  public ServiceConfigController(final ServerConfigService serverConfigService) {
    this.serverConfigService = serverConfigService;
  }

  @GetMapping("/apps/serverConfig/findAllConfigService")
  public List<ServerConfigDTO> find() {
    List<ServerConfig> app = serverConfigService.findAll();

    return BeanUtils.batchTransform(ServerConfigDTO.class, app);
  }

  @PostMapping("/apps/serverConfig/create")
  public void create(@Valid @RequestBody ServerConfig dto) {
    serverConfigService.save(dto);
  }

  @PostMapping("/apps/serverConfig/update")
  public void update(@Valid @RequestBody ServerConfig dto) {
    serverConfigService.update(dto);
  }
}
