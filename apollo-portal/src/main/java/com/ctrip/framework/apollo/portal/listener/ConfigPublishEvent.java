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
package com.ctrip.framework.apollo.portal.listener;

import com.ctrip.framework.apollo.portal.environment.Env;

import org.springframework.context.ApplicationEvent;

public class ConfigPublishEvent extends ApplicationEvent {

  private ConfigPublishInfo configPublishInfo;

  public ConfigPublishEvent(Object source) {
    super(source);
    configPublishInfo = (ConfigPublishInfo) source;
  }

  public static ConfigPublishEvent instance() {
    ConfigPublishInfo info = new ConfigPublishInfo();
    return new ConfigPublishEvent(info);
  }

  public ConfigPublishInfo getConfigPublishInfo(){
    return configPublishInfo;
  }

  public ConfigPublishEvent withAppId(String appId) {
    configPublishInfo.setAppId(appId);
    return this;
  }

  public ConfigPublishEvent withCluster(String clusterName) {
    configPublishInfo.setClusterName(clusterName);
    return this;
  }

  public ConfigPublishEvent withNamespace(String namespaceName) {
    configPublishInfo.setNamespaceName(namespaceName);
    return this;
  }

  public ConfigPublishEvent withReleaseId(long releaseId){
    configPublishInfo.setReleaseId(releaseId);
    return this;
  }

  public ConfigPublishEvent withPreviousReleaseId(long previousReleaseId){
    configPublishInfo.setPreviousReleaseId(previousReleaseId);
    return this;
  }

  public ConfigPublishEvent setNormalPublishEvent(boolean isNormalPublishEvent) {
    configPublishInfo.setNormalPublishEvent(isNormalPublishEvent);
    return this;
  }

  public ConfigPublishEvent setGrayPublishEvent(boolean isGrayPublishEvent) {
    configPublishInfo.setGrayPublishEvent(isGrayPublishEvent);
    return this;
  }

  public ConfigPublishEvent setRollbackEvent(boolean isRollbackEvent) {
    configPublishInfo.setRollbackEvent(isRollbackEvent);
    return this;
  }

  public ConfigPublishEvent setMergeEvent(boolean isMergeEvent) {
    configPublishInfo.setMergeEvent(isMergeEvent);
    return this;
  }

  public ConfigPublishEvent setEnv(Env env) {
    configPublishInfo.setEnv(env);
    return this;
  }


  public static class ConfigPublishInfo {

    private String env;
    private String appId;
    private String clusterName;
    private String namespaceName;
    private long releaseId;
    private long previousReleaseId;
    private boolean isRollbackEvent;
    private boolean isMergeEvent;
    private boolean isNormalPublishEvent;
    private boolean isGrayPublishEvent;

    public Env getEnv() {
      return Env.valueOf(env);
    }

    public void setEnv(Env env) {
      this.env = env.toString();
    }

    public String getAppId() {
      return appId;
    }

    public void setAppId(String appId) {
      this.appId = appId;
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

    public long getReleaseId() {
      return releaseId;
    }

    public void setReleaseId(long releaseId) {
      this.releaseId = releaseId;
    }

    public long getPreviousReleaseId() {
      return previousReleaseId;
    }

    public void setPreviousReleaseId(long previousReleaseId) {
      this.previousReleaseId = previousReleaseId;
    }

    public boolean isRollbackEvent() {
      return isRollbackEvent;
    }

    public void setRollbackEvent(boolean rollbackEvent) {
      isRollbackEvent = rollbackEvent;
    }

    public boolean isMergeEvent() {
      return isMergeEvent;
    }

    public void setMergeEvent(boolean mergeEvent) {
      isMergeEvent = mergeEvent;
    }

    public boolean isNormalPublishEvent() {
      return isNormalPublishEvent;
    }

    public void setNormalPublishEvent(boolean normalPublishEvent) {
      isNormalPublishEvent = normalPublishEvent;
    }

    public boolean isGrayPublishEvent() {
      return isGrayPublishEvent;
    }

    public void setGrayPublishEvent(boolean grayPublishEvent) {
      isGrayPublishEvent = grayPublishEvent;
    }
  }
}
