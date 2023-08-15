/*
 * Copyright 2023 Apollo Authors
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApolloAuditSpan {

  private final static Logger logger = LoggerFactory.getLogger(ApolloAuditSpan.class);

  private String parentId;
  private String followsFromId;

  private OpType opType;
  private String opName;
  private String description;

  private ApolloAuditSpanContext spanContext;

  public ApolloAuditSpanContext context(){
    return this.spanContext;
  }

  //just do nothing
  public void finish(){}

  public String id() {
    return spanContext.getSpanId();
  }

  public String operator() {
    return spanContext.getOperator();
  }

  public String traceId() {
    return spanContext.getTraceId();
  }

  public void log(){
    logger.info("Span of " + this.opName + " generated!");
  }

  public String getParentId() {
    return parentId;
  }

  public void setParentId(String parentId) {
    this.parentId = parentId;
  }

  public String getFollowsFromId() {
    return followsFromId;
  }

  public void setFollowsFromId(String followsFromId) {
    this.followsFromId = followsFromId;
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

  public ApolloAuditSpanContext getSpanContext() {
    return spanContext;
  }

  public void setSpanContext(ApolloAuditSpanContext spanContext) {
    this.spanContext = spanContext;
  }
}