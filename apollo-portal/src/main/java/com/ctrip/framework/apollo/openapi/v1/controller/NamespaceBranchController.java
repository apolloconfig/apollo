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
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.auth.ConsumerPermissionValidator;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.model.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.server.service.ReleaseOpenApiService;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.listener.ConfigPublishEvent;
import com.ctrip.framework.apollo.portal.service.NamespaceBranchService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController("openapiNamespaceBranchController")
@RequestMapping("/openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}")
public class NamespaceBranchController {

    private final ConsumerPermissionValidator consumerPermissionValidator;
    private final ReleaseOpenApiService releaseOpenApiService;
    private final NamespaceBranchService namespaceBranchService;
    private final UserService userService;
    private final ApplicationEventPublisher publisher;
    private final PortalConfig portalConfig;
    private final UserInfoHolder userInfoHolder;

    public NamespaceBranchController(
            final ConsumerPermissionValidator consumerPermissionValidator,
            final ReleaseOpenApiService releaseOpenApiService,
            final NamespaceBranchService namespaceBranchService,
            final UserService userService,
            final ApplicationEventPublisher publisher,
            final PortalConfig portalConfig,
            final UserInfoHolder userInfoHolder) {
        this.consumerPermissionValidator = consumerPermissionValidator;
        this.releaseOpenApiService = releaseOpenApiService;
        this.namespaceBranchService = namespaceBranchService;
        this.userService = userService;
        this.publisher = publisher;
        this.portalConfig = portalConfig;
        this.userInfoHolder = userInfoHolder;
    }

    /**
     * 获取命名空间分支信息
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches
     */
    @GetMapping("/branches")
    public ResponseEntity<OpenNamespaceDTO> findBranch(@PathVariable String appId,
                                                      @PathVariable String env,
                                                      @PathVariable String clusterName,
                                                      @PathVariable String namespaceName) {
        NamespaceBO namespaceBO = namespaceBranchService.findBranch(appId, Env.valueOf(env.toUpperCase()), clusterName, namespaceName);
        if (namespaceBO == null) {
            throw new BadRequestException("Namespace branch not found");
        }
        return ResponseEntity.ok(OpenApiBeanUtils.transformFromNamespaceBO(namespaceBO));
    }

