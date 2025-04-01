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


import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.service.ReleaseHistoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

@RestController
public class ReleaseHistoryController {

  private final ReleaseHistoryService releaseHistoryService;
  private final UserPermissionValidator userPermissionValidator;

  public ReleaseHistoryController(final ReleaseHistoryService releaseHistoryService, final UserPermissionValidator userPermissionValidator) {
    this.releaseHistoryService = releaseHistoryService;
    this.userPermissionValidator = userPermissionValidator;
  }

  @GetMapping("/apps/{appId}/envs/{env}/clusters/{clusterName}/namespaces/{namespaceName}/releases/histories")
  public List<ReleaseHistoryBO> findReleaseHistoriesByNamespace(@PathVariable String appId,
                                                                @PathVariable String env,
                                                                @PathVariable String clusterName,
                                                                @PathVariable String namespaceName,
                                                                @RequestParam(value = "page", defaultValue = "0") int page,
                                                                @RequestParam(value = "size", defaultValue = "10") int size) {

    if (userPermissionValidator.shouldHideConfigToCurrentUser(appId, env, clusterName, namespaceName)) {
      return Collections.emptyList();
    }

   return releaseHistoryService.findNamespaceReleaseHistory(appId, Env.valueOf(env), clusterName ,namespaceName, page, size);
  }

}
