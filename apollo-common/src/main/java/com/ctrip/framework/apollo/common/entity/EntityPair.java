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
package com.ctrip.framework.apollo.common.entity;

public class EntityPair<E> {

  private E firstEntity;
  private E secondEntity;

  public EntityPair(E firstEntity, E secondEntity){
    this.firstEntity = firstEntity;
    this.secondEntity = secondEntity;
  }

  public E getFirstEntity() {
    return firstEntity;
  }

  public void setFirstEntity(E firstEntity) {
    this.firstEntity = firstEntity;
  }

  public E getSecondEntity() {
    return secondEntity;
  }

  public void setSecondEntity(E secondEntity) {
    this.secondEntity = secondEntity;
  }
}
