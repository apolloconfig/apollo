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
package com.ctrip.framework.apollo.openapi.server.service.impl;

import java.util.List;

import com.ctrip.framework.apollo.openapi.server.service.ReleaseOpenApiService;
import org.springframework.stereotype.Service;

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.openapi.model.NamespaceReleaseDTO;
import com.ctrip.framework.apollo.openapi.model.OpenReleaseDTO;
import com.ctrip.framework.apollo.openapi.util.OpenApiBeanUtils;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseBO;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceReleaseModel;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ReleaseService;

/**
 * @author wxq
 */
@Service
public class ServerReleaseOpenApiService implements ReleaseOpenApiService {
  private final ReleaseService releaseService;

  public ServerReleaseOpenApiService(
      ReleaseService releaseService) {
    this.releaseService = releaseService;
  }

  @Override
  public OpenReleaseDTO publishNamespace(String appId, String env, String clusterName,
                                         String namespaceName, NamespaceReleaseDTO releaseDTO) {
    NamespaceReleaseModel releaseModel = BeanUtils.transform(NamespaceReleaseModel.class, releaseDTO);

    releaseModel.setAppId(appId);
    releaseModel.setEnv(Env.valueOf(env).toString());
    releaseModel.setClusterName(clusterName);
    releaseModel.setNamespaceName(namespaceName);

    return OpenApiBeanUtils.transformFromReleaseDTO(releaseService.publish(releaseModel));
  }

  @Override
  public OpenReleaseDTO getLatestActiveRelease(String appId, String env, String clusterName,
      String namespaceName) {
    ReleaseDTO releaseDTO = releaseService.loadLatestRelease(appId, Env.valueOf
        (env), clusterName, namespaceName);
    if (releaseDTO == null) {
      return null;
    }

    return OpenApiBeanUtils.transformFromReleaseDTO(releaseDTO);
  }

  @Override
  public void rollbackRelease(String env, long releaseId, String operator) {
    releaseService.rollback(Env.valueOf(env), releaseId, operator);
  }

  /**
   * 获取发布详情
   * @param env 环境
   * @param releaseId 发布ID
   * @return 发布详情
   */
  @Override
  public OpenReleaseDTO getReleaseById(String env, long releaseId) {
    ReleaseDTO release = releaseService.findReleaseById(Env.valueOf(env), releaseId);
    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }
    return OpenApiBeanUtils.transformFromReleaseDTO(release);
  }

  /**
   * 获取所有发布（分页）
   * @param appId 应用ID
   * @param env 环境
   * @param clusterName 集群名称
   * @param namespaceName 命名空间名称
   * @param page 页码
   * @param size 页大小
   * @return 发布列表
   */
  public List<ReleaseBO> findAllReleases(String appId, String env, String clusterName, String namespaceName, int page, int size) {
    return releaseService.findAllReleases(appId, Env.valueOf(env), clusterName, namespaceName, page, size);
  }

  /**
   * 获取活跃发布（分页）
   * @param appId 应用ID
   * @param env 环境
   * @param clusterName 集群名称
   * @param namespaceName 命名空间名称
   * @param page 页码
   * @param size 页大小
   * @return 活跃发布列表
   */
  public List<OpenReleaseDTO> findActiveReleases(String appId, String env, String clusterName, String namespaceName, int page, int size) {
    List<ReleaseDTO> releases = releaseService.findActiveReleases(appId, Env.valueOf(env), clusterName, namespaceName, page, size);
    return releases.stream()
        .map(OpenApiBeanUtils::transformFromReleaseDTO)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * 对比发布
   * @param env 环境
   * @param baseReleaseId 基础发布ID
   * @param toCompareReleaseId 对比发布ID
   * @return 对比结果
   */
  public ReleaseCompareResult compareRelease(String env, long baseReleaseId, long toCompareReleaseId) {
    return releaseService.compare(Env.valueOf(env), baseReleaseId, toCompareReleaseId);
  }
}
