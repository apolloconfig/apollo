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

import com.ctrip.framework.apollo.common.exception.AuthorizedException;
import com.ctrip.framework.apollo.common.exception.BadRequestException;

import com.ctrip.framework.apollo.portal.util.JWTUtils;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import static com.ctrip.framework.apollo.portal.constant.JWTConstant.ID_TOKEN;
import static com.ctrip.framework.apollo.portal.constant.JWTConstant.PROVIDER;


@Component
public class JwtOidcAuthenticationFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private final UserDetailsService userDetailsService;
    private static final String[] BY_PASS_URLS = {
            "/vendor/**", "/styles/**", "/scripts/**", "/views/**", "/img/**", "/i18n/**",
            "/favicon.ico", "/default_sso_heartbeat.html", "/health", "/metrics/**","/prefix-path","/oauth2/**","/login/**","/refreshToken"
    };

    public JwtOidcAuthenticationFilter(JWTUtils jwtUtils, HandlerExceptionResolver handlerExceptionResolver, UserDetailsService userDetailsService) {
        this.jwtUtils = jwtUtils;

        this.handlerExceptionResolver = handlerExceptionResolver;

        this.userDetailsService = userDetailsService;
    }



    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (shouldByPass(request)) {
            filterChain.doFilter(request, response);
            return;
        }
                try {
                    String accessToken = jwtUtils.extractAccessTokenFromRequest(request);
                    if (ObjectUtils.isEmpty(accessToken)){
                        throw new BadRequestException("token is blank");
                    }
                    if (!jwtUtils.isValidToken(accessToken)) {
                        throw new AuthorizedException("token is invalid");
                    }

                    String username = jwtUtils.getUsernameFromToken(accessToken);
                    String idToken = jwtUtils.getClaimValue(accessToken, ID_TOKEN, String.class);
                    String provider = jwtUtils.getClaimValue(accessToken, PROVIDER, String.class);
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    OidcIdToken oidcIdToken = new OidcIdToken(
                            idToken,
                            Instant.now(),
                            Instant.now().plusSeconds(3600),
                            Collections.singletonMap("sub",username)
                    );

                    OidcUser oidcUser = new DefaultOidcUser(
                            userDetails.getAuthorities(),
                            oidcIdToken,
                            "sub"
                    );

                    OAuth2AuthenticationToken auth = new OAuth2AuthenticationToken(
                            oidcUser,
                            Collections.emptyList(),
                            provider
                    );
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;

                }
                catch (Exception e ) {
                    if (isApiRequest( request)){
                        handlerExceptionResolver.resolveException(request, response, null, e);
                    }
                    else {
                        filterChain.doFilter(request, response);
                    }

                }

        }

    private boolean shouldByPass(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : BY_PASS_URLS) {
            if (path.startsWith(pattern.replace("/**", ""))) {
                return true;
            }
        }
        return false;
    }
    public static boolean isApiRequest(HttpServletRequest request) {
        // 1. Exclude the most explicit document navigation
        String secFetchDest = request.getHeader("Sec-Fetch-Dest");
        if ("document".equalsIgnoreCase(secFetchDest)) {
            return false;
        }

        // 2. Traditional XMLHttpRequest flag
        String xRequestedWith = request.getHeader("X-Requested-With");
        if ("XMLHttpRequest".equalsIgnoreCase(xRequestedWith)) {
            return true;
        }

        // 3. Only consider it an API request when explicitly accepting structured data like JSON/XML
        String accept = request.getHeader("Accept");
        if (accept == null) {
            return false;
        }
        return accept.contains("application/json") ||
                accept.contains("application/xml") ||
                accept.contains("application/hal+json") ||
                accept.contains("application/vnd.api+json");
    }

}