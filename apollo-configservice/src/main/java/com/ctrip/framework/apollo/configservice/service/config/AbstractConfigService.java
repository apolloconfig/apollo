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
package com.ctrip.framework.apollo.configservice.service.config;

import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.grayReleaseRule.GrayReleaseRulesHolder;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;

import com.ctrip.framework.apollo.core.dto.ConfigurationChange;
import com.google.common.base.Strings;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public abstract class AbstractConfigService implements ConfigService {

  private final GrayReleaseRulesHolder grayReleaseRulesHolder;

  protected AbstractConfigService(final GrayReleaseRulesHolder grayReleaseRulesHolder) {
    this.grayReleaseRulesHolder = grayReleaseRulesHolder;
  }

  @Override
  public Release loadConfig(String clientAppId, String clientIp, String clientLabel, String configAppId, String configClusterName,
      String configNamespace, String dataCenter, ApolloNotificationMessages clientMessages) {
    // load from specified cluster first
    if (!Objects.equals(ConfigConsts.CLUSTER_NAME_DEFAULT, configClusterName)) {
      Release clusterRelease = findRelease(clientAppId, clientIp, clientLabel, configAppId, configClusterName, configNamespace,
          clientMessages);

      if (Objects.nonNull(clusterRelease)) {
        return clusterRelease;
      }
    }

    // try to load via data center
    if (!Strings.isNullOrEmpty(dataCenter) && !Objects.equals(dataCenter, configClusterName)) {
      Release dataCenterRelease = findRelease(clientAppId, clientIp, clientLabel, configAppId, dataCenter, configNamespace,
          clientMessages);
      if (Objects.nonNull(dataCenterRelease)) {
        return dataCenterRelease;
      }
    }

    // fallback to default release
    return findRelease(clientAppId, clientIp, clientLabel, configAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, configNamespace,
        clientMessages);
  }

  /**
   * Find release
   *
   * @param clientAppId the client's app id
   * @param clientIp the client ip
   * @param clientLabel the client label
   * @param configAppId the requested config's app id
   * @param configClusterName the requested config's cluster name
   * @param configNamespace the requested config's namespace name
   * @param clientMessages the messages received in client side
   * @return the release
   */
  private Release findRelease(String clientAppId, String clientIp, String clientLabel, String configAppId, String configClusterName,
      String configNamespace, ApolloNotificationMessages clientMessages) {
    Long grayReleaseId = grayReleaseRulesHolder.findReleaseIdFromGrayReleaseRule(clientAppId, clientIp, clientLabel, configAppId,
        configClusterName, configNamespace);

    Release release = null;

    if (grayReleaseId != null) {
      release = findActiveOne(grayReleaseId, clientMessages);
    }

    if (release == null) {
      release = findLatestActiveRelease(configAppId, configClusterName, configNamespace, clientMessages);
    }

    return release;
  }

  public List<ConfigurationChange> calcConfigurationChanges(
      Map<String, String> latestReleaseConfigurations, Map<String, String> historyConfigurations) {
    if (latestReleaseConfigurations == null) {
      latestReleaseConfigurations = new HashMap<>();
    }

    if (historyConfigurations == null) {
      historyConfigurations = new HashMap<>();
    }

    Set<String> previousKeys = historyConfigurations.keySet();
    Set<String> currentKeys = latestReleaseConfigurations.keySet();

    Set<String> commonKeys = Sets.intersection(previousKeys, currentKeys);
    Set<String> newKeys = Sets.difference(currentKeys, commonKeys);
    Set<String> removedKeys = Sets.difference(previousKeys, commonKeys);

    List<ConfigurationChange> changes = Lists.newArrayList();

    for (String newKey : newKeys) {
      changes.add(new ConfigurationChange(newKey, latestReleaseConfigurations.get(newKey),
          "ADDED"));
    }

    for (String removedKey : removedKeys) {
      changes.add(new ConfigurationChange(removedKey, null, "DELETED"));
    }

    for (String commonKey : commonKeys) {
      String previousValue = historyConfigurations.get(commonKey);
      String currentValue = latestReleaseConfigurations.get(commonKey);
      if (com.google.common.base.Objects.equal(previousValue, currentValue)) {
        continue;
      }
      changes.add(
          new ConfigurationChange(commonKey, currentValue, "MODIFIED"));
    }

    return changes;
  }

  @Override
  public Map<String, Release> findReleasesByReleaseKeys(Set<String> releaseKeys) {
    return null;
  }

  /**
   * Find active release by id
   */
  protected abstract Release findActiveOne(long id, ApolloNotificationMessages clientMessages);

  /**
   * Find active release by app id, cluster name and namespace name
   */
  protected abstract Release findLatestActiveRelease(String configAppId, String configClusterName,
      String configNamespaceName, ApolloNotificationMessages clientMessages);
}
