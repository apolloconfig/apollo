/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.core.dto;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloNotificationMessages {
  private Map<String, Long> details;

  public ApolloNotificationMessages() {
    this(Maps.<String, Long>newTreeMap());
  }

  private ApolloNotificationMessages(Map<String, Long> details) {
    this.details = details;
  }

  public void put(String key, long notificationId) {
    details.put(key, notificationId);
  }

  public Long get(String key) {
    return this.details.get(key);
  }

  public boolean has(String key) {
    return this.details.containsKey(key);
  }

  public boolean isEmpty() {
    return this.details.isEmpty();
  }

  public Map<String, Long> getDetails() {
    return details;
  }

  public void setDetails(Map<String, Long> details) {
    this.details = details;
  }

  public void mergeFrom(ApolloNotificationMessages source) {
    if (source == null) {
      return;
    }

    for (Map.Entry<String, Long> entry : source.getDetails().entrySet()) {
      //to make sure the notification id always grows bigger
      if (this.has(entry.getKey()) &&
          this.get(entry.getKey()) >= entry.getValue()) {
        continue;
      }
      this.put(entry.getKey(), entry.getValue());
    }
  }

  public ApolloNotificationMessages clone() {
    return new ApolloNotificationMessages(ImmutableMap.copyOf(this.details));
  }
}
