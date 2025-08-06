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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.ObjectUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JWTUtils jwtUtils;
    private final UserDetailsService userDetailsService;
    private final HandlerExceptionResolver handlerExceptionResolver;
    private static final String[] BY_PASS_URLS = {
            "/vendor/**", "/styles/**", "/scripts/**", "/views/**", "/img/**", "/i18n/**",
            "/favicon.ico", "/default_sso_heartbeat.html", "/health", "/metrics/**","/prefix-path","/login","/refreshToken"
    };

    public JwtAuthenticationFilter(JWTUtils jwtUtils, UserDetailsService service, HandlerExceptionResolver resolver) {
        this.jwtUtils = jwtUtils;
        this.userDetailsService = service;
        this.handlerExceptionResolver = resolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        try {
            if (isExcludePath(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            String token = jwtUtils.extractAccessTokenFromRequest(request);

            if (ObjectUtils.isEmpty(token)){
                throw new BadRequestException("token is blank");
            }
            if (!jwtUtils.isValidToken(token)) {
                throw new AuthorizedException("token is invalid");
            }

            String username = jwtUtils.getUsernameFromToken(token);

            UserDetails user = userDetailsService.loadUserByUsername(username);
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(auth);

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            handlerExceptionResolver.resolveException(request, response, null, e);
        }
    }



    private boolean isExcludePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        for (String pattern : BY_PASS_URLS) {
            if (path.startsWith(pattern.replace("/**", ""))) {
                return true;
            }
        }
        return false;
    }
}

