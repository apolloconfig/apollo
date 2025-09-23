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
package com.ctrip.framework.apollo.openapi.server.service;

import com.ctrip.framework.apollo.openapi.model.OpenAppNamespaceDTO;
import java.util.List;

/**
 * App Namespace Open API Service
 *
 * @author apollo
 */
public interface AppNamespaceOpenApiService {

    /**
     * 获取所有公共的 App Namespace
     * 公共的 app ns，能被其它项目关联到的 app ns
     *
     * @return 公共 App Namespace 列表
     */
    List<OpenAppNamespaceDTO> findPublicAppNamespaces();

    /**
     * 根据名称查找公共 App Namespace
     *
     * @param namespaceName namespace 名称
     * @return 公共 App Namespace，如果不存在返回 null
     */
    OpenAppNamespaceDTO findPublicAppNamespace(String namespaceName);

    /**
     * 根据 appId 和 namespace 名称查找 App Namespace
     *
     * @param appId app ID
     * @param namespaceName namespace 名称
     * @return App Namespace，如果不存在返回 null
     */
    OpenAppNamespaceDTO findByAppIdAndName(String appId, String namespaceName);

    /**
     * 根据 appId 查找所有 App Namespace
     *
     * @param appId app ID
     * @return App Namespace 列表
     */
    List<OpenAppNamespaceDTO> findByAppId(String appId);

    /**
     * 获取所有 App Namespace
     *
     * @return 所有 App Namespace 列表
     */
    List<OpenAppNamespaceDTO> findAll();

    /**
     * 为指定 App 创建默认的 App Namespace (application)
     *
     * @param appId app ID
     */
    void createDefaultAppNamespace(String appId);

    /**
     * 检查 App Namespace 名称是否唯一
     *
     * @param appId app ID
     * @param namespaceName namespace 名称
     * @return true 如果名称唯一，false 如果已存在
     */
    boolean isAppNamespaceNameUnique(String appId, String namespaceName);

    /**
     * 创建 App Namespace
     *
     * @param appNamespace App Namespace 信息
     * @return 创建成功的 App Namespace
     */
    OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace);

    /**
     * 创建 App Namespace（可指定是否添加命名空间前缀）
     *
     * @param appNamespace App Namespace 信息
     * @param appendNamespacePrefix 是否添加命名空间前缀
     * @return 创建成功的 App Namespace
     */
    OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespace, boolean appendNamespacePrefix);

    /**
     * 导入 App Namespace
     *
     * @param appNamespace App Namespace 信息
     * @return 导入成功的 App Namespace
     */
    OpenAppNamespaceDTO importAppNamespace(OpenAppNamespaceDTO appNamespace);

    /**
     * 删除 App Namespace
     *
     * @param appId app ID
     * @param namespaceName namespace 名称
     * @return 被删除的 App Namespace
     */
    OpenAppNamespaceDTO deleteAppNamespace(String appId, String namespaceName);

    /**
     * 根据 appId 批量删除 App Namespace
     *
     * @param appId app ID
     */
    void batchDeleteByAppId(String appId);
}
