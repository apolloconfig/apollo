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
package com.ctrip.framework.apollo.common.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends AbstractApolloHttpException {

  /**
   * @see AbstractApolloHttpException#AbstractApolloHttpException(String, Object...)
   */
  public NotFoundException(String msgTpl, Object... args) {
    super(msgTpl, args);
    setHttpStatus(HttpStatus.NOT_FOUND);
  }

  public static NotFoundException itemNotFound(long itemId) {
    return new NotFoundException("item not found for itemId:%s",itemId);
  }

  public static NotFoundException itemNotFound(String itemKey) {
    return new NotFoundException("item not found for itemKey:%s",itemKey);
  }

  public static NotFoundException itemNotFound(String appId, String clusterName, String namespaceName, String itemKey) {
    return new NotFoundException("item not found for appId:%s clusterName:%s namespaceName:%s itemKey:%s", appId, clusterName, namespaceName, itemKey);
  }

  public static NotFoundException itemNotFound(String appId, String clusterName, String namespaceName, long itemId) {
    return new NotFoundException("item not found for appId:%s clusterName:%s namespaceName:%s itemId:%s", appId, clusterName, namespaceName, itemId);
  }

  public static NotFoundException namespaceNotFound(String appId, String clusterName, String namespaceName) {
    return new NotFoundException("namespace not found for appId:%s clusterName:%s namespaceName:%s", appId, clusterName, namespaceName);
  }

  public static NotFoundException namespaceNotFound(long namespaceId) {
    return new NotFoundException("namespace not found for namespaceId:%s", namespaceId);
  }

  public static NotFoundException releaseNotFound(Object releaseId) {
    return new NotFoundException("release not found for releaseId:%s", releaseId);
  }

  public static NotFoundException clusterNotFound(String appId, String clusterName) {
    return new NotFoundException("cluster not found for appId:%s clusterName:%s", appId, clusterName);
  }

  public static NotFoundException appNotFound(String appId) {
    return new NotFoundException("app not found for appId:%s", appId);
  }

  public static NotFoundException roleNotFound(String roleName) {
    return new NotFoundException(
        "role not found for roleName:%s, please check apollo portal DB table 'Role'",
        roleName
    );
  }
}
