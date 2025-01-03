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
package com.ctrip.framework.apollo.portal.constant;

public class RoleType {

  public static final String MASTER = "Master";

  public static final String MODIFY_NAMESPACE = "ModifyNamespace";

  public static final String RELEASE_NAMESPACE = "ReleaseNamespace";

  public static final String MODIFY_NAMESPACES_IN_CLUSTER = "ModifyNamespacesInCluster";

  public static final String RELEASE_NAMESPACES_IN_CLUSTER = "ReleaseNamespacesInCluster";

  public static boolean isValidRoleType(String roleType) {
    return MASTER.equals(roleType) || MODIFY_NAMESPACE.equals(roleType) || RELEASE_NAMESPACE.equals(
        roleType) || MODIFY_NAMESPACES_IN_CLUSTER.equals(roleType) || RELEASE_NAMESPACES_IN_CLUSTER.equals(roleType);
  }

}
