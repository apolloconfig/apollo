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
package com.ctrip.framework.apollo.biz.grayReleaseRule;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;

import java.util.Set;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class GrayReleaseRuleCache implements Comparable<GrayReleaseRuleCache> {
  private long ruleId;
  private String branchName;
  private String namespaceName;
  private long releaseId;
  private long loadVersion;
  private int branchStatus;
  private Set<GrayReleaseRuleItemDTO> ruleItems;

  public GrayReleaseRuleCache(long ruleId, String branchName, String namespaceName, long
      releaseId, int branchStatus, long loadVersion, Set<GrayReleaseRuleItemDTO> ruleItems) {
    this.ruleId = ruleId;
    this.branchName = branchName;
    this.namespaceName = namespaceName;
    this.releaseId = releaseId;
    this.branchStatus = branchStatus;
    this.loadVersion = loadVersion;
    this.ruleItems = ruleItems;
  }

  public long getRuleId() {
    return ruleId;
  }

  public Set<GrayReleaseRuleItemDTO> getRuleItems() {
    return ruleItems;
  }

  public String getBranchName() {
    return branchName;
  }

  public int getBranchStatus() {
    return branchStatus;
  }

  public long getReleaseId() {
    return releaseId;
  }

  public long getLoadVersion() {
    return loadVersion;
  }

  public void setLoadVersion(long loadVersion) {
    this.loadVersion = loadVersion;
  }

  public String getNamespaceName() {
    return namespaceName;
  }

  public boolean matches(String clientAppId, String clientIp, String clientLabel) {
    for (GrayReleaseRuleItemDTO ruleItem : ruleItems) {
      if (ruleItem.matches(clientAppId, clientIp, clientLabel)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public int compareTo(GrayReleaseRuleCache that) {
    return Long.compare(this.ruleId, that.ruleId);
  }
}
