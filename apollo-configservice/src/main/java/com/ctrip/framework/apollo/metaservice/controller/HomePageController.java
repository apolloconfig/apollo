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
package com.ctrip.framework.apollo.metaservice.controller;

import com.ctrip.framework.apollo.core.ServiceNameConsts;
import com.ctrip.framework.apollo.core.dto.ServiceDTO;
import com.ctrip.framework.apollo.metaservice.service.DiscoveryService;
import com.google.common.collect.Lists;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * For non-eureka discovery services such as kubernetes and nacos, there is no eureka home page, so we need to add a default one
 */
@Profile({
    "kubernetes",
    "nacos-discovery",
    "consul-discovery",
    "zookeeper-discovery",
    "custom-defined-discovery",
    "database-discovery",
})
@RestController
public class HomePageController {
  private final DiscoveryService discoveryService;

  public HomePageController(DiscoveryService discoveryService) {
    this.discoveryService = discoveryService;
  }

  @GetMapping("/")
  public List<ServiceDTO> listAllServices() {
    List<ServiceDTO> allServices = Lists.newLinkedList();
    allServices
        .addAll(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_CONFIGSERVICE));
    allServices.addAll(discoveryService.getServiceInstances(ServiceNameConsts.APOLLO_ADMINSERVICE));

    return allServices;
  }
}
