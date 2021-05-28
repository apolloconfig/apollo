/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.openapi.client.service;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

public class ClusterOpenApiService extends AbstractOpenApiService {

  public ClusterOpenApiService(CloseableHttpClient client, String baseUrl, Gson gson) {
    super(client, baseUrl, gson);
  }

  public OpenClusterDTO getCluster(String appId, String env, String clusterName) {
    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");

    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }

    String path = String.format("envs/%s/apps/%s/clusters/%s", escapePath(env), escapePath(appId),
        escapePath(clusterName));

    try (CloseableHttpResponse response = get(path)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OpenClusterDTO.class);
    } catch (Throwable ex) {
      throw new RuntimeException(String
          .format("Get cluster for appId: %s, cluster: %s in env: %s failed", appId, clusterName, env), ex);
    }
  }

  public OpenClusterDTO createCluster(String env, OpenClusterDTO openClusterDTO) {
    checkNotEmpty(openClusterDTO.getAppId(), "App id");
    checkNotEmpty(env, "Env");
    checkNotEmpty(openClusterDTO.getName(), "Cluster name");
    checkNotEmpty(openClusterDTO.getDataChangeCreatedBy(), "Created by");

    String path = String.format("envs/%s/apps/%s/clusters", escapePath(env), escapePath(openClusterDTO.getAppId()));

    try (CloseableHttpResponse response = post(path, openClusterDTO)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OpenClusterDTO.class);
    } catch (Throwable ex) {
      throw new RuntimeException(String
          .format("Create cluster: %s for appId: %s in env: %s failed", openClusterDTO.getName(),
              openClusterDTO.getAppId(), env), ex);
    }
  }
}
