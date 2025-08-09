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

import com.ctrip.framework.apollo.core.dto.ConfigurationChange;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class DefaultIncrementalSyncService implements IncrementalSyncService {

  private final Cache<ReleaseKeyPair, List<ConfigurationChange>> configurationChangeCache;

  public DefaultIncrementalSyncService() {
    configurationChangeCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(10, TimeUnit.MINUTES)
        .build();
  }

  @Override
  public List<ConfigurationChange> getConfigurationChanges(String latestMergedReleaseKey,
      Map<String, String> latestReleaseConfigurations, String clientSideReleaseKey,
      Map<String, String> clientSideConfigurations) {

    ReleaseKeyPair key = new ReleaseKeyPair(clientSideReleaseKey, latestMergedReleaseKey);

    List<ConfigurationChange> cachedChanges = configurationChangeCache.getIfPresent(key);
    if (cachedChanges != null) {
      return cachedChanges;
    }

    List<ConfigurationChange> computed = calcConfigurationChanges(
        latestReleaseConfigurations, clientSideConfigurations);

    configurationChangeCache.put(key, computed);
    return computed;
  }

  private List<ConfigurationChange> calcConfigurationChanges(
      Map<String, String> latestReleaseConfigurations,
      Map<String, String> clientSideConfigurations) {

    if (latestReleaseConfigurations == null) {
      latestReleaseConfigurations = new HashMap<>();
    }

    if (clientSideConfigurations == null) {
      clientSideConfigurations = new HashMap<>();
    }

    Set<String> previousKeys = clientSideConfigurations.keySet();
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
      String previousValue = clientSideConfigurations.get(commonKey);
      String currentValue = latestReleaseConfigurations.get(commonKey);
      if (com.google.common.base.Objects.equal(previousValue, currentValue)) {
        continue;
      }
      changes.add(
          new ConfigurationChange(commonKey, currentValue, "MODIFIED"));
    }

    return changes;
  }

  public static class ReleaseKeyPair {

    private final String clientSideReleaseKey;
    private final String latestMergedReleaseKey;

    public ReleaseKeyPair(String clientSideReleaseKey, String latestMergedReleaseKey) {
      this.clientSideReleaseKey = clientSideReleaseKey;
      this.latestMergedReleaseKey = latestMergedReleaseKey;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof ReleaseKeyPair)) {
        return false;
      }
      ReleaseKeyPair that = (ReleaseKeyPair) obj;
      return Objects.equals(clientSideReleaseKey, that.clientSideReleaseKey) &&
          Objects.equals(latestMergedReleaseKey, that.latestMergedReleaseKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(clientSideReleaseKey, latestMergedReleaseKey);
    }
  }

}
