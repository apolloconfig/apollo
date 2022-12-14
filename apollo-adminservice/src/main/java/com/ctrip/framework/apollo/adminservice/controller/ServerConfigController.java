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
