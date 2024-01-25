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
package com.ctrip.framework.apollo.portal.entity.vo;

import com.ctrip.framework.apollo.common.entity.EntityPair;
import com.ctrip.framework.apollo.portal.entity.bo.KVEntity;
import com.ctrip.framework.apollo.portal.enums.ChangeType;

public class Change {

  private ChangeType type;
  private EntityPair<KVEntity> entity;

  public Change(ChangeType type, EntityPair<KVEntity> entity) {
    this.type = type;
    this.entity = entity;
  }

  public ChangeType getType() {
    return type;
  }

  public void setType(ChangeType type) {
    this.type = type;
  }

  public EntityPair<KVEntity> getEntity() {
    return entity;
  }

  public void setEntity(EntityPair<KVEntity> entity) {
    this.entity = entity;
  }
}
