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

import java.util.ArrayList;
import java.util.List;

/**
 * Describes one OpenAPI action available to the current user token.
 */
public class UserTokenOpenApiAction {

  private String id;
  private String method;
  private String path;
  private List<String> requiredOperations;
  private List<String> grantedOperations;
  private String operationMatch;
  private String resourceScope;
  private String description;

  public UserTokenOpenApiAction() {}

  public UserTokenOpenApiAction(String id, String method, String path,
      List<String> requiredOperations, String operationMatch, String resourceScope,
      String description) {
    this.id = id;
    this.method = method;
    this.path = path;
    this.requiredOperations = copyList(requiredOperations);
    this.grantedOperations = new ArrayList<>();
    this.operationMatch = operationMatch;
    this.resourceScope = resourceScope;
    this.description = description;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public List<String> getRequiredOperations() {
    return copyList(requiredOperations);
  }

  public void setRequiredOperations(List<String> requiredOperations) {
    this.requiredOperations = copyList(requiredOperations);
  }

  public List<String> getGrantedOperations() {
    return copyList(grantedOperations);
  }

  public void setGrantedOperations(List<String> grantedOperations) {
    this.grantedOperations = copyList(grantedOperations);
  }

  public String getOperationMatch() {
    return operationMatch;
  }

  public void setOperationMatch(String operationMatch) {
    this.operationMatch = operationMatch;
  }

  public String getResourceScope() {
    return resourceScope;
  }

  public void setResourceScope(String resourceScope) {
    this.resourceScope = resourceScope;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  private static List<String> copyList(List<String> values) {
    return values == null ? new ArrayList<>() : new ArrayList<>(values);
  }
}
