/*
 * Copyright 2025 Apollo Authors
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

import java.util.List;

/**
 * Whether an AppNamespace is in the Portal, and its status in every environment.
 */
public class AppNamespaceDeletionCheckBO {

  private final String appId;

  private final String namespaceName;

  private final boolean existsInPortal;

  private final List<AppNamespaceEnvDeletionStatusBO> environments;

  public AppNamespaceDeletionCheckBO(String appId, String namespaceName, boolean existsInPortal,
      List<AppNamespaceEnvDeletionStatusBO> environments) {
    this.appId = appId;
    this.namespaceName = namespaceName;
    this.existsInPortal = existsInPortal;
    this.environments = environments;
  }

  public String getAppId() {
    return appId;
  }

  public String getNamespaceName() {
    return namespaceName;
  }

  public boolean isExistsInPortal() {
    return existsInPortal;
  }

  public List<AppNamespaceEnvDeletionStatusBO> getEnvironments() {
    return environments;
  }
}
