/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.util;

import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import com.ctrip.framework.apollo.portal.entity.po.UserTokenAudit;
import com.ctrip.framework.apollo.portal.service.UserTokenService;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Service
public class UserTokenAuditUtil implements InitializingBean {

  private static final int USER_TOKEN_AUDIT_MAX_SIZE = 10000;
  private static final int BATCH_SIZE = 100;
  private static long BATCH_TIMEOUT = 5;
  private static TimeUnit BATCH_TIMEUNIT = TimeUnit.SECONDS;

  private final BlockingQueue<UserTokenAudit> audits =
      Queues.newLinkedBlockingQueue(USER_TOKEN_AUDIT_MAX_SIZE);
  private final ExecutorService auditExecutorService;
  private final AtomicBoolean auditStopped;
  private final UserTokenService userTokenService;

  public UserTokenAuditUtil(final UserTokenService userTokenService) {
    this.userTokenService = userTokenService;
    auditExecutorService =
        Executors.newSingleThreadExecutor(ApolloThreadFactory.create("UserTokenAuditUtil", true));
    auditStopped = new AtomicBoolean(false);
  }

  public boolean audit(HttpServletRequest request, UserToken userToken) {
    if ("GET".equalsIgnoreCase(request.getMethod())) {
      return true;
    }

    String uri = request.getRequestURI();
    if (!Strings.isNullOrEmpty(request.getQueryString())) {
      uri += "?" + request.getQueryString();
    }

    Date now = new Date();
    UserTokenAudit userTokenAudit = new UserTokenAudit();
    userTokenAudit.setTokenId(userToken.getId());
    userTokenAudit.setUserId(userToken.getUserId());
    userTokenAudit.setUri(uri);
    userTokenAudit.setMethod(request.getMethod());
    userTokenAudit.setDataChangeCreatedTime(now);
    userTokenAudit.setDataChangeLastModifiedTime(now);

    return this.audits.offer(userTokenAudit);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    auditExecutorService.submit(() -> {
      while (!auditStopped.get() && !Thread.currentThread().isInterrupted()) {
        List<UserTokenAudit> toAudit = Lists.newArrayList();
        try {
          Queues.drain(audits, toAudit, BATCH_SIZE, BATCH_TIMEOUT, BATCH_TIMEUNIT);
          if (!toAudit.isEmpty()) {
            userTokenService.createUserTokenAudits(toAudit);
          }
        } catch (Throwable ex) {
          Tracer.logError(ex);
        }
      }
    });
  }

  public void stopAudit() {
    auditStopped.set(true);
  }
}
