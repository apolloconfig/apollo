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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class UserTokenOperation {

  public static final String CONFIG_READ = "config:read";
  public static final String CONFIG_MODIFY = "config:modify";
  public static final String CONFIG_RELEASE = "config:release";
  public static final String NAMESPACE_CREATE = "namespace:create";
  public static final String NAMESPACE_DELETE = "namespace:delete";
  public static final String CLUSTER_CREATE = "cluster:create";
  public static final String APP_CREATE = "app:create";
  public static final String APP_MANAGE_ROLE = "app:manage-role";
  public static final String USER_MANAGE = "user:manage";
  public static final String SYSTEM_ADMIN = "system:admin";

  public static final List<String> RESOURCE_SCOPED =
      Collections.unmodifiableList(Arrays.asList(CONFIG_READ, CONFIG_MODIFY, CONFIG_RELEASE,
          NAMESPACE_CREATE, NAMESPACE_DELETE, CLUSTER_CREATE, APP_MANAGE_ROLE));

  public static final List<String> ALL = Collections.unmodifiableList(
      Arrays.asList(CONFIG_READ, CONFIG_MODIFY, CONFIG_RELEASE, NAMESPACE_CREATE, NAMESPACE_DELETE,
          CLUSTER_CREATE, APP_CREATE, APP_MANAGE_ROLE, USER_MANAGE, SYSTEM_ADMIN));

  private UserTokenOperation() {}
}
