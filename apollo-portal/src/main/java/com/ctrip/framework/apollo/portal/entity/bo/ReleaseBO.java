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
package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.portal.entity.bo.KVEntity;

import java.util.Set;

public class ReleaseBO {

  private ReleaseDTO baseInfo;

  private Set<KVEntity> items;

  public ReleaseDTO getBaseInfo() {
    return baseInfo;
  }

  public void setBaseInfo(ReleaseDTO baseInfo) {
    this.baseInfo = baseInfo;
  }

  public Set<KVEntity> getItems() {
    return items;
  }

  public void setItems(Set<KVEntity> items) {
    this.items = items;
  }

}
