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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
public class ClusterController {

  private final ClusterService clusterService;
  private final UserInfoHolder userInfoHolder;

  public ClusterController(final ClusterService clusterService, final UserInfoHolder userInfoHolder) {
    this.clusterService = clusterService;
    this.userInfoHolder = userInfoHolder;
  }

  @PreAuthorize(value = "@userPermissionValidator.hasCreateClusterPermission(#appId)")
  @PostMapping(value = "apps/{appId}/envs/{env}/clusters")
  @ApolloAuditLog(type = OpType.CREATE, name = "Cluster.create")
  public ClusterDTO createCluster(@PathVariable String appId, @PathVariable String env,
                                  @Valid @RequestBody ClusterDTO cluster) {
    String operator = userInfoHolder.getUser().getUserId();
    cluster.setDataChangeLastModifiedBy(operator);
    cluster.setDataChangeCreatedBy(operator);

    return clusterService.createCluster(Env.valueOf(env), cluster);
  }

  @PreAuthorize(value = "@userPermissionValidator.isSuperAdmin()")
  @DeleteMapping(value = "apps/{appId}/envs/{env}/clusters/{clusterName:.+}")
  @ApolloAuditLog(type = OpType.DELETE, name = "Cluster.delete")
  public ResponseEntity<Void> deleteCluster(@PathVariable String appId, @PathVariable String env,
                                            @PathVariable String clusterName){
    clusterService.deleteCluster(Env.valueOf(env), appId, clusterName);
    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "apps/{appId}/envs/{env}/clusters/{clusterName:.+}")
  public ClusterDTO loadCluster(@PathVariable("appId") String appId, @PathVariable String env, @PathVariable("clusterName") String clusterName) {

    return clusterService.loadCluster(appId, Env.valueOf(env), clusterName);
  }

}
