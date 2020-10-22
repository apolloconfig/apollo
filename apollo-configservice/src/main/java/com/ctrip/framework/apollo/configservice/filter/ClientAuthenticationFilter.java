package com.ctrip.framework.apollo.configservice.filter;

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

  private static final Long TIMESTAMP_INTERVAL = 60 * 1000L;

  private final AccessKeyUtil accessKeyUtil;

  public ClientAuthenticationFilter(AccessKeyUtil accessKeyUtil) {
    this.accessKeyUtil = accessKeyUtil;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
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
    return x >= -TIMESTAMP_INTERVAL && x <= TIMESTAMP_INTERVAL;
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
