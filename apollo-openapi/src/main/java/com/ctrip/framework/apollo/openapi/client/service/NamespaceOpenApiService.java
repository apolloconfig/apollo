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
package com.ctrip.framework.apollo.openapi.client.service;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.openapi.client.url.OpenApiPathBuilder;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

public class NamespaceOpenApiService extends AbstractOpenApiService implements
    com.ctrip.framework.apollo.openapi.api.NamespaceOpenApiService {
  private static final Type OPEN_NAMESPACE_DTO_LIST_TYPE = new TypeToken<List<OpenNamespaceDTO>>() {
  }.getType();

  public NamespaceOpenApiService(CloseableHttpClient client, String baseUrl, Gson gson) {
    super(client, baseUrl, gson);
  }

  @Override
  public OpenNamespaceDTO getNamespace(String appId, String env, String clusterName, String namespaceName) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
    if (Strings.isNullOrEmpty(namespaceName)) {
      namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");

    OpenApiPathBuilder pathBuilder = OpenApiPathBuilder.newBuilder()
        .envsPathVal(env)
        .appsPathVal(appId)
        .clustersPathVal(clusterName)
        .namespacesPathVal(namespaceName);

    try (CloseableHttpResponse response = get(pathBuilder)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OpenNamespaceDTO.class);
    } catch (Throwable ex) {
      throw new RuntimeException(String
          .format("Get namespace for appId: %s, cluster: %s, namespace: %s in env: %s failed", appId, clusterName,
              namespaceName, env), ex);
    }
  }

  @Override
  public List<OpenNamespaceDTO> getNamespaces(String appId, String env, String clusterName) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");

    OpenApiPathBuilder pathBuilder = OpenApiPathBuilder.newBuilder()
        .envsPathVal(env)
        .appsPathVal(appId)
        .clustersPathVal(clusterName)
        .customResource("namespaces");

    try (CloseableHttpResponse response = get(pathBuilder)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OPEN_NAMESPACE_DTO_LIST_TYPE);
    } catch (Throwable ex) {
      throw new RuntimeException(String
          .format("Get namespaces for appId: %s, cluster: %s in env: %s failed", appId, clusterName, env), ex);
    }
  }

  @Override
  public OpenAppNamespaceDTO createAppNamespace(OpenAppNamespaceDTO appNamespaceDTO) {
    checkNotEmpty(appNamespaceDTO.getAppId(), "App id");
    checkNotEmpty(appNamespaceDTO.getName(), "Name");
    checkNotEmpty(appNamespaceDTO.getDataChangeCreatedBy(), "Created by");

    if (Strings.isNullOrEmpty(appNamespaceDTO.getFormat())) {
      appNamespaceDTO.setFormat(ConfigFileFormat.Properties.getValue());
    }

    OpenApiPathBuilder pathBuilder = OpenApiPathBuilder.newBuilder()
        .appsPathVal(appNamespaceDTO.getAppId())
        .customResource("appnamespaces");

    try (CloseableHttpResponse response = post(pathBuilder, appNamespaceDTO)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OpenAppNamespaceDTO.class);
    } catch (Throwable ex) {
      throw new RuntimeException(String
          .format("Create app namespace: %s for appId: %s, format: %s failed", appNamespaceDTO.getName(),
              appNamespaceDTO.getAppId(), appNamespaceDTO.getFormat()), ex);
    }
  }

  @Override
  public OpenNamespaceLockDTO getNamespaceLock(String appId, String env, String clusterName, String namespaceName) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
    if (Strings.isNullOrEmpty(namespaceName)) {
      namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");

    OpenApiPathBuilder pathBuilder = OpenApiPathBuilder.newBuilder()
        .envsPathVal(env)
        .appsPathVal(appId)
        .clustersPathVal(clusterName)
        .namespacesPathVal(namespaceName)
        .customResource("lock");

    try (CloseableHttpResponse response = get(pathBuilder)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OpenNamespaceLockDTO.class);
    } catch (Throwable ex) {
      throw new RuntimeException(String
          .format("Get namespace lock for appId: %s, cluster: %s, namespace: %s in env: %s failed", appId, clusterName,
              namespaceName, env), ex);
    }
  }
}
