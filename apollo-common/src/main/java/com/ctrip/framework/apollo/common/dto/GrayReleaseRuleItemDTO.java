/*
 * Copyright 2021 Apollo Authors
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

  private String clientAppId;
  private Set<String> clientIpList;

  public GrayReleaseRuleItemDTO(String clientAppId) {
    this(clientAppId, Sets.newHashSet());
  }

  public GrayReleaseRuleItemDTO(String clientAppId, Set<String> clientIpList) {
    this.clientAppId = clientAppId;
    this.clientIpList = clientIpList;
  }

  public String getClientAppId() {
    return clientAppId;
  }

  public Set<String> getClientIpList() {
    return clientIpList;
  }

  public boolean matches(String clientAppId, String clientIp) {
    return appIdMatches(clientAppId) && ipMatches(clientIp);
  }

  private boolean appIdMatches(String clientAppId) {
    return this.clientAppId.equalsIgnoreCase(clientAppId);
  }

  private boolean ipMatches(String clientIp) {
    return this.clientIpList.contains(ALL_IP) || clientIpList.contains(clientIp);
  }

  @Override
  public String toString() {
    return toStringHelper(this).add("clientAppId", clientAppId)
        .add("clientIpList", clientIpList).toString();
  }
}
