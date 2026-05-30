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
package com.ctrip.framework.apollo.configservice.wrapper;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DeferredResultWrapper implements Comparable<DeferredResultWrapper> {
  private static final ResponseEntity<List<ApolloConfigNotification>> NOT_MODIFIED_RESPONSE_LIST =
      new ResponseEntity<>(HttpStatus.NOT_MODIFIED);

  private Map<String, String> normalizedNamespaceNameToOriginalNamespaceName;
  private DeferredResult<ResponseEntity<?>> result;


  public DeferredResultWrapper(long timeoutInMilli) {
    result = new DeferredResult<>(timeoutInMilli, NOT_MODIFIED_RESPONSE_LIST);
  }

  public void recordNamespaceNameNormalizedResult(String originalNamespaceName,
      String normalizedNamespaceName) {
    if (normalizedNamespaceNameToOriginalNamespaceName == null) {
      normalizedNamespaceNameToOriginalNamespaceName = Maps.newHashMap();
    }
    normalizedNamespaceNameToOriginalNamespaceName.put(normalizedNamespaceName,
        originalNamespaceName);
  }


  public void onTimeout(Runnable timeoutCallback) {
    result.onTimeout(timeoutCallback);
  }

  public void onCompletion(Runnable completionCallback) {
    result.onCompletion(completionCallback);
  }


  public void setResult(ApolloConfigNotification notification) {
    setResult(Lists.newArrayList(notification));
  }

  public void setResult(String namespaceName, ApolloConfigNotification notification,
      ResponseEntity<String> serializedNotificationResponse) {
    if (!shouldRestoreOriginalNamespaceName(namespaceName)) {
      result.setResult(serializedNotificationResponse);
      return;
    }
    setResult(notification);
  }

  /**
   * The namespace name is used as a key in client side, so we have to return the original
   * one instead of the correct one.
   */
  public void setResult(List<ApolloConfigNotification> notifications) {
    if (normalizedNamespaceNameToOriginalNamespaceName != null) {
      // Most responses can reuse the shared notification object. Copy only when
      // this wrapper must restore a client-supplied namespace name, because that
      // restoration mutates the DTO and must not affect other long-poll clients.
      List<ApolloConfigNotification> notificationsToReturn = notifications;
      for (int i = 0; i < notifications.size(); i++) {
        ApolloConfigNotification notification = notifications.get(i);
        if (!normalizedNamespaceNameToOriginalNamespaceName
            .containsKey(notification.getNamespaceName())) {
          continue;
        }
        if (notificationsToReturn == notifications) {
          notificationsToReturn = Lists.newArrayList(notifications);
        }
        ApolloConfigNotification copiedNotification = copyApolloConfigNotification(notification);
        copiedNotification.setNamespaceName(
            normalizedNamespaceNameToOriginalNamespaceName.get(notification.getNamespaceName()));
        notificationsToReturn.set(i, copiedNotification);
      }
      notifications = notificationsToReturn;
    }

    result.setResult(new ResponseEntity<>(notifications, HttpStatus.OK));
  }

  public DeferredResult<ResponseEntity<?>> getResult() {
    return result;
  }

  private boolean shouldRestoreOriginalNamespaceName(String namespaceName) {
    return normalizedNamespaceNameToOriginalNamespaceName != null
        && normalizedNamespaceNameToOriginalNamespaceName.containsKey(namespaceName);
  }

  private ApolloConfigNotification copyApolloConfigNotification(
      ApolloConfigNotification notification) {
    ApolloConfigNotification copiedNotification = new ApolloConfigNotification(
        notification.getNamespaceName(), notification.getNotificationId());
    notification.getMessages().getDetails().forEach(copiedNotification::addMessage);
    return copiedNotification;
  }

  @Override
  public int compareTo(@NonNull DeferredResultWrapper deferredResultWrapper) {
    return Integer.compare(this.hashCode(), deferredResultWrapper.hashCode());
  }
}
