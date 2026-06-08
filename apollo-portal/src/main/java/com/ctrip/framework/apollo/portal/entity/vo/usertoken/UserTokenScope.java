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
package com.ctrip.framework.apollo.portal.entity.vo.usertoken;

import com.ctrip.framework.apollo.portal.environment.Env;
import com.google.common.base.Strings;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UserTokenScope {

  private Set<String> operations;
  private Set<String> appIds;
  private Set<String> envs;
  private List<UserTokenNamespaceScope> namespaces;
  private transient boolean denyAll;

  public static UserTokenScope allowAll() {
    return new UserTokenScope();
  }

  public static UserTokenScope denyAll() {
    UserTokenScope scope = new UserTokenScope();
    scope.denyAll = true;
    return scope;
  }

  public boolean allowsOperation(String operation) {
    if (denyAll) {
      return false;
    }
    return operations == null || operations.isEmpty() || operations.contains(operation);
  }

  public boolean allowsApp(String appId) {
    if (denyAll) {
      return false;
    }
    return appIds == null || appIds.isEmpty() || appIds.contains(appId);
  }

  public boolean allowsEnv(String env) {
    if (denyAll) {
      return false;
    }
    return envs == null || envs.isEmpty() || envs.contains(normalizeEnv(env));
  }

  public boolean allowsNamespace(String appId, String env, String clusterName,
      String namespaceName) {
    if (!allowsApp(appId) || !allowsEnv(env)) {
      return false;
    }
    if (namespaces == null || namespaces.isEmpty()) {
      return true;
    }
    String normalizedEnv = normalizeEnv(env);
    for (UserTokenNamespaceScope namespaceScope : namespaces) {
      if (matches(namespaceScope.getAppId(), appId)
          && matches(normalizeEnv(namespaceScope.getEnv()), normalizedEnv)
          && matches(namespaceScope.getClusterName(), clusterName)
          && matches(namespaceScope.getNamespaceName(), namespaceName)) {
        return true;
      }
    }
    return false;
  }

  private boolean matches(String expected, String actual) {
    return Strings.isNullOrEmpty(expected) || "*".equals(expected) || expected.equals(actual);
  }

  private String normalizeEnv(String env) {
    if (Strings.isNullOrEmpty(env)) {
      return env;
    }
    Env transformedEnv = Env.transformEnv(env);
    return transformedEnv == Env.UNKNOWN ? env : transformedEnv.getName();
  }

  public Set<String> getOperations() {
    return operations == null ? Collections.emptySet() : operations;
  }

  public void setOperations(Set<String> operations) {
    this.operations = operations;
  }

  public Set<String> getAppIds() {
    return appIds == null ? Collections.emptySet() : appIds;
  }

  public void setAppIds(Set<String> appIds) {
    this.appIds = appIds;
  }

  public Set<String> getEnvs() {
    return envs == null ? Collections.emptySet() : envs;
  }

  public void setEnvs(Set<String> envs) {
    this.envs = envs;
  }

  public List<UserTokenNamespaceScope> getNamespaces() {
    return namespaces == null ? Collections.emptyList() : namespaces;
  }

  public void setNamespaces(List<UserTokenNamespaceScope> namespaces) {
    this.namespaces = namespaces;
  }

  public boolean isDenyAll() {
    return denyAll;
  }
}
