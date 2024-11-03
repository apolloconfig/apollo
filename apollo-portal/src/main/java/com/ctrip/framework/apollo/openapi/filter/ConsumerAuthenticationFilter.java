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
package com.ctrip.framework.apollo.openapi.filter;

import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuditUtil;
import com.ctrip.framework.apollo.openapi.util.ConsumerAuthUtil;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.RateLimiter;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConsumerAuthenticationFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(ConsumerAuthenticationFilter.class);

  private final ConsumerAuthUtil consumerAuthUtil;
  private final ConsumerAuditUtil consumerAuditUtil;
  private final PortalConfig portalConfig;

  private static final int WARMUP_MILLIS = 1000; // ms
  private static final int RATE_LIMITER_CACHE_MAX_SIZE = 50000;

  private static final Cache<String, ImmutablePair<Long, RateLimiter>> LIMITER = CacheBuilder.newBuilder()
      .expireAfterWrite(1, TimeUnit.DAYS)
      .maximumSize(RATE_LIMITER_CACHE_MAX_SIZE).build();

  public ConsumerAuthenticationFilter(ConsumerAuthUtil consumerAuthUtil, ConsumerAuditUtil consumerAuditUtil, PortalConfig portalConfig) {
    this.consumerAuthUtil = consumerAuthUtil;
    this.consumerAuditUtil = consumerAuditUtil;
    this.portalConfig = portalConfig;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    //nothing
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain) throws
      IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;

    String token = request.getHeader(HttpHeaders.AUTHORIZATION);
    ConsumerToken consumerToken = consumerAuthUtil.getConsumerToken(token);

    if (null == consumerToken || consumerToken.getConsumerId() <= 0) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
      return;
    }

    Integer limitCount = consumerToken.getLimitCount();
    if (limitCount == null) {
      limitCount = 0;
    }
    if (portalConfig.isOpenApiLimitEnabled() && limitCount > 0) {
      try {
        ImmutablePair<Long, RateLimiter> rateLimiterPair = getOrCreateRateLimiterPair(token, limitCount);
        long warmupToMillis = rateLimiterPair.getLeft() + WARMUP_MILLIS;
        if (System.currentTimeMillis() > warmupToMillis && !rateLimiterPair.getRight().tryAcquire()) {
          response.sendError(HttpServletResponse.SC_FORBIDDEN, "Too many call requests, the flow is limited");
          return;
        }
      } catch (Exception e) {
        logger.error("ConsumerAuthenticationFilter ratelimit error", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Rate limiting failed");
        return;
      }
    }

    long consumerId = consumerToken.getConsumerId();
    consumerAuthUtil.storeConsumerId(request, consumerId);
    consumerAuditUtil.audit(request, consumerId);

    chain.doFilter(req, resp);
  }

  @Override
  public void destroy() {
    //nothing
  }

  private ImmutablePair<Long, RateLimiter> getOrCreateRateLimiterPair(String key, Integer limitCount) {
    ImmutablePair<Long, RateLimiter> rateLimiterPair = LIMITER.getIfPresent(key);
    if (rateLimiterPair == null) {
      rateLimiterPair = ImmutablePair.of(System.currentTimeMillis(), RateLimiter.create(limitCount));
      LIMITER.put(key, rateLimiterPair);
    }
    return rateLimiterPair;
  }

}
