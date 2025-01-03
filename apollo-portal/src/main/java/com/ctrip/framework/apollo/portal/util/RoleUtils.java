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
package com.ctrip.framework.apollo.portal.util;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.portal.constant.RoleType;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.Iterator;

public class RoleUtils {

  private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).skipNulls();
  private static final Splitter STRING_SPLITTER = Splitter.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
      .omitEmptyStrings().trimResults();

  public static String buildAppMasterRoleName(String appId) {
    return STRING_JOINER.join(RoleType.MASTER, appId);
  }

  public static String extractAppIdFromMasterRoleName(String masterRoleName) {
    Iterator<String> parts = STRING_SPLITTER.split(masterRoleName).iterator();

    // skip role type
    if (parts.hasNext() && parts.next().equals(RoleType.MASTER) && parts.hasNext()) {
      return parts.next();
    }

    return null;
  }

  public static String extractAppIdFromRoleName(String roleName) {
     Iterator<String> parts = STRING_SPLITTER.split(roleName).iterator();
     if (parts.hasNext()) {
       String roleType = parts.next();
       if (RoleType.isValidRoleType(roleType) && parts.hasNext()) {
         return parts.next();
       }
     }
     return null;
  }

  public static String buildAppRoleName(String appId, String roleType) {
    return STRING_JOINER.join(roleType, appId);
  }

  public static String buildModifyNamespaceRoleName(String appId, String namespaceName) {
    return buildModifyNamespaceRoleName(appId, namespaceName, null);
  }

  public static String buildModifyNamespaceRoleName(String appId, String namespaceName, String env) {
    return STRING_JOINER.join(RoleType.MODIFY_NAMESPACE, appId, namespaceName, env);
  }

  public static String buildModifyNamespacesInClusterRoleName(String appId, String env, String clusterName) {
    return STRING_JOINER.join(RoleType.MODIFY_NAMESPACES_IN_CLUSTER, appId, env, clusterName);
  }

  public static String buildModifyDefaultNamespaceRoleName(String appId) {
    return STRING_JOINER.join(RoleType.MODIFY_NAMESPACE, appId, ConfigConsts.NAMESPACE_APPLICATION);
  }

  public static String buildReleaseNamespaceRoleName(String appId, String namespaceName) {
    return buildReleaseNamespaceRoleName(appId, namespaceName, null);
  }

  public static String buildReleaseNamespaceRoleName(String appId, String namespaceName, String env) {
    return STRING_JOINER.join(RoleType.RELEASE_NAMESPACE, appId, namespaceName, env);
  }

  public static String buildReleaseNamespacesInClusterRoleName(String appId, String env, String clusterName) {
    return STRING_JOINER.join(RoleType.RELEASE_NAMESPACES_IN_CLUSTER, appId, env, clusterName);
  }

  public static String buildNamespaceRoleName(String appId, String namespaceName, String roleType) {
    return buildNamespaceRoleName(appId, namespaceName, roleType, null);
  }

  public static String buildNamespaceRoleName(String appId, String namespaceName, String roleType, String env) {
    return STRING_JOINER.join(roleType, appId, namespaceName, env);
  }

  public static String buildClusterRoleName(String appId, String env, String clusterName, String roleType) {
    return STRING_JOINER.join(roleType, appId, env, clusterName);
  }

  public static String buildReleaseDefaultNamespaceRoleName(String appId) {
    return STRING_JOINER.join(RoleType.RELEASE_NAMESPACE, appId, ConfigConsts.NAMESPACE_APPLICATION);
  }

  public static String buildNamespaceTargetId(String appId, String namespaceName) {
    return buildNamespaceTargetId(appId, namespaceName, null);
  }

  public static String buildNamespaceTargetId(String appId, String namespaceName, String env) {
    return STRING_JOINER.join(appId, namespaceName, env);
  }

  public static String buildClusterTargetId(String appId, String env, String clusterName) {
    return STRING_JOINER.join(appId, env, clusterName);
  }

  public static String buildDefaultNamespaceTargetId(String appId) {
    return STRING_JOINER.join(appId, ConfigConsts.NAMESPACE_APPLICATION);
  }

  public static String buildCreateApplicationRoleName(String permissionType, String permissionTargetId) {
    return STRING_JOINER.join(permissionType, permissionTargetId);
  }
}
