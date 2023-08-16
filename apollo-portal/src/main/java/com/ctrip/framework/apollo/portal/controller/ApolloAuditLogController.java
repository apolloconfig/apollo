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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.service.ApolloAuditLogService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/audit/log")
public class ApolloAuditLogController {

  private final ApolloAuditLogService auditLogService;
  private final UserInfoHolder userInfoHolder;

  public ApolloAuditLogController(ApolloAuditLogService auditLogService,
      UserInfoHolder userInfoHolder) {
    this.auditLogService = auditLogService;
    this.userInfoHolder = userInfoHolder;
  }

  @GetMapping("/by-traceId")
  public List<ApolloAuditLog> findAuditLogByTraceId(String traceId) {
    return auditLogService.findAuditLogByTraceId(traceId);
  }
}
