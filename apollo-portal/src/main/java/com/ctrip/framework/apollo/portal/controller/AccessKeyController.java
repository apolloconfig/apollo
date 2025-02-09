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

import static com.ctrip.framework.apollo.common.constants.AccessKeyMode.FILTER;

import com.ctrip.framework.apollo.audit.annotation.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.common.dto.AccessKeyDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AccessKeyService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * @author nisiyong
 */
@RestController
public class AccessKeyController {

  private final UserInfoHolder userInfoHolder;
  private final AccessKeyService accessKeyService;

  public AccessKeyController(
      UserInfoHolder userInfoHolder,
      AccessKeyService accessKeyService) {
    this.userInfoHolder = userInfoHolder;
    this.accessKeyService = accessKeyService;
  }

  @PreAuthorize(value = "@userPermissionValidator.isAppAdmin(#appId)")
  @PostMapping(value = "/apps/{appId}/envs/{env}/accesskeys")
  @ApolloAuditLog(type = OpType.CREATE, name = "AccessKey.create")
  public AccessKeyDTO save(@PathVariable String appId, @PathVariable String env,
      @RequestBody AccessKeyDTO accessKeyDTO) {
    String secret = UUID.randomUUID().toString().replaceAll("-", "");
    accessKeyDTO.setAppId(appId);
    accessKeyDTO.setSecret(secret);
    return accessKeyService.createAccessKey(Env.valueOf(env), accessKeyDTO);
  }

  @PreAuthorize(value = "@userPermissionValidator.isAppAdmin(#appId)")
  @GetMapping(value = "/apps/{appId}/envs/{env}/accesskeys")
  public List<AccessKeyDTO> findByAppId(@PathVariable String appId,
      @PathVariable String env) {
    return accessKeyService.findByAppId(Env.valueOf(env), appId);
  }

  @PreAuthorize(value = "@userPermissionValidator.isAppAdmin(#appId)")
  @DeleteMapping(value = "/apps/{appId}/envs/{env}/accesskeys/{id}")
  @ApolloAuditLog(type = OpType.DELETE, name = "AccessKey.delete")
  public void delete(@PathVariable String appId,
      @PathVariable String env,
      @PathVariable long id) {
    String operator = userInfoHolder.getUser().getUserId();
    accessKeyService.deleteAccessKey(Env.valueOf(env), appId, id, operator);
  }

  @PreAuthorize(value = "@userPermissionValidator.isAppAdmin(#appId)")
  @PutMapping(value = "/apps/{appId}/envs/{env}/accesskeys/{id}/enable")
  @ApolloAuditLog(type = OpType.UPDATE, name = "AccessKey.enable")
  public void enable(@PathVariable String appId,
      @PathVariable String env,
      @PathVariable long id,
      @RequestParam(required = false, defaultValue = "" + FILTER) int mode) {
    String operator = userInfoHolder.getUser().getUserId();
    accessKeyService.enable(Env.valueOf(env), appId, id, mode, operator);
  }

  @PreAuthorize(value = "@userPermissionValidator.isAppAdmin(#appId)")
  @PutMapping(value = "/apps/{appId}/envs/{env}/accesskeys/{id}/disable")
  @ApolloAuditLog(type = OpType.UPDATE, name = "AccessKey.disable")
  public void disable(@PathVariable String appId,
      @PathVariable String env,
      @PathVariable long id) {
    String operator = userInfoHolder.getUser().getUserId();
    accessKeyService.disable(Env.valueOf(env), appId, id, operator);
  }
}
