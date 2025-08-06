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
package com.ctrip.framework.apollo.portal.spi.oidc;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import com.ctrip.framework.apollo.portal.util.JWTUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static com.ctrip.framework.apollo.portal.constant.JWTConstant.*;

@Component
public class JwtAuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final JWTUtils jwtUtils;

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    public JwtAuthenticationSuccessHandler(JWTUtils jwtUtils,HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository) {
        this.jwtUtils = jwtUtils;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException {

        String userIdentifier = extractUserIdentifier(authentication);
        OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        Map<String, Object> payload = new HashMap<>();
        payload.put(PROVIDER, oAuth2AuthenticationToken.getAuthorizedClientRegistrationId());
        payload.put(ID_TOKEN, oidcUser.getIdToken().getTokenValue());
        String accessToken = jwtUtils.generateAccessToken(userIdentifier, payload);
        String refreshToken = jwtUtils.generateRefreshToken(userIdentifier, payload);

        ResponseCookie cookie = ResponseCookie.from(REFRESH_TOKEN, refreshToken)
                .httpOnly(true)
                .path("/")
                .sameSite("Lax")
                .maxAge(Duration.ofDays(7))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        String landing = request.getContextPath()
                + "/landing.html?accessToken=" + accessToken;
        response.sendRedirect(landing);
    }

    private String extractUserIdentifier(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof OidcUser) {
            return ((OidcUser) principal).getSubject();
        } else if (principal instanceof Jwt) {
            return ((Jwt) principal).getSubject();
        } else if (principal instanceof OAuth2User) {
            return ((OAuth2User) principal).getName();
        } else if (principal instanceof Principal) {
            return ((Principal) principal).getName();
        }
        return String.valueOf(principal);
    }
}