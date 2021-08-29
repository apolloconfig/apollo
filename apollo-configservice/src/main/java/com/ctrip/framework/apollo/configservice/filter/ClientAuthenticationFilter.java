/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.configservice.filter;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.configservice.util.AccessKeyUtil;
import com.ctrip.framework.apollo.core.signature.Signature;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.net.HttpHeaders;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
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
import org.springframework.util.CollectionUtils;

/**
 * @author nisiyong
 */
public class ClientAuthenticationFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(ClientAuthenticationFilter.class);

  private final BizConfig bizConfig;
  private final AccessKeyUtil accessKeyUtil;

  public ClientAuthenticationFilter(BizConfig bizConfig, AccessKeyUtil accessKeyUtil) {
    this.bizConfig = bizConfig;
    this.accessKeyUtil = accessKeyUtil;
  }

  @Override
  public void init(FilterConfig filterConfig) {
    //nothing
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;

    String appId = accessKeyUtil.extractAppIdFromRequest(request);
    if (StringUtils.isBlank(appId)) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "InvalidAppId");
      return;
    }

    List<String> availableSecrets = accessKeyUtil.findAvailableSecret(appId);
    if (!CollectionUtils.isEmpty(availableSecrets)) {
      String timestamp = request.getHeader(Signature.HTTP_HEADER_TIMESTAMP);
      String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);

      // check timestamp, valid within 1 minute
      if (!checkTimestamp(timestamp)) {
        logger.warn("Invalid timestamp. appId={},timestamp={}", appId, timestamp);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "RequestTimeTooSkewed");
        return;
      }

      // check signature
      String uri = request.getRequestURI();
      String query = request.getQueryString();
      if (!checkAuthorization(authorization, availableSecrets, timestamp, uri, query)) {
        logger.warn("Invalid authorization. appId={},authorization={}", appId, authorization);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
        return;
      }
    }

    chain.doFilter(request, response);
  }

  @Override
  public void destroy() {
    //nothing
  }

  private boolean checkTimestamp(String timestamp) {
    long requestTimeMillis = 0L;
    try {
      requestTimeMillis = Long.parseLong(timestamp);
    } catch (NumberFormatException e) {
      // nothing to do
    }

    long x = System.currentTimeMillis() - requestTimeMillis;
    long authTimeDiffToleranceInMillis = bizConfig.accessKeyAuthTimeDiffTolerance() * 1000L;
    return Math.abs(x) < authTimeDiffToleranceInMillis;
  }

  private boolean checkAuthorization(String authorization, List<String> availableSecrets,
      String timestamp, String path, String query) {

    String signature = null;
    if (authorization != null) {
      String[] split = authorization.split(":");
      if (split.length > 1) {
        signature = split[1];
      }
    }

    for (String secret : availableSecrets) {
      String availableSignature = accessKeyUtil.buildSignature(path, query, timestamp, secret);
      if (Objects.equals(signature, availableSignature)) {
        return true;
      }
    }
    return false;
  }
}
