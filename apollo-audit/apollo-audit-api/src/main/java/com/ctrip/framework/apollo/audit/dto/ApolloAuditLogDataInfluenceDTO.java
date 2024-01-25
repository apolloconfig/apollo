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
package com.ctrip.framework.apollo.audit.dto;

import java.util.Date;

public class ApolloAuditLogDataInfluenceDTO {

  private long id;
  private String spanId;
  private String influenceEntityName;
  private String influenceEntityId;
  private String fieldName;
  private String fieldOldValue;
  private String fieldNewValue;
  private Date happenedTime;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getSpanId() {
    return spanId;
  }

  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  public String getInfluenceEntityName() {
    return influenceEntityName;
  }

  public void setInfluenceEntityName(String influenceEntityName) {
    this.influenceEntityName = influenceEntityName;
  }

  public String getInfluenceEntityId() {
    return influenceEntityId;
  }

  public void setInfluenceEntityId(String influenceEntityId) {
    this.influenceEntityId = influenceEntityId;
  }

  public String getFieldName() {
    return fieldName;
  }

  public void setFieldName(String fieldName) {
    this.fieldName = fieldName;
  }

  public String getFieldOldValue() {
    return fieldOldValue;
  }

  public void setFieldOldValue(String fieldOldValue) {
    this.fieldOldValue = fieldOldValue;
  }

  public String getFieldNewValue() {
    return fieldNewValue;
  }

  public void setFieldNewValue(String fieldNewValue) {
    this.fieldNewValue = fieldNewValue;
  }

  public Date getHappenedTime() {
    return happenedTime;
  }

  public void setHappenedTime(Date happenedTime) {
    this.happenedTime = happenedTime;
  }
}
