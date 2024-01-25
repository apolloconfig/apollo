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
package com.ctrip.framework.apollo.common.dto;

import com.google.common.collect.Sets;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class GrayReleaseRuleItemDTO {
  public static final String ALL_IP = "*";
  public static final String ALL_Label = "*";

  private String clientAppId;
  private Set<String> clientIpList;
  private Set<String> clientLabelList;

  // this default constructor is for json deserialize use, to make sure all fields are initialized
  public GrayReleaseRuleItemDTO() {
    this("");
  }

  public GrayReleaseRuleItemDTO(String clientAppId) {
    this(clientAppId, Sets.newHashSet(), Sets.newHashSet());
  }

  public GrayReleaseRuleItemDTO(String clientAppId, Set<String> clientIpList, Set<String> clientLabelList) {
    this.clientAppId = clientAppId;
    this.clientIpList = clientIpList;
    this.clientLabelList = clientLabelList;
  }

  public String getClientAppId() {
    return clientAppId;
  }

  public Set<String> getClientIpList() {
    return clientIpList;
  }

  public Set<String> getClientLabelList() {
    return clientLabelList;
  }

  public boolean matches(String clientAppId, String clientIp,String clientLabel) {
    return (appIdMatches(clientAppId) && ipMatches(clientIp))||(appIdMatches(clientAppId) && labelMatches(clientLabel));
  }

  private boolean appIdMatches(String clientAppId) {
    return this.clientAppId.equalsIgnoreCase(clientAppId);
  }

  private boolean ipMatches(String clientIp) {
    return this.clientIpList.contains(ALL_IP) || clientIpList.contains(clientIp);
  }

  private boolean labelMatches(String clientLabel) {
    return this.clientLabelList.contains(ALL_Label) || clientLabelList.contains(clientLabel);
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("clientAppId", clientAppId)
        .add("clientIpList", clientIpList)
        .add("clientLabelList", clientLabelList).toString();
  }
}
