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
package com.ctrip.framework.apollo.audit.component;

import com.ctrip.framework.apollo.audit.annotation.OpType;
import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class ApolloAuditContextFilter implements Filter {

  private final ApolloAuditLogApi api;

  public ApolloAuditContextFilter(ApolloAuditLogApi api) {
    this.api = api;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    Filter.super.init(filterConfig);
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest request = (HttpServletRequest) req;
    if(request.getMethod().equals("GET")) {
      chain.doFilter(req, resp);
      return;
    }

    AutoCloseable requestSpanScope = api.appendSpan(OpType.REQUEST, request.getRequestURI());

    chain.doFilter(req, resp);

    try {
      requestSpanScope.close();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void destroy() {
    Filter.super.destroy();
  }
}
