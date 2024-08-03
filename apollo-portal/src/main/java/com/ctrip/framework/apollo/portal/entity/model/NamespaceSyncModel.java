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
package com.ctrip.framework.apollo.portal.entity.model;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.portal.entity.vo.NamespaceIdentifier;

import org.springframework.util.CollectionUtils;

import java.util.List;

public class NamespaceSyncModel implements Verifiable {

  private List<NamespaceIdentifier> syncToNamespaces;
  private List<ItemDTO> syncItems;

  @Override
  public boolean isInvalid() {
    if (CollectionUtils.isEmpty(syncToNamespaces) || CollectionUtils.isEmpty(syncItems)) {
      return true;
    }
    for (NamespaceIdentifier namespaceIdentifier : syncToNamespaces) {
      if (namespaceIdentifier.isInvalid()) {
        return true;
      }
    }
    return false;
  }

  public boolean syncToNamespacesValid(String appId, String namespaceName) {
    for (NamespaceIdentifier namespaceIdentifier : syncToNamespaces) {
      if (appId.equals(namespaceIdentifier.getAppId()) && namespaceName.equals(
          namespaceIdentifier.getNamespaceName())) {
        continue;
      }
      return false;
    }
    return true;
  }

  public List<NamespaceIdentifier> getSyncToNamespaces() {
    return syncToNamespaces;
  }

  public void setSyncToNamespaces(List<NamespaceIdentifier> syncToNamespaces) {
    this.syncToNamespaces = syncToNamespaces;
  }

  public List<ItemDTO> getSyncItems() {
    return syncItems;
  }

  public void setSyncItems(List<ItemDTO> syncItems) {
    this.syncItems = syncItems;
  }
}
