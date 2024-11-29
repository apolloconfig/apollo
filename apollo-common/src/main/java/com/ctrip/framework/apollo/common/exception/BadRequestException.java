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

public class BadRequestException extends AbstractApolloHttpException {

  /**
   * @see AbstractApolloHttpException#AbstractApolloHttpException(String, Object...)
   */
  public BadRequestException(String msgtpl, Object... args) {
    super(msgtpl, args);
    setHttpStatus(HttpStatus.BAD_REQUEST);
  }

  public static BadRequestException ownerNameIsBlank() {
    return new BadRequestException("ownerName can not be blank");
  }

  public static BadRequestException orgIdIsBlank() {
    return new BadRequestException("orgId can not be blank");
  }

  public static BadRequestException rateLimitIsInvalid() {
    return new BadRequestException("rate limit must be greater than 1");
  }

  public static BadRequestException itemAlreadyExists(String itemKey) {
    return new BadRequestException("item already exists for itemKey:%s", itemKey);
  }

  public static BadRequestException itemNotExists(long itemId) {
    return new BadRequestException("item not exists for itemId:%s", itemId);
  }

  public static BadRequestException namespaceNotExists() {
    return new BadRequestException("namespace not exist.");
  }

  public static BadRequestException namespaceNotExists(String appId, String clusterName,
      String namespaceName) {
    return new BadRequestException(
        "namespace not exist for appId:%s clusterName:%s namespaceName:%s", appId, clusterName,
        namespaceName);
  }

  public static BadRequestException namespaceAlreadyExists(String namespaceName) {
    return new BadRequestException("namespace already exists for namespaceName:%s", namespaceName);
  }

  public static BadRequestException appNamespaceNotExists(String appId, String namespaceName) {
    return new BadRequestException("appNamespace not exist for appId:%s namespaceName:%s", appId, namespaceName);
  }

  public static BadRequestException appNamespaceAlreadyExists(String appId, String namespaceName) {
    return new BadRequestException("appNamespace already exists for appId:%s namespaceName:%s", appId, namespaceName);
  }

  public static BadRequestException invalidNamespaceFormat(String format) {
    return new BadRequestException("invalid namespace format:%s", format);
  }

  public static BadRequestException invalidNotificationsFormat(String format) {
    return new BadRequestException("invalid notifications format:%s", format);
  }

  public static BadRequestException invalidClusterNameFormat(String format) {
    return new BadRequestException("invalid clusterName format:%s", format);
  }

  public static BadRequestException invalidRoleTypeFormat(String format) {
    return new BadRequestException("invalid roleType format:%s", format);
  }

  public static BadRequestException invalidEnvFormat(String format) {
    return new BadRequestException("invalid env format:%s", format);
  }

  public static BadRequestException namespaceNotMatch() {
    return new BadRequestException("invalid request, item and namespace do not match!");
  }

  public static BadRequestException appNotExists(String appId) {
    return new BadRequestException("app not exists for appId:%s", appId);
  }

  public static BadRequestException appAlreadyExists(String appId) {
    return new BadRequestException("app already exists for appId:%s", appId);
  }

  public static BadRequestException appIdIsBlank() {
    return new BadRequestException("appId can not be blank");
  }

  public static BadRequestException appNameIsBlank() {
    return new BadRequestException("app name can not be blank");
  }

  public static BadRequestException clusterNotExists(String clusterName) {
    return new BadRequestException("cluster not exists for clusterName:%s", clusterName);
  }

  public static BadRequestException clusterAlreadyExists(String clusterName) {
    return new BadRequestException("cluster already exists for clusterName:%s", clusterName);
  }

  public static BadRequestException userNotExists(String userName) {
    return new BadRequestException("user not exists for userName:%s", userName);
  }

  public static BadRequestException userAlreadyExists(String userName) {
    return new BadRequestException("user already exists for userName:%s", userName);
  }

  public static BadRequestException userAlreadyAuthorized(String userName) {
    return new BadRequestException("%s already authorized", userName);
  }

  public static BadRequestException accessKeyNotExists() {
    return new BadRequestException("accessKey not exist.");
  }
}
