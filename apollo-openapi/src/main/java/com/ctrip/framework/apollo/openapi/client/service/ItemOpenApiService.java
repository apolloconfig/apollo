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
import com.ctrip.framework.apollo.openapi.client.exception.ApolloOpenApiException;
import com.ctrip.framework.apollo.openapi.client.url.OpenApiPathBuilder;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.google.common.base.Strings;
import com.google.gson.Gson;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

public class ItemOpenApiService extends AbstractOpenApiService
    implements com.ctrip.framework.apollo.openapi.api.ItemOpenApiService {

  public ItemOpenApiService(CloseableHttpClient client, String baseUrl, Gson gson) {
    super(client, baseUrl, gson);
  }

  @Override
  public OpenItemDTO getItem(
      String appId, String env, String clusterName, String namespaceName, String key) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
    if (Strings.isNullOrEmpty(namespaceName)) {
      namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");
    checkNotEmpty(key, "Item key");

    OpenApiPathBuilder pathBuilder =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .itemsPathVal(key);

    try (CloseableHttpResponse response = get(pathBuilder)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OpenItemDTO.class);
    } catch (Throwable ex) {
      // return null if item doesn't exist
      if (ex instanceof ApolloOpenApiException
          && ((ApolloOpenApiException) ex).getStatus() == 404) {
        return null;
      }
      throw new RuntimeException(
          String.format(
              "Get item: %s for appId: %s, cluster: %s, namespace: %s in env: %s failed",
              key, appId, clusterName, namespaceName, env),
          ex);
    }
  }

  @Override
  public OpenItemDTO createItem(
      String appId, String env, String clusterName, String namespaceName, OpenItemDTO itemDTO) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
    if (Strings.isNullOrEmpty(namespaceName)) {
      namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");
    checkNotEmpty(itemDTO.getKey(), "Item key");
    checkNotEmpty(itemDTO.getDataChangeCreatedBy(), "Item created by");

    OpenApiPathBuilder pathBuilder =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .customResource("items");

    try (CloseableHttpResponse response = post(pathBuilder, itemDTO)) {
      return gson.fromJson(EntityUtils.toString(response.getEntity()), OpenItemDTO.class);
    } catch (Throwable ex) {
      throw new RuntimeException(
          String.format(
              "Create item: %s for appId: %s, cluster: %s, namespace: %s in env: %s failed",
              itemDTO.getKey(), appId, clusterName, namespaceName, env),
          ex);
    }
  }

  @Override
  public void updateItem(
      String appId, String env, String clusterName, String namespaceName, OpenItemDTO itemDTO) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
    if (Strings.isNullOrEmpty(namespaceName)) {
      namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");
    checkNotEmpty(itemDTO.getKey(), "Item key");
    checkNotEmpty(itemDTO.getDataChangeLastModifiedBy(), "Item modified by");

    OpenApiPathBuilder pathBuilder =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .itemsPathVal(itemDTO.getKey());

    try (CloseableHttpResponse ignored = put(pathBuilder, itemDTO)) {
    } catch (Throwable ex) {
      throw new RuntimeException(
          String.format(
              "Update item: %s for appId: %s, cluster: %s, namespace: %s in env: %s failed",
              itemDTO.getKey(), appId, clusterName, namespaceName, env),
          ex);
    }
  }

  @Override
  public void createOrUpdateItem(
      String appId, String env, String clusterName, String namespaceName, OpenItemDTO itemDTO) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
    if (Strings.isNullOrEmpty(namespaceName)) {
      namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");
    checkNotEmpty(itemDTO.getKey(), "Item key");
    checkNotEmpty(itemDTO.getDataChangeCreatedBy(), "Item created by");

    if (Strings.isNullOrEmpty(itemDTO.getDataChangeLastModifiedBy())) {
      itemDTO.setDataChangeLastModifiedBy(itemDTO.getDataChangeCreatedBy());
    }

    OpenApiPathBuilder pathBuilder =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .itemsPathVal(itemDTO.getKey())
            .addParam("createIfNotExists", "true");

    try (CloseableHttpResponse ignored = put(pathBuilder, itemDTO)) {
    } catch (Throwable ex) {
      throw new RuntimeException(
          String.format(
              "CreateOrUpdate item: %s for appId: %s, cluster: %s, namespace: %s in env: %s failed",
              itemDTO.getKey(), appId, clusterName, namespaceName, env),
          ex);
    }
  }

  @Override
  public void removeItem(
      String appId,
      String env,
      String clusterName,
      String namespaceName,
      String key,
      String operator) {
    if (Strings.isNullOrEmpty(clusterName)) {
      clusterName = ConfigConsts.CLUSTER_NAME_DEFAULT;
    }
    if (Strings.isNullOrEmpty(namespaceName)) {
      namespaceName = ConfigConsts.NAMESPACE_APPLICATION;
    }

    checkNotEmpty(appId, "App id");
    checkNotEmpty(env, "Env");
    checkNotEmpty(key, "Item key");
    checkNotEmpty(operator, "Operator");

    OpenApiPathBuilder pathBuilder =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .itemsPathVal(key)
            .addParam("operator", operator);

    try (CloseableHttpResponse ignored = delete(pathBuilder)) {
    } catch (Throwable ex) {
      throw new RuntimeException(
          String.format(
              "Remove item: %s for appId: %s, cluster: %s, namespace: %s in env: %s failed",
              key, appId, clusterName, namespaceName, env),
          ex);
    }
  }
}
