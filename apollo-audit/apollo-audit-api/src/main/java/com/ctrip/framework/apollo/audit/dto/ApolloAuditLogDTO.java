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

public class ApolloAuditLogDTO {

  private long id;
  private String traceId;
  private String spanId;
  private String parentSpanId;
  private String followsFromSpanId;
  private String operator;
  private String opType;
  private String opName;
  private String description;
  private Date happenedTime;

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getTraceId() {
    return traceId;
  }

  public void setTraceId(String traceId) {
    this.traceId = traceId;
  }

  public String getSpanId() {
    return spanId;
  }

  public void setSpanId(String spanId) {
    this.spanId = spanId;
  }

  public String getParentSpanId() {
    return parentSpanId;
  }

  public void setParentSpanId(String parentSpanId) {
    this.parentSpanId = parentSpanId;
  }

  public String getFollowsFromSpanId() {
    return followsFromSpanId;
  }

  public void setFollowsFromSpanId(String followsFromSpanId) {
    this.followsFromSpanId = followsFromSpanId;
  }

  public String getOperator() {
    return operator;
  }

  public void setOperator(String operator) {
    this.operator = operator;
  }

  public String getOpType() {
    return opType;
  }

  public void setOpType(String opType) {
    this.opType = opType;
  }

  public String getOpName() {
    return opName;
  }

  public void setOpName(String opName) {
    this.opName = opName;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Date getHappenedTime() {
    return happenedTime;
  }

  public void setHappenedTime(Date happenedTime) {
    this.happenedTime = happenedTime;
  }
}
