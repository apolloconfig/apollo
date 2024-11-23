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

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.openapi.api.ReleaseOpenApiService;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.dto.NamespaceGrayDelReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceGrayDelReleaseModel;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController("openapiReleaseController")
@RequestMapping("/openapi/v1/envs/{env}")
public class ReleaseController {

  private final ReleaseService releaseService;
  private final UserService userService;
  private final NamespaceBranchService namespaceBranchService;
  private final ConsumerPermissionValidator consumerPermissionValidator;
  private final ReleaseOpenApiService releaseOpenApiService;

  public ReleaseController(
      final ReleaseService releaseService,
      final UserService userService,
      final NamespaceBranchService namespaceBranchService,
      final ConsumerPermissionValidator consumerPermissionValidator,
      ReleaseOpenApiService releaseOpenApiService) {
    this.releaseService = releaseService;
    this.userService = userService;
    this.namespaceBranchService = namespaceBranchService;
    this.consumerPermissionValidator = consumerPermissionValidator;
    this.releaseOpenApiService = releaseOpenApiService;
  }

  @PreAuthorize(value = "@consumerPermissionValidator.hasReleaseNamespacePermission(#request, #appId, #namespaceName, #env)")
  @PostMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases")
  public OpenReleaseDTO createRelease(@PathVariable String appId, @PathVariable String env,
                                      @PathVariable String clusterName,
                                      @PathVariable String namespaceName,
                                      @RequestBody NamespaceReleaseDTO model,
                                      HttpServletRequest request) {
      boolean doesContainEmpty = StringUtils.isContainEmpty(model.getReleasedBy(), model.getReleaseTitle());
      RequestPrecondition.checkArguments(!doesContainEmpty, "Params(releaseTitle and releasedBy) can not be empty");

    if (userService.findByUserId(model.getReleasedBy()) == null) {
      throw BadRequestException.userNotExists(model.getReleasedBy());
    }

    return this.releaseOpenApiService.publishNamespace(appId, env, clusterName, namespaceName, model);
  }

  @GetMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/latest")
  public OpenReleaseDTO loadLatestActiveRelease(@PathVariable String appId, @PathVariable String env,
                                                @PathVariable String clusterName, @PathVariable
                                                    String namespaceName) {
    return this.releaseOpenApiService.getLatestActiveRelease(appId, env, clusterName, namespaceName);
  }

    @PreAuthorize(value = "@consumerPermissionValidator.hasReleaseNamespacePermission(#request, #appId, #namespaceName, #env)")
    @PostMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/merge")
    public OpenReleaseDTO merge(@PathVariable String appId, @PathVariable String env,
                            @PathVariable String clusterName, @PathVariable String namespaceName,
                            @PathVariable String branchName, @RequestParam(value = "deleteBranch", defaultValue = "true") boolean deleteBranch,
                            @RequestBody NamespaceReleaseDTO model, HttpServletRequest request) {
        boolean doesContainEmpty = StringUtils.isContainEmpty(model.getReleasedBy(), model.getReleaseTitle());
        RequestPrecondition.checkArguments(!doesContainEmpty, "Params(releaseTitle and releasedBy) can not be empty");

        if (userService.findByUserId(model.getReleasedBy()) == null) {
            throw BadRequestException.userNotExists(model.getReleasedBy());
        }

        ReleaseDTO mergedRelease = namespaceBranchService.merge(appId, Env.valueOf(env.toUpperCase()), clusterName, namespaceName, branchName,
                model.getReleaseTitle(), model.getReleaseComment(),
                model.isEmergencyPublish(), deleteBranch, model.getReleasedBy());

        return OpenApiBeanUtils.transformFromReleaseDTO(mergedRelease);
    }

    @PreAuthorize(value = "@consumerPermissionValidator.hasReleaseNamespacePermission(#request, #appId, #namespaceName, #env)")
    @PostMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/releases")
    public OpenReleaseDTO createGrayRelease(@PathVariable String appId,
                                        @PathVariable String env, @PathVariable String clusterName,
                                        @PathVariable String namespaceName, @PathVariable String branchName,
                                        @RequestBody NamespaceReleaseDTO model,
                                        HttpServletRequest request) {
        boolean doesContainEmpty = StringUtils.isContainEmpty(model.getReleasedBy(), model.getReleaseTitle());
        RequestPrecondition.checkArguments(!doesContainEmpty, "Params(releaseTitle and releasedBy) can not be empty");

        if (userService.findByUserId(model.getReleasedBy()) == null) {
            throw BadRequestException.userNotExists(model.getReleasedBy());
        }

        NamespaceReleaseModel releaseModel = BeanUtils.transform(NamespaceReleaseModel.class, model);
        populateReleaseModel(releaseModel, appId, Env.valueOf(env).toString(), branchName, namespaceName);

        return OpenApiBeanUtils.transformFromReleaseDTO(releaseService.publish(releaseModel));
    }

    @PreAuthorize(value = "@consumerPermissionValidator.hasReleaseNamespacePermission(#request, #appId, #namespaceName, #env)")
    @PostMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/gray-del-releases")
    public OpenReleaseDTO createGrayDelRelease(@PathVariable String appId,
                                               @PathVariable String env, @PathVariable String clusterName,
                                               @PathVariable String namespaceName, @PathVariable String branchName,
                                               @RequestBody NamespaceGrayDelReleaseDTO model,
                                               HttpServletRequest request) {
        boolean doesContainEmpty = StringUtils.isContainEmpty(model.getReleasedBy(), model.getReleaseTitle());
        RequestPrecondition.checkArguments(!doesContainEmpty, "Params(releaseTitle and releasedBy) can not be empty");

        RequestPrecondition.checkArguments(model.getGrayDelKeys() != null,
                "Params(grayDelKeys) can not be null");

        if (userService.findByUserId(model.getReleasedBy()) == null) {
            throw BadRequestException.userNotExists(model.getReleasedBy());
        }

        NamespaceGrayDelReleaseModel releaseModel = BeanUtils.transform(NamespaceGrayDelReleaseModel.class, model);
        populateReleaseModel(releaseModel, appId, env.toUpperCase(), branchName, namespaceName);

        ReleaseDTO publish = releaseService.publish(releaseModel, releaseModel.getReleasedBy());
        return OpenApiBeanUtils.transformFromReleaseDTO(publish);
    }

  @PutMapping(path = "/releases/{releaseId}/rollback")
  public void rollback(@PathVariable String env,
      @PathVariable long releaseId, @RequestParam String operator, HttpServletRequest request) {
     boolean doesContainEmpty = StringUtils.isContainEmpty(operator);
     RequestPrecondition.checkArguments(!doesContainEmpty, "Param operator can not be empty");

    if (userService.findByUserId(operator) == null) {
      throw BadRequestException.userNotExists(operator);
    }

    ReleaseDTO release = releaseService.findReleaseById(Env.valueOf(env), releaseId);

    if (release == null) {
      throw new BadRequestException("release not found");
    }

    if (!consumerPermissionValidator.hasReleaseNamespacePermission(request,release.getAppId(), release.getNamespaceName(), env)) {
      throw new AccessDeniedException("Forbidden operation. you don't have release permission");
    }

    this.releaseOpenApiService.rollbackRelease(env, releaseId, operator);
  }

    private void populateReleaseModel(NamespaceReleaseModel releaseModel, String appId, String env, String branchName, String namespaceName) {
        releaseModel.setAppId(appId);
        releaseModel.setEnv(env.toUpperCase());
        releaseModel.setClusterName(branchName);
        releaseModel.setNamespaceName(namespaceName);
    }

}