    /**
     * 创建命名空间分支
     * POST /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches
     * 原先operator是必选，现在从authorization可以获取，为了保持兼容性留下但改为非必须
     */
    @PreAuthorize(value = "@consumerPermissionValidator.hasCreateNamespacePermission(#appId)")
    @PostMapping("/branches")
    @ApolloAuditLog(type = OpType.CREATE, name = "NamespaceBranch.create")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<OpenNamespaceDTO> createBranch(@PathVariable String appId,
                                                         @PathVariable String env,
                                                         @PathVariable String clusterName,
                                                         @PathVariable String namespaceName,
                                                         @RequestParam(value = "operator", required = false) String operator) {
        operator = userInfoHolder.getUser().getUserId();

        if (userService.findByUserId(operator) == null) {
            throw BadRequestException.userNotExists(operator);
        }

        NamespaceDTO namespaceDTO = namespaceBranchService.createBranch(appId, Env.valueOf(env.toUpperCase()), clusterName, namespaceName, operator);
        if (namespaceDTO == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
        OpenNamespaceDTO result = BeanUtils.transform(OpenNamespaceDTO.class, namespaceDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * 删除命名空间分支
     * DELETE /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}
     */
    @PreAuthorize(value = "@consumerPermissionValidator.hasModifyNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
    @DeleteMapping("/branches/{branchName}")
    @ApolloAuditLog(type = OpType.DELETE, name = "NamespaceBranch.delete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> deleteBranch(@PathVariable String env,
                                             @PathVariable String appId,
                                             @PathVariable String clusterName,
                                             @PathVariable String namespaceName,
                                             @PathVariable String branchName,
                                             @RequestParam(value = "operator", required = false) String operator) {
        operator = userInfoHolder.getUser().getUserId();

        if (userService.findByUserId(operator) == null) {
            throw BadRequestException.userNotExists(operator);
        }

        boolean canDelete = consumerPermissionValidator.hasReleaseNamespacePermission(appId, env, clusterName, namespaceName) ||
            (consumerPermissionValidator.hasModifyNamespacePermission(appId, env, clusterName, namespaceName) &&
                    releaseOpenApiService.getLatestActiveRelease(appId, env, branchName, namespaceName) == null);

        if (!canDelete) {
            throw new AccessDeniedException("Forbidden operation. "
                + "Caused by: 1.you don't have release permission "
                + "or 2. you don't have modification permission "
                + "or 3. you have modification permission but branch has been released");
        }
        namespaceBranchService.deleteBranch(appId, Env.valueOf(env.toUpperCase()), clusterName, namespaceName, branchName, operator);
        return ResponseEntity.noContent().build();
    }

    /**
     * 合并分支到主分支
     * PATCH /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}
     * 使用 PATCH 方法表示部分更新操作（将分支状态从"独立"更新为"合并"）
     */
    @PreAuthorize(value = "@consumerPermissionValidator.hasReleaseNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
    @PatchMapping("/branches/{branchName}")
    @ApolloAuditLog(type = OpType.UPDATE, name = "NamespaceBranch.merge")
    public ResponseEntity<OpenReleaseDTO> mergeBranch(@PathVariable String env,
                                                      @PathVariable String appId,
                                                      @PathVariable String clusterName,
                                                      @PathVariable String namespaceName,
                                                      @PathVariable String branchName,
                                                      @RequestParam(value = "deleteBranch", defaultValue = "true") boolean deleteBranch,
                                                      @RequestBody NamespaceReleaseDTO model) {
        RequestPrecondition.checkArguments(!StringUtils.isContainEmpty(model.getReleaseTitle()),
            "releaseTitle can not be empty");

        if (Boolean.TRUE.equals(model.getIsEmergencyPublish()) && !portalConfig.isEmergencyPublishAllowed(Env.valueOf(env.toUpperCase()))) {
            throw new BadRequestException("Env: %s is not supported emergency publish now", env);
        }

        ReleaseDTO createdRelease = namespaceBranchService.merge(appId, Env.valueOf(env.toUpperCase()), clusterName, namespaceName, branchName,
                                                                model.getReleaseTitle(), model.getReleaseComment(),
                                                                Boolean.TRUE.equals(model.getIsEmergencyPublish()), deleteBranch);

        ConfigPublishEvent event = ConfigPublishEvent.instance();
        event.withAppId(appId)
            .withCluster(clusterName)
            .withNamespace(namespaceName)
            .withReleaseId(createdRelease.getId())
            .setMergeEvent(true)
            .setEnv(Env.valueOf(env.toUpperCase()));

        publisher.publishEvent(event);

        return ResponseEntity.ok(OpenApiBeanUtils.transformFromReleaseDTO(createdRelease));
    }

    /**
     * 获取分支灰度发布规则
     * GET /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules
     */
    @GetMapping("/branches/{branchName}/rules")
    public ResponseEntity<OpenGrayReleaseRuleDTO> getBranchGrayRules(@PathVariable String appId,
                                                                     @PathVariable String env,
                                                                     @PathVariable String clusterName,
                                                                     @PathVariable String namespaceName,
                                                                     @PathVariable String branchName) {
        GrayReleaseRuleDTO grayReleaseRuleDTO = namespaceBranchService.findBranchGrayRules(appId, Env.valueOf(env.toUpperCase()), clusterName, namespaceName, branchName);
        if (grayReleaseRuleDTO == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(OpenApiBeanUtils.transformFromGrayReleaseRuleDTO(grayReleaseRuleDTO));
    }

    /**
     * 更新分支灰度发布规则
     * PUT /openapi/v1/envs/{env}/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/branches/{branchName}/rules
     */
    @PreAuthorize(value = "@consumerPermissionValidator.hasReleaseNamespacePermission(#appId, #env, #clusterName, #namespaceName)")
    @PutMapping("/branches/{branchName}/rules")
    @ApolloAuditLog(type = OpType.UPDATE, name = "NamespaceBranch.updateBranchRules")
    public ResponseEntity<Void> updateBranchGrayRules(@PathVariable String appId,
                                                      @PathVariable String env,
                                                      @PathVariable String clusterName,
                                                      @PathVariable String namespaceName,
                                                      @PathVariable String branchName,
                                                      @RequestBody OpenGrayReleaseRuleDTO rules,
                                                      @RequestParam(value = "operator", required = false) String operator) {
        operator = userInfoHolder.getUser().getUserId();

        rules.setAppId(appId);
        rules.setClusterName(clusterName);
        rules.setNamespaceName(namespaceName);
        rules.setBranchName(branchName);

        GrayReleaseRuleDTO grayReleaseRuleDTO = OpenApiBeanUtils.transformToGrayReleaseRuleDTO(rules);
        namespaceBranchService
                .updateBranchGrayRules(appId, Env.valueOf(env.toUpperCase()), clusterName, namespaceName, branchName, grayReleaseRuleDTO, operator);

        return ResponseEntity.ok().build();
    }
}
