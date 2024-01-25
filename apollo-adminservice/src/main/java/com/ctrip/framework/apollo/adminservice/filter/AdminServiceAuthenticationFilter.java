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
package com.ctrip.framework.apollo.adminservice.filter;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.io.IOException;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

public class AdminServiceAuthenticationFilter implements Filter {

  private static final Logger logger = LoggerFactory
      .getLogger(AdminServiceAuthenticationFilter.class);
  private static final Splitter ACCESS_TOKEN_SPLITTER = Splitter.on(",").omitEmptyStrings()
      .trimResults();

  private final BizConfig bizConfig;
  private volatile String lastAccessTokens;
  private volatile List<String> accessTokenList;

  public AdminServiceAuthenticationFilter(BizConfig bizConfig) {
    this.bizConfig = bizConfig;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    if (bizConfig.isAdminServiceAccessControlEnabled()) {
      HttpServletRequest request = (HttpServletRequest) req;
      HttpServletResponse response = (HttpServletResponse) resp;

      String token = request.getHeader(HttpHeaders.AUTHORIZATION);

      if (!checkAccessToken(token)) {
        logger.warn("Invalid access token: {} for uri: {}", token, request.getRequestURI());
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        return;
      }
    }

    chain.doFilter(req, resp);
  }

  private boolean checkAccessToken(String token) {
    String accessTokens = bizConfig.getAdminServiceAccessTokens();

    // if user forget to configure access tokens, then default to pass
    if (Strings.isNullOrEmpty(accessTokens)) {
      return true;
    }

    // no need to check
    if (Strings.isNullOrEmpty(token)) {
      return false;
    }

    // update cache
    if (!accessTokens.equals(lastAccessTokens)) {
      synchronized (this) {
        accessTokenList = ACCESS_TOKEN_SPLITTER.splitToList(accessTokens);
        lastAccessTokens = accessTokens;
      }
    }

    return accessTokenList.contains(token);
  }

  @Override
  public void destroy() {

  }
}
