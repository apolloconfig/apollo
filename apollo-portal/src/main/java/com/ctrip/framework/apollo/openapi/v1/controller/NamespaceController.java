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
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.openapi.api.NamespaceOpenApiService;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Objects;

@RestController("openapiNamespaceController")
public class NamespaceController {

  private final UserService userService;
  private final NamespaceOpenApiService namespaceOpenApiService;

  public NamespaceController(
      final UserService userService,
      NamespaceOpenApiService namespaceOpenApiService) {
    this.userService = userService;
    this.namespaceOpenApiService = namespaceOpenApiService;
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasCreateNamespacePermission(#appId)")
  @PostMapping(value = "/openapi/v1/apps/{appId}/appnamespaces")
  public OpenAppNamespaceDTO createNamespace(@PathVariable String appId,
                                             @RequestBody OpenAppNamespaceDTO appNamespaceDTO) {

    if (!Objects.equals(appId, appNamespaceDTO.getAppId())) {
      throw new BadRequestException("AppId not equal. AppId in path = %s, AppId in payload = %s", appId,
                                                  appNamespaceDTO.getAppId());
    }
    RequestPrecondition.checkArgumentsNotEmpty(appNamespaceDTO.getAppId(), appNamespaceDTO.getName(),
                                               appNamespaceDTO.getFormat(), appNamespaceDTO.getDataChangeCreatedBy());

    if (!InputValidator.isValidAppNamespace(appNamespaceDTO.getName())) {
      throw BadRequestException.invalidNamespaceFormat(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE + " & "
                                                  + InputValidator.INVALID_NAMESPACE_NAMESPACE_MESSAGE);
    }

    if (!ConfigFileFormat.isValidFormat(appNamespaceDTO.getFormat())) {
      throw BadRequestException.invalidNamespaceFormat(appNamespaceDTO.getFormat());
    }

    String operator = appNamespaceDTO.getDataChangeCreatedBy();
    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }

    return this.namespaceOpenApiService.createAppNamespace(appNamespaceDTO);
  }

  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces")
  public List<OpenNamespaceDTO> findNamespaces(@PathVariable String appId, @PathVariable String env,
                                               @PathVariable String clusterName,
                                               @RequestParam(defaultValue = "true") boolean fillItemDetail) {
    return this.namespaceOpenApiService.getNamespaces(appId, env, clusterName, fillItemDetail);
  }

  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName:.+}")
  public OpenNamespaceDTO loadNamespace(@PathVariable String appId, @PathVariable String env,
                                        @PathVariable String clusterName, @PathVariable String namespaceName,
                                        @RequestParam(defaultValue = "true") boolean fillItemDetail) {
    return this.namespaceOpenApiService.getNamespace(appId, env, clusterName, namespaceName, fillItemDetail);
  }

  @GetMapping(value = "/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock")
  public OpenNamespaceLockDTO getNamespaceLock(@PathVariable String appId, @PathVariable String env,
                                               @PathVariable String clusterName, @PathVariable
                                               String namespaceName) {
    return this.namespaceOpenApiService.getNamespaceLock(appId, env, clusterName, namespaceName);
  }

}
