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

import com.ctrip.framework.apollo.openapi.api.ClusterOpenApiService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;

@RestController("openapiClusterController")
@RequestMapping("/openapi/v1/envs/{env}")
public class ClusterController {

  private final UserService userService;
  private final ClusterOpenApiService clusterOpenApiService;

  public ClusterController(
      UserService userService,
      ClusterOpenApiService clusterOpenApiService) {
    this.userService = userService;
    this.clusterOpenApiService = clusterOpenApiService;
  }

  @GetMapping(value = "apps/{appId}/clusters/{clusterName:.+}")
  public OpenClusterDTO getCluster(@PathVariable("appId") String appId, @PathVariable String env,
      @PathVariable("clusterName") String clusterName) {
    return this.clusterOpenApiService.getCluster(appId, env, clusterName);
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateClusterPermission(#appId)")
  @PostMapping(value = "apps/{appId}/clusters")
  public OpenClusterDTO createCluster(@PathVariable String appId, @PathVariable String env,
      @Valid @RequestBody OpenClusterDTO cluster) {

    if (!Objects.equals(appId, cluster.getAppId())) {
      throw new BadRequestException(
          "AppId not equal. AppId in path = %s, AppId in payload = %s", appId, cluster.getAppId());
    }

    String clusterName = cluster.getName();
    String operator = cluster.getDataChangeCreatedBy();

    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(clusterName, operator),
        "name and dataChangeCreatedBy should not be null or empty");

    if (!InputValidator.isValidClusterNamespace(clusterName)) {
      throw BadRequestException.invalidClusterNameFormat(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE);
    }

    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }

    return this.clusterOpenApiService.createCluster(env, cluster);
  }

}
