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
package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.util.NamespaceBOUtils;

/**
 * a namespace represent.
 * @author wxq
 */
public class ConfigBO {

  private final Env env;

  private final String ownerName;

  private final String appId;

  private final String clusterName;

  private final String namespace;

  private final String configFileContent;

  private final ConfigFileFormat format;

  private final boolean isPublic;

  public ConfigBO(Env env, String ownerName, String appId, String clusterName,
      String namespace, boolean isPublic, String configFileContent, ConfigFileFormat format) {
    this.env = env;
    this.ownerName = ownerName;
    this.appId = appId;
    this.clusterName = clusterName;
    this.namespace = namespace;
    this.isPublic = isPublic;
    this.configFileContent = configFileContent;
    this.format = format;
  }

  public ConfigBO(Env env, String ownerName, String appId, String clusterName, NamespaceBO namespaceBO) {
    this(env, ownerName, appId, clusterName,
         namespaceBO.getBaseInfo().getNamespaceName(), namespaceBO.isPublic(),
         NamespaceBOUtils.convert2configFileContent(namespaceBO),
         ConfigFileFormat.fromString(namespaceBO.getFormat())
    );
  }

  @Override
  public String toString() {
    return "ConfigBO{" +
        "env=" + env +
        ", ownerName='" + ownerName + '\'' +
        ", appId='" + appId + '\'' +
        ", clusterName='" + clusterName + '\'' +
        ", namespace='" + namespace + '\'' +
        ", isPublic='" + isPublic + '\'' +
        ", configFileContent='" + configFileContent + '\'' +
        ", format=" + format +
        '}';
  }

  public Env getEnv() {
    return env;
  }

  public String getOwnerName() {
    return ownerName;
  }

  public String getAppId() {
    return appId;
  }

  public String getClusterName() {
    return clusterName;
  }

  public String getNamespace() {
    return namespace;
  }

  public String getConfigFileContent() {
    return configFileContent;
  }

  public ConfigFileFormat getFormat() {
    return format;
  }

  public boolean isPublic() {
    return isPublic;
  }
}
