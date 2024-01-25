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
package com.ctrip.framework.apollo.audit.context;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import java.util.Date;

public class ApolloAuditSpan {

  private OpType opType;
  private String opName;
  private String description;
  private Date startTime;
  private Date endTime;

  private ApolloAuditSpanContext context;

  public ApolloAuditSpanContext context() {
    return this.context;
  }

  //just do nothing
  public void finish() {
    endTime = new Date();
  }

  public void log() {
  }

  // sugar method
  public String spanId() {
    return context.getSpanId();
  }

  public String operator() {
    return context.getOperator();
  }

  public String traceId() {
    return context.getTraceId();
  }

  public String parentId() {
    return context.getParentId();
  }

  public String followsFromId() {
    return context.getFollowsFromId();
  }

  public OpType getOpType() {
    return opType;
  }

  public void setOpType(OpType opType) {
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

  public ApolloAuditSpanContext getContext() {
    return context;
  }

  public void setContext(ApolloAuditSpanContext context) {
    this.context = context;
  }

  public Date getStartTime() {
    return startTime;
  }

  public void setStartTime(Date startTime) {
    this.startTime = startTime;
  }

  public Date getEndTime() {
    return endTime;
  }

  public void setEndTime(Date endTime) {
    this.endTime = endTime;
  }
}