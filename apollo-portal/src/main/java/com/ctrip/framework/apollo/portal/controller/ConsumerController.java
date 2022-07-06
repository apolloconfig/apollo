/*
 * Copyright 2022 Apollo Authors
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

import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RestController
public class ConsumerController {

  private static final Date DEFAULT_EXPIRES = new GregorianCalendar(2099, Calendar.JANUARY, 1).getTime();

  private final ConsumerService consumerService;

  public ConsumerController(final ConsumerService consumerService) {
    this.consumerService = consumerService;
  }


  @Transactional
  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @PostMapping(value = "/consumers")
  public ConsumerToken createConsumer(@RequestBody Consumer consumer,
                                      @RequestParam(value = "expires", required = false)
                                      @DateTimeFormat(pattern = "yyyyMMddHHmmss") Date
                                          expires) {

    if (StringUtils.isContainEmpty(consumer.getAppId(), consumer.getName(),
                                   consumer.getOwnerName(), consumer.getOrgId())) {
      throw new BadRequestException("Params(appId、name、ownerName、orgId) can not be empty.");
    }

    Consumer createdConsumer = consumerService.createConsumer(consumer);

    if (Objects.isNull(expires)) {
      expires = DEFAULT_EXPIRES;
    }

    return consumerService.generateAndSaveConsumerToken(createdConsumer, expires);
  }

  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @GetMapping(value = "/consumer-tokens/by-appId")
  public ConsumerToken getConsumerTokenByAppId(@RequestParam String appId) {
    return consumerService.getConsumerTokenByAppId(appId);
  }

  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  @PostMapping(value = "/consumers/{token}/assign-role")
  public List<ConsumerRole> assignNamespaceRoleToConsumer(@PathVariable String token,
                                                          @RequestParam String type,
                                                          @RequestParam(required = false) String envs,
                                                          @RequestBody NamespaceDTO namespace) {

    String appId = namespace.getAppId();
    String namespaceName = namespace.getNamespaceName();

    if (StringUtils.isEmpty(appId)) {
      throw new BadRequestException("Params(AppId) can not be empty.");
    }
    if (Objects.equals("AppRole", type)) {
      return Collections.singletonList(consumerService.assignAppRoleToConsumer(token, appId));
    }
    if (StringUtils.isEmpty(namespaceName)) {
      throw new BadRequestException("Params(NamespaceName) can not be empty.");
    }
    if (null != envs){
      String[] envArray = envs.split(",");
      List<String> envList = Lists.newArrayList();
      // validate env parameter
      for (String env : envArray) {
        if (Strings.isNullOrEmpty(env)) {
          continue;
        }
        if (Env.UNKNOWN.equals(Env.transformEnv(env))) {
          throw new BadRequestException(String.format("env: %s is illegal", env));
        }
        envList.add(env);
      }

      List<ConsumerRole> consumeRoles = new ArrayList<>();
      for (String env : envList) {
        consumeRoles.addAll(consumerService.assignNamespaceRoleToConsumer(token, appId, namespaceName, env));
      }
      return consumeRoles;
    }

    return consumerService.assignNamespaceRoleToConsumer(token, appId, namespaceName);
  }

  @GetMapping("/consumers")
  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  public List<Consumer> getConsumerList(Pageable page){
    return consumerService.findAllConsumer(page);
  }

  @DeleteMapping(value = "/consumers/by-appId")
  @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
  public void deleteConsumers(@RequestParam String appId) {
    consumerService.deleteConsumer(appId);
  }

}
