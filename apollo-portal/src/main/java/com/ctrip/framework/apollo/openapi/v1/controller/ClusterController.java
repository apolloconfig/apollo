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
import com.ctrip.framework.apollo.openapi.server.service.ClusterOpenApiService;
import com.ctrip.framework.apollo.portal.component.UserIdentityContextHolder;
import com.ctrip.framework.apollo.portal.constant.UserIdentityConstants;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Objects;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.api.ClusterManagementApi;
import com.ctrip.framework.apollo.openapi.model.OpenClusterDTO;
import org.springframework.http.ResponseEntity;

@RestController("openapiClusterController")
public class ClusterController implements ClusterManagementApi {

  private final UserService userService;
  private final ClusterOpenApiService clusterOpenApiService;
  private final UserInfoHolder userInfoHolder;

  public ClusterController(
      UserService userService,
      ClusterOpenApiService clusterOpenApiService,
      UserInfoHolder userInfoHolder) {
    this.userService = userService;
    this.clusterOpenApiService = clusterOpenApiService;
    this.userInfoHolder = userInfoHolder;
  }

  @Override
  public ResponseEntity<OpenClusterDTO> getCluster(String appId, String clusterName, String env) {
    return ResponseEntity.ok(this.clusterOpenApiService.getCluster(appId, env, clusterName));
  }

  @PreAuthorize(value = "@unifiedPermissionValidator.hasCreateClusterPermission(#appId)")
  @Override
  public ResponseEntity<OpenClusterDTO> createCluster(String appId, String env,
      OpenClusterDTO cluster) {

    if (!Objects.equals(appId, cluster.getAppId())) {
      throw new BadRequestException(
          "AppId not equal. AppId in path = %s, AppId in payload = %s", appId, cluster.getAppId());
    }

    String clusterName = cluster.getName();
    String operator = null;
    if (UserIdentityConstants.USER.equals(UserIdentityContextHolder.getAuthType())) {
      operator = userInfoHolder.getUser().getUserId();
      cluster.setDataChangeLastModifiedBy(operator);
      cluster.setDataChangeCreatedBy(operator);
    } else {
      operator = cluster.getDataChangeCreatedBy();
    }

    RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(clusterName, operator),
        "name and dataChangeCreatedBy should not be null or empty");

    if (!InputValidator.isValidClusterNamespace(clusterName)) {
      throw BadRequestException.invalidClusterNameFormat(
          InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE);
    }

    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }

    return ResponseEntity.ok(this.clusterOpenApiService.createCluster(env, cluster));
  }

  /**
   * Delete Clusters
   */
  @PreAuthorize(value = "@unifiedPermissionValidator.isAppAdmin(#appId)")
  @ApolloAuditLog(type = OpType.DELETE, name = "Cluster.delete")
  @Override
  public ResponseEntity<Object> deleteCluster(String env, String appId, String clusterName,
      String operator) {
    if (UserIdentityConstants.CONSUMER.equals(UserIdentityContextHolder.getAuthType())) {
      RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(operator),
          "operator should not be null or empty");

      if (userService.findByUserId(operator) == null) {
        throw BadRequestException.userNotExists(operator);
      }
    }

    clusterOpenApiService.deleteCluster(env, appId, clusterName);
    return ResponseEntity.ok().build();
  }

}
