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

import com.ctrip.framework.apollo.common.dto.CommitDTO;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.service.CommitService;
import javax.validation.Valid;
import javax.validation.constraints.Positive;
import javax.validation.constraints.PositiveOrZero;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@Validated
@RestController
public class CommitController {

  private final CommitService commitService;
  private final UserPermissionValidator userPermissionValidator;

  public CommitController(final CommitService commitService, final UserPermissionValidator userPermissionValidator) {
    this.commitService = commitService;
    this.userPermissionValidator = userPermissionValidator;
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/commits")
  public List<CommitDTO> find(@PathVariable String appId, @PathVariable String env,
                              @PathVariable String clusterName, @PathVariable String namespaceName,
                              @RequestParam(required = false) String key,
                              @Valid @PositiveOrZero(message = "page should be positive or 0") @RequestParam(defaultValue = "0") int page,
                              @Valid @Positive(message = "size should be positive number") @RequestParam(defaultValue = "10") int size) {
    if (userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return Collections.emptyList();
    }

    if (StringUtils.isEmpty(key)) {
      return commitService.find(appId, Env.valueOf(env), clusterName, namespaceName, page, size);
    } else {
      return commitService.findByKey(appId, Env.valueOf(env), clusterName, namespaceName, key, page, size);
    }

  }
}
