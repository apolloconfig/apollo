package com.ctrip.framework.apollo.metaservice.controller;

import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.metaservice.service.DiscoveryService;
import java.util.Collections;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/services")
public class ServiceController {

  private final DiscoveryService discoveryService;

  public ServiceController(final DiscoveryService discoveryService) {
    this.discoveryService = discoveryService;
  }

  /**
   * This method always return an empty list as meta service is not used at all
   */
  @Deprecated
  @RequestMapping("/meta")
  public List<ServiceDTO> getMetaService() {
    return Collections.emptyList();
  }

  @RequestMapping("/config")
  public List<ServiceDTO> getConfigService(
      @RequestParam(value = "appId", defaultValue = "") String appId,
      @RequestParam(value = "ip", required = false) String clientIp) {
    return discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE);
  }

  @RequestMapping("/admin")
  public List<ServiceDTO> getAdminService() {
    return discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_ADMINSERVICE);
  }
}
