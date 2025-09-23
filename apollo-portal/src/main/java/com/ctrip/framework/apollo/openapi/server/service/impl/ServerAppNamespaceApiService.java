/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ctrip.framework.apollo.openapi.server.service.impl;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.ctrip.framework.apollo.openapi.server.service.AppNamespaceOpenApiService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.listener.AppNamespaceCreationEvent;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;

@Service
public class ServerAppNamespaceApiService implements AppNamespaceOpenApiService {

    private final AppNamespaceService appNamespaceService;
    private final UserInfoHolder userInfoHolder;
    private final ApplicationEventPublisher publisher;

    public ServerAppNamespaceApiService(
        AppNamespaceService appNamespaceService,
        UserInfoHolder userInfoHolder,
        ApplicationEventPublisher publisher) {
        this.appNamespaceService = appNamespaceService;
        this.userInfoHolder = userInfoHolder;
        this.publisher = publisher;
    }

    /**
     * 获取所有公共的 App Namespace
     * 公共的 app ns，能被其它项目关联到的 app ns
     *
     * @return 公共 App Namespace 列表
     */
    @Override
    public List<OpenAppNamespaceDTO> findPublicAppNamespaces() {
        List<AppNamespace> appNamespaces = appNamespaceService.findPublicAppNamespaces();
        if (appNamespaces == null || appNamespaces.isEmpty()) {
            return Collections.emptyList();
        }
        return appNamespaces.stream()
                .map(OpenApiBeanUtils::transformToOpenAppNamespaceDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据名称查找公共 App Namespace
     *
     * @param namespaceName namespace 名称
     * @return 公共 App Namespace，如果不存在返回 null
     */
    @Override
    public OpenAppNamespaceDTO findPublicAppNamespace(String namespaceName) {
        AppNamespace appNamespace = appNamespaceService.findPublicAppNamespace(namespaceName);
        if (appNamespace == null) {
            return null;
        }
        return OpenApiBeanUtils.transformToOpenAppNamespaceDTO(appNamespace);
    }

    /**
     * 根据 appId 和 namespace 名称查找 App Namespace
     *
     * @param appId         app ID
     * @param namespaceName namespace 名称
     * @return App Namespace，如果不存在返回 null
     */
    @Override
    public OpenAppNamespaceDTO findByAppIdAndName(String appId, String namespaceName) {
        AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(appId, namespaceName);
        if (appNamespace == null) {
            return null;
        }
        return OpenApiBeanUtils.transformToOpenAppNamespaceDTO(appNamespace);
    }

    /**
     * 根据 appId 查找所有 App Namespace
     *
     * @param appId app ID
     * @return App Namespace 列表
     */
    @Override
    public List<OpenAppNamespaceDTO> findByAppId(String appId) {
        List<AppNamespace> appNamespaces = appNamespaceService.findByAppId(appId);
        if (appNamespaces == null || appNamespaces.isEmpty()) {
            return Collections.emptyList();
        }
        return appNamespaces.stream()
                .map(OpenApiBeanUtils::transformToOpenAppNamespaceDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取所有 App Namespace
     *
     * @return 所有 App Namespace 列表
     */
    @Override
    public List<OpenAppNamespaceDTO> findAll() {
        List<AppNamespace> appNamespaces = appNamespaceService.findAll();
        if (appNamespaces == null || appNamespaces.isEmpty()) {
            return Collections.emptyList();
        }
        return appNamespaces.stream()
                .map(OpenApiBeanUtils::transformToOpenAppNamespaceDTO)
                .collect(Collectors.toList());
    }

    /**
     * 为指定 App 创建默认的 App Namespace (application)
     *
     * @param appId app ID
     */
    @Override
    public void createDefaultAppNamespace(String appId) {
        appNamespaceService.createDefaultAppNamespace(appId);
    }

    /**
     * 检查 App Namespace 名称是否唯一
     *
     * @param appId         app ID
     * @param namespaceName namespace 名称
     * @return true 如果名称唯一，false 如果已存在
     */
    @Override
    public boolean isAppNamespaceNameUnique(String appId, String namespaceName) {
        return appNamespaceService.isAppNamespaceNameUnique(appId, namespaceName);
    }

    /**
     * 创建 App Namespace
     *
     * @param appNamespace App Namespace 信息
     * @return 创建成功的 App Namespace
     */
    @Override
    public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace) {
        return createAppNamespace(appNamespace, true);
    }

    /**
     * 创建 App Namespace（可指定是否添加命名空间前缀）
     *
     * @param appNamespace          App Namespace 信息
     * @param appendNamespacePrefix 是否添加命名空间前缀
     * @return 创建成功的 App Namespace
     */
    @Override
    public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace, boolean appendNamespacePrefix) {
        AppNamespace entity = OpenApiBeanUtils.transformToAppNamespace(appNamespace);
        AppNamespace createdAppNamespace = appNamespaceService.createAppNamespaceInLocal(entity, appendNamespacePrefix);
        
        // 发布创建事件
        publisher.publishEvent(new AppNamespaceCreationEvent(createdAppNamespace));
        
        return OpenApiBeanUtils.transformToOpenAppNamespaceDTO(createdAppNamespace);
    }

    /**
     * 导入 App Namespace
     *
     * @param appNamespace App Namespace 信息
     * @return 导入成功的 App Namespace
     */
    @Override
    public OpenAppNamespaceDTO importAppNamespace(OpenAppNamespaceDTO appNamespace) {
        AppNamespace entity = OpenApiBeanUtils.transformToAppNamespace(appNamespace);
        AppNamespace importedAppNamespace = appNamespaceService.importAppNamespaceInLocal(entity);
        
        return OpenApiBeanUtils.transformToOpenAppNamespaceDTO(importedAppNamespace);
    }

    /**
     * 删除 App Namespace
     *
     * @param appId         app ID
     * @param namespaceName namespace 名称
     * @return 被删除的 App Namespace
     */
    @Override
    public OpenAppNamespaceDTO deleteAppNamespace(String appId, String namespaceName) {
        AppNamespace deletedAppNamespace = appNamespaceService.deleteAppNamespace(appId, namespaceName);
        return OpenApiBeanUtils.transformToOpenAppNamespaceDTO(deletedAppNamespace);
    }

    /**
     * 根据 appId 批量删除 App Namespace
     *
     * @param appId app ID
     */
    @Override
    public void batchDeleteByAppId(String appId) {
        String operator = userInfoHolder.getUser().getUserId();
        appNamespaceService.batchDeleteByAppId(appId, operator);
    }
}
