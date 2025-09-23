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

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.model.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.server.service.ClusterOpenApiService;
import com.ctrip.framework.apollo.openapi.server.service.impl.ServerClusterOpenApiService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Objects;

@RestController("openapiClusterController")
@RequestMapping("/openapi/v1/envs/{env}")
public class ClusterController {

  private final UserService userService;
  private final ClusterOpenApiService clusterOpenApiService;
  private final ServerClusterOpenApiService serverClusterOpenApiService;

  public ClusterController(
      UserService userService,
      ClusterOpenApiService clusterOpenApiService,
      ServerClusterOpenApiService serverClusterOpenApiService) {
    this.userService = userService;
    this.clusterOpenApiService = clusterOpenApiService;
    this.serverClusterOpenApiService = serverClusterOpenApiService;
  }

  /**
   * 获取指定集群信息
   * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}
   */
  @GetMapping(value = "/apps/{appId}/clusters/{clusterName:.+}")
  public ResponseEntity<OpenClusterDTO> getCluster(@PathVariable String env,
                                                   @PathVariable String appId, 
                                                   @PathVariable String clusterName) {
    OpenClusterDTO cluster = this.clusterOpenApiService.getCluster(appId, env, clusterName);
    return ResponseEntity.ok(cluster);
  }


  /**
   * 创建集群
   * POST /openapi/v1/envs/{env}/apps/{appId}/clusters
   */
  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateClusterPermission(#appId)")
  @PostMapping(value = "/apps/{appId}/clusters")
  public ResponseEntity<OpenClusterDTO> createCluster(@PathVariable String env,
                                                      @PathVariable String appId, 
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

    OpenClusterDTO createdCluster = this.clusterOpenApiService.createCluster(env, cluster);
    return ResponseEntity.ok(createdCluster);
  }

  /**
   * 删除集群
   * DELETE /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}
   */
  @PreAuthorize(value = "@consumerPermissionValidator.isAppAdmin(#appId)")
  @DeleteMapping(value = "/apps/{appId}/clusters/{clusterName:.+}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Cluster.delete")
  public ResponseEntity<Void> deleteCluster(@PathVariable String env,
                                            @PathVariable String appId, 
                                            @PathVariable String clusterName) {
    serverClusterOpenApiService.deleteCluster(env, appId, clusterName);
    return ResponseEntity.ok().build();
  }

}
