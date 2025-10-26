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
package com.ctrip.framework.apollo.portal.spi.springsecurity;

import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

public class SpringSecurityUserInfoHolder implements UserInfoHolder {


    private final UserService userService;
    private final ConsumerService consumerService;
    private static final Logger logger = LoggerFactory.getLogger(SpringSecurityUserInfoHolder.class);

  public SpringSecurityUserInfoHolder(UserService userService,
                               @Lazy ConsumerService consumerService) {
    this.userService = userService;
    this.consumerService = consumerService;
  }

    @Override
    public UserInfo getUser() {
        // 1. try to retrieve user information from consumer context
        UserInfo consumerUserInfo = getConsumerUserInfo();
        if (consumerUserInfo != null) {
            return consumerUserInfo;
        }

        // 2. fall back to spring security context
        String userId = getCurrentUsername();
        UserInfo userInfoFound = userService.findByUserId(userId);
        if (userInfoFound != null) {
            return userInfoFound;
        }

        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(userId);
        return userInfo;
    }

    /**
     * Retrieve information from consumer context
     */
    private UserInfo getConsumerUserInfo() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }

        HttpServletRequest request = attributes.getRequest();
        String requestURI = request.getRequestURI();
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!path.startsWith("/openapi/")) {
            // not the openapi request
            return null;
        }

        // consumerAuthenticationFilter has already validated the request's consumer token must be legal
        String token = request.getHeader(HttpHeaders.AUTHORIZATION);
        ConsumerToken consumerToken = consumerService.getConsumerTokenByToken(token);
        if(consumerToken == null) {
            logger.error("Consumer token not found for OpenAPI request: {}", requestURI);
            throw new IllegalStateException("Consumer token not found for OpenAPI request: " + requestURI);
        }

        Consumer consumer = consumerService.getConsumerByConsumerId(consumerToken.getConsumerId());
        if (consumer == null || consumer.getOwnerName() == null) {
            logger.error("Consumer not found or incomplete, consumerId={}", consumerToken.getConsumerId());
            throw new IllegalStateException("Consumer information is missing for consumerId "
                    + consumerToken.getConsumerId());
        }

        UserInfo consumerUserInfo = userService.findByUserId(consumer.getOwnerName());
        if (consumerUserInfo == null) {
            logger.error("Consumer not found or incomplete, consumer={}", consumer.getOwnerName());
            throw  new IllegalStateException("Consumer information is missing for owner of consumer " +
                    consumer.getOwnerName());
        }
        return consumerUserInfo;
    }

    /**
     * 从Spring Security上下文获取用户名
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        if (principal instanceof Principal) {
            return ((Principal) principal).getName();
        }
        return String.valueOf(principal);
    }
}
