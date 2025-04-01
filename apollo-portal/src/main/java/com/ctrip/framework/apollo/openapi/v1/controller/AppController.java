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
import javax.transaction.Transactional;
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
  @Transactional
  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateApplicationPermission()")
  @PostMapping(value = "/apps")
  public void createApp(
      @RequestBody OpenCreateAppDTO req
  ) {
    if (null == req.getApp()) {
      throw new BadRequestException("App is null");
    }
    final OpenAppDTO app = req.getApp();
    if (null == app.getAppId()) {
      throw new BadRequestException("AppId is null");
    }
    // create app
    this.appOpenApiService.createApp(req);
    if (req.isAssignAppRoleToSelf()) {
      long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();
      consumerService.assignAppRoleToConsumer(consumerId, app.getAppId());
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
  public List<OpenAppDTO> findAppsAuthorized() {
    long consumerId = this.consumerAuthUtil.retrieveConsumerIdFromCtx();

    Set<String> appIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);

    return this.appOpenApiService.getAppsInfo(new ArrayList<>(appIds));
  }

}
