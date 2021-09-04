/*
 * Copyright 2021 Apollo Authors
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

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.openapi.api.ApolloAppOpenApi;
import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.openapi.dto.OpenAppDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenEnvClusterDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.google.common.collect.Sets;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@RestController("openapiAppController")
@RequestMapping("/openapi/v1")
public class AppController implements ApolloAppOpenApi {

  private final PortalSettings portalSettings;
  private final ClusterService clusterService;
  private final AppService appService;
  private final ConsumerAuthUtil consumerAuthUtil;
  private final ConsumerService consumerService;

  public AppController(final PortalSettings portalSettings,
      final ClusterService clusterService,
      final AppService appService,
      final ConsumerAuthUtil consumerAuthUtil,
      final ConsumerService consumerService) {
    this.portalSettings = portalSettings;
    this.clusterService = clusterService;
    this.appService = appService;
    this.consumerAuthUtil = consumerAuthUtil;
    this.consumerService = consumerService;
  }

  @GetMapping(value = "/apps/{appId}/envclusters")
  public List<OpenEnvClusterDTO> getEnvClusterInfo(@PathVariable String appId){

    List<OpenEnvClusterDTO> envClusters = new LinkedList<>();

    List<Env> envs = portalSettings.getActiveEnvs();
    for (Env env : envs) {
      OpenEnvClusterDTO envCluster = new OpenEnvClusterDTO();

      envCluster.setEnv(env.name());
      List<ClusterDTO> clusterDTOs = clusterService.findClusters(env, appId);
      envCluster.setClusters(BeanUtils.toPropertySet("name", clusterDTOs));

      envClusters.add(envCluster);
    }

    return envClusters;

  }

  @GetMapping("/apps")
  @Override
  public List<OpenAppDTO> getAllApps() {
    final List<App> apps = appService.findAll();
    return OpenApiBeanUtils.transformFromApps(apps);
  }

  @GetMapping(value = "/apps", params = "appIds")
  @Override
  public List<OpenAppDTO> getAppsByIds(@RequestParam(value = "appIds") List<String> appIds) {
    final List<App> apps = appService.findByAppIds(Sets.newHashSet(appIds));
    return OpenApiBeanUtils.transformFromApps(apps);
  }

  /**
   * @return which apps can be operated by open api
   */
  @GetMapping("/apps/authorized")
  public List<OpenAppDTO> getAuthorizedApps() {
    HttpServletRequest httpServletRequest = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
    long consumerId = this.consumerAuthUtil.retrieveConsumerId(httpServletRequest);

    Set<String> appIds = this.consumerService.findAppIdsAuthorizedByConsumerId(consumerId);

    List<App> apps = this.appService.findByAppIds(appIds);
    List<OpenAppDTO> openAppDTOS = OpenApiBeanUtils.transformFromApps(apps);
    return openAppDTOS;
  }

}
