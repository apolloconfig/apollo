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
package com.ctrip.framework.apollo.core.dto;

import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloConfig {

  private String appId;

  private String cluster;

  private String namespaceName;

  private Map<String, String> configurations;

  private String releaseKey;

  public ApolloConfig() {}

  public ApolloConfig(String appId, String cluster, String namespaceName, String releaseKey) {
    this.appId = appId;
    this.cluster = cluster;
    this.namespaceName = namespaceName;
    this.releaseKey = releaseKey;
  }

  public String getAppId() {
    return appId;
  }

  public String getCluster() {
    return cluster;
  }

  public String getNamespaceName() {
    return namespaceName;
  }

  public String getReleaseKey() {
    return releaseKey;
  }

  public Map<String, String> getConfigurations() {
    return configurations;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setCluster(String cluster) {
    this.cluster = cluster;
  }

  public void setNamespaceName(String namespaceName) {
    this.namespaceName = namespaceName;
  }

  public void setReleaseKey(String releaseKey) {
    this.releaseKey = releaseKey;
  }

  public void setConfigurations(Map<String, String> configurations) {
    this.configurations = configurations;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ApolloConfig{");
    sb.append("appId='").append(appId).append('\'');
    sb.append(", cluster='").append(cluster).append('\'');
    sb.append(", namespaceName='").append(namespaceName).append('\'');
    sb.append(", configurations=").append(configurations);
    sb.append(", releaseKey='").append(releaseKey).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
