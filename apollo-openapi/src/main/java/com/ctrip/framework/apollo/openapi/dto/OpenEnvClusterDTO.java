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
package com.ctrip.framework.apollo.openapi.dto;

import java.util.Set;

public class OpenEnvClusterDTO {

  private String env;
  private Set<String> clusters;

  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }

  public Set<String> getClusters() {
    return clusters;
  }

  public void setClusters(Set<String> clusters) {
    this.clusters = clusters;
  }

  @Override
  public String toString() {
    return "OpenEnvClusterDTO{" +
        "env='" + env + '\'' +
        ", clusters=" + clusters +
        '}';
  }
}
