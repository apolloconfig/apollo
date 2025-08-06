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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.dto.UserLoginDto;
import com.ctrip.framework.apollo.common.exception.AuthorizedException;
import com.ctrip.framework.apollo.portal.entity.vo.UserToken;
import com.ctrip.framework.apollo.portal.util.CookieUtils;
import com.ctrip.framework.apollo.portal.util.JWTUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.time.Duration;
import javax.servlet.http.Cookie;

import static com.ctrip.framework.apollo.portal.constant.JWTConstant.REFRESH_TOKEN;

@Service
public class LoginService {

    private final AuthenticationManager authenticationManager;
    private final JWTUtils jwtUtils;


    public LoginService(final AuthenticationManager authenticationManager, JWTUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
    }
    public UserToken login(UserLoginDto userLoginDto, HttpServletResponse response) {
        try {
            UserToken userToken = new UserToken();

            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    userLoginDto.getUsername(), userLoginDto.getPassword());
            Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();

            String accessToken = jwtUtils.generateAccessToken(userDetails.getUsername());
            String refreshToken = jwtUtils.generateRefreshToken(userDetails.getUsername());
            userToken.setAccessToken(accessToken);
            setRefreshTokenCookie(response, refreshToken);
            return userToken;
        } catch (BadCredentialsException e) {
            throw new AuthorizedException("username or password not correct");
        }
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        CookieUtils.deleteCookie(request, response, REFRESH_TOKEN);
    }
    public UserToken refreshToken(HttpServletRequest request) {
        String refreshToken = CookieUtils.getCookie(request, REFRESH_TOKEN)
                .map(Cookie::getValue)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new AuthorizedException("refresh token not found"));

        if (!jwtUtils.isValidToken(refreshToken)) {
            throw new AuthorizedException("refresh token is invalid");
        }
        String username = jwtUtils.getUsernameFromToken(refreshToken);

        UserToken userToken = new UserToken();
        userToken.setAccessToken(jwtUtils.generateAccessToken(username));
        return userToken;
    }


    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {

        CookieUtils.addCookie(response, REFRESH_TOKEN, refreshToken, (int) Duration.ofDays(7).getSeconds());
    }
}
