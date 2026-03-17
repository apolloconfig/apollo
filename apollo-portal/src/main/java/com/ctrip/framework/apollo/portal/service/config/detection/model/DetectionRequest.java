/*
 * Copyright 2026 Apollo Authors
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
package com.ctrip.framework.apollo.portal.service.config.detection.model;

/**
 * Configuration detection request
 */
public class DetectionRequest {

  private String appId;
  private String env;
  private String clusterName;
  private String namespaceName;
  private String key;
  private String value;
  private String comment;
  private String provider;
  private String dimension;

  public DetectionRequest() {
  }

  private DetectionRequest(Builder builder) {
    this.appId = builder.appId;
    this.env = builder.env;
    this.clusterName = builder.clusterName;
    this.namespaceName = builder.namespaceName;
    this.key = builder.key;
    this.value = builder.value;
    this.comment = builder.comment;
    this.provider = builder.provider;
    this.dimension = builder.dimension;
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getEnv() {
    return env;
  }

  public void setEnv(String env) {
    this.env = env;
  }

  public String getClusterName() {
    return clusterName;
  }

  public void setClusterName(String clusterName) {
    this.clusterName = clusterName;
  }

  public String getNamespaceName() {
    return namespaceName;
  }

  public void setNamespaceName(String namespaceName) {
    this.namespaceName = namespaceName;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getDimension() {
    return dimension;
  }

  public void setDimension(String dimension) {
    this.dimension = dimension;
  }

  public static class Builder {
    private String appId;
    private String env;
    private String clusterName;
    private String namespaceName;
    private String key;
    private String value;
    private String comment;
    private String provider;
    private String dimension;

    public Builder appId(String appId) {
      this.appId = appId;
      return this;
    }

    public Builder env(String env) {
      this.env = env;
      return this;
    }

    public Builder clusterName(String clusterName) {
      this.clusterName = clusterName;
      return this;
    }

    public Builder namespaceName(String namespaceName) {
      this.namespaceName = namespaceName;
      return this;
    }

    public Builder key(String key) {
      this.key = key;
      return this;
    }

    public Builder value(String value) {
      this.value = value;
      return this;
    }

    public Builder comment(String comment) {
      this.comment = comment;
      return this;
    }

    public Builder provider(String provider) {
      this.provider = provider;
      return this;
    }

    public Builder dimension(String dimension) {
      this.dimension = dimension;
      return this;
    }

    public DetectionRequest build() {
      return new DetectionRequest(this);
    }
  }
}
