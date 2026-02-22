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
package com.ctrip.framework.apollo.portal.filter;

import java.io.IOException;
import java.util.Arrays;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

/**
 * Filter to handle Portal user session validation for OpenAPI requests. This filter runs before
 * ConsumerAuthenticationFilter to detect and handle: 1. Authenticated Portal users - allow them to
 * access OpenAPI 2. Expired Portal sessions - handle based on authentication mode: - auth/ldap:
 * redirect to /signin (form login page) - oidc: return 401 (let frontend handle or trigger OAuth2
 * flow) - those three login methods' experiences are the same as before
 * <p>
 * This keeps the ConsumerAuthenticationFilter focused solely on Consumer Token validation.
 */
public class PortalUserSessionFilter implements Filter {

  private static final Logger logger = LoggerFactory.getLogger(PortalUserSessionFilter.class);

  private static final String SESSION_COOKIE_NAME = "SESSION";
  private static final String OIDC_PROFILE = "oidc";
  private static final String PORTAL_USER_AUTHENTICATED = "PORTAL_USER_AUTHENTICATED";
  private static final LoginUrlAuthenticationEntryPoint LOGIN_ENTRY_POINT =
      new LoginUrlAuthenticationEntryPoint("/signin");

  private final Environment environment;

  public PortalUserSessionFilter(Environment environment) {
    this.environment = environment;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    // nothing
  }

  @Override
  public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest request = (HttpServletRequest) req;
    HttpServletResponse response = (HttpServletResponse) resp;

    // Check if the user is an authenticated Portal user
    if (isAuthenticatedPortalUser(request)) {
      // Portal user is authenticated, allow access to OpenAPI
      logger.debug("Authenticated portal user accessing OpenAPI: {}", request.getRequestURI());
      request.setAttribute(PORTAL_USER_AUTHENTICATED, true);
      chain.doFilter(req, resp);
      return;
    }

    // Check if there's a session cookie but user is not authenticated
    // This indicates the session has expired
    if (hasSessionCookie(request)) {
      logger.info(
          "Request has SESSION cookie but user is not authenticated - session is expired. URI: {}",
          request.getRequestURI());

      handleSessionExpired(request, response);
      return;
    }

    // Neither authenticated Portal user nor expired session
    // Continue to next filter (ConsumerAuthenticationFilter) for token validation
    chain.doFilter(req, resp);
  }

  @Override
  public void destroy() {
    // nothing
  }

  /**
   * Determines whether the current request is from an authenticated Portal user by checking Spring
   * Security's SecurityContext.
   *
   * @param request the HTTP request
   * @return true if authenticated Portal user, false otherwise
   */
  private boolean isAuthenticatedPortalUser(HttpServletRequest request) {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

      // Check if there is authentication information and it has been authenticated
      if (authentication != null && authentication.isAuthenticated()) {
        // Exclude anonymous users
        String principal = authentication.getName();
        if (principal != null && !"anonymousUser".equals(principal)) {
          logger.debug("Authenticated portal user: {} accessing OpenAPI: {}", principal,
              request.getRequestURI());
          return true;
        }
      }
    } catch (Exception e) {
      logger.debug("Failed to get authentication from SecurityContext", e);
    }

    return false;
  }

  /**
   * Checks if the request has a session cookie. This is used to detect expired sessions.
   *
   * @param request the HTTP request
   * @return true if SESSION cookie exists, false otherwise
   */
  private boolean hasSessionCookie(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Handles expired session based on authentication mode. - auth/ldap: redirect to /signin (form
   * login page) - oidc: return 401 (maintains original behavior, frontend can handle)
   *
   * @param request  the HTTP request
   * @param response the HTTP response
   */
  private void handleSessionExpired(HttpServletRequest request, HttpServletResponse response)
      throws IOException, ServletException {
    if (isOidcProfile()) {
      // OIDC mode: return 401 to maintain original behavior
      logger.debug("OIDC mode: returning 401 for expired session");
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Session expired");
    } else {
      // Auth/LDAP mode: reuse LoginUrlAuthenticationEntryPoint for consistent redirect handling
      logger.debug(
          "Auth/LDAP mode: delegating to LoginUrlAuthenticationEntryPoint for login redirect");
      LOGIN_ENTRY_POINT.commence(request, response, null);
    }
  }

  /**
   * Checks if the current active profile is OIDC.
   *
   * @return true if OIDC profile is active, false otherwise
   */
  private boolean isOidcProfile() {
    if (environment != null) {
      return Arrays.asList(environment.getActiveProfiles()).contains(OIDC_PROFILE);
    }
    return false;
  }

}
