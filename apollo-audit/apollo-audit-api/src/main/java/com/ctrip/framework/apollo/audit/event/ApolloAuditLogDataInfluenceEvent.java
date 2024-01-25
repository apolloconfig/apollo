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
package com.ctrip.framework.apollo.audit.event;

public class ApolloAuditLogDataInfluenceEvent {

  private Class<?> beanDefinition;
  private Object entity;

  public ApolloAuditLogDataInfluenceEvent(Class<?> beanDefinition, Object entity) {
    this.beanDefinition = beanDefinition;
    this.entity = entity;
  }

  public Class<?> getBeanDefinition() {
    return beanDefinition;
  }

  public void setBeanDefinition(Class<?> beanDefinition) {
    this.beanDefinition = beanDefinition;
  }

  public Object getEntity() {
    return entity;
  }

  public void setEntity(Object entity) {
    this.entity = entity;
  }
}
