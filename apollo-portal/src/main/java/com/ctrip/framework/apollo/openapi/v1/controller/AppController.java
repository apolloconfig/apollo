/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.v1.controller;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.api.AppOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenCreateAppDTO;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.portal.entity.model.AppModel;
import java.util.Arrays;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController("openapiAppController")
@RequestMapping("/openapi/v1")
public class AppController {

  private final ConsumerAuthUtil consumerAuthUtil;
  private final ConsumerService consumerService;
  private final AppOpenApiService appOpenApiService;

  public AppController(
      final ConsumerAuthUtil consumerAuthUtil,
      final ConsumerService consumerService,
      AppOpenApiService appOpenApiService) {
    this.consumerAuthUtil = consumerAuthUtil;
    this.consumerService = consumerService;
    this.appOpenApiService = appOpenApiService;
  }

  /**
   * @see com.ctrip.framework.apollo.portal.controller.AppController#create(AppModel)
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateApplicationPermission(#request)")
  @PostMapping(value = "/apps")
  public void createApp(
      @RequestBody OpenCreateAppDTO req,
      HttpServletRequest request
  ) {
    if (null == req.getAppId()) {
      throw new BadRequestException("AppId is null");
    }
    if (req.isAssignAppRoleToSelf()) {
      // create app and assign app role to this consumer
      this.appOpenApiService.createApp(req);
      // todo @Transactional
      long consumerId = this.consumerAuthUtil.retrieveConsumerId(request);
      consumerService.assignAppRoleToConsumer(consumerId, req.getAppId());
    } else {
      // only create app
      this.appOpenApiService.createApp(req);
    }
  }

  @GetMapping(value = "/apps/{appId}/envclusters")
  public List<OpenEnvClusterDTO> getEnvClusterInfo(@PathVariable String appId){
    return this.appOpenApiService.getEnvClusterInfo(appId);
  }

  @GetMapping("/apps")
  public List<OpenAppDTO> findApps(@RequestParam(value = "appIds", required = false) String appIds) {
    if (StringUtils.hasText(appIds)) {
      return this.appOpenApiService.getAppsInfo(Arrays.asList(appIds.split(",")));
    } else {
      return this.appOpenApiService.getAllApps();
    }
  }

  /**
   * @return which apps can be operated by open api
   */
  @GetMapping("/apps/authorized")
  public List<OpenAppDTO> findAppsAuthorized(HttpServletRequest request) {
    long consumerId = this.consumerAuthUtil.retrieveConsumerId(request);

    Set<String> appIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);

    return this.appOpenApiService.getAppsInfo(new ArrayList<>(appIds));
  }

}
