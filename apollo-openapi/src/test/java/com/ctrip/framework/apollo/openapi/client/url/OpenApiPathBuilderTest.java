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
package com.ctrip.framework.apollo.openapi.client.url;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OpenApiPathBuilderTest {

  @Test
  public void testBuildPath() {
    String baseURL = "http://localhost";
    OpenApiPathBuilder tools = OpenApiPathBuilder.newBuilder();
    String path, expected, actual;
    String env = "test";
    String appId = "appid-1001";
    String clusterName = "cluster-1001";
    String namespaceName = "application.yml";
    String key = "spring.profile";
    String operator = "junit";
    long releaseId = 1L;

    // AppOpenApiService path check

    path = String.format("apps/%s/envclusters", tools.escapePath(appId));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .appsPathVal(appId)
            .customResource("envclusters")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    String param = "1,2,3";
    path = String.format("apps?appIds=%s", tools.escapeParam(param));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .customResource("apps")
            .addParam("appIds", param)
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path = "apps/authorized";
    expected = String.format("%s/%s", baseURL, path);
    actual = OpenApiPathBuilder.newBuilder().customResource("apps/authorized").buildPath(baseURL);
    assertEquals(expected, actual);

    // ClusterOpenApiService path check

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s",
            tools.escapePath(env), tools.escapePath(appId), tools.escapePath(clusterName));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format("envs/%s/apps/%s/clusters", tools.escapePath(env), tools.escapePath(appId));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .customResource("clusters")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    // ItemOpenApiService path check

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s/items/%s",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName),
            tools.escapePath(key));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .itemsPathVal(key)
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s/items",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .customResource("items")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s/items/%s?createIfNotExists=true",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName),
            tools.escapePath(key));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .itemsPathVal(key)
            .addParam("createIfNotExists", "true")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s/items/%s?operator=%s",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName),
            tools.escapePath(key),
            tools.escapeParam(operator));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .itemsPathVal(key)
            .addParam("operator", operator)
            .buildPath(baseURL);
    assertEquals(expected, actual);

    // NamespaceOpenApiService path check

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces",
            tools.escapePath(env), tools.escapePath(appId), tools.escapePath(clusterName));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .customResource("namespaces")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path = String.format("apps/%s/appnamespaces", tools.escapePath(appId));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .appsPathVal(appId)
            .customResource("appnamespaces")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s/lock",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .customResource("lock")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    // ReleaseOpenApiService path check

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s/releases",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .customResource("releases")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format(
            "envs/%s/apps/%s/clusters/%s/namespaces/%s/releases/latest",
            tools.escapePath(env),
            tools.escapePath(appId),
            tools.escapePath(clusterName),
            tools.escapePath(namespaceName));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .appsPathVal(appId)
            .clustersPathVal(clusterName)
            .namespacesPathVal(namespaceName)
            .releasesPathVal("latest")
            .buildPath(baseURL);
    assertEquals(expected, actual);

    path =
        String.format(
            "envs/%s/releases/%s/rollback?operator=%s",
            tools.escapePath(env), releaseId, tools.escapeParam(operator));
    expected = String.format("%s/%s", baseURL, path);
    actual =
        OpenApiPathBuilder.newBuilder()
            .envsPathVal(env)
            .releasesPathVal(String.valueOf(releaseId))
            .customResource("rollback")
            .addParam("operator", operator)
            .buildPath(baseURL);
    assertEquals(expected, actual);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAddParamKeyEmpty() {
    OpenApiPathBuilder.newBuilder().addParam("", "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBuildPathURLEmpty() {
    OpenApiPathBuilder.newBuilder().buildPath("");
  }
}
