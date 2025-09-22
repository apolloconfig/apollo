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

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;

public class SpringSecurityUserInfoHolder implements UserInfoHolder {


  private final UserService userService;
  private final ConsumerService consumerService;

  public SpringSecurityUserInfoHolder(UserService userService,
                               @Lazy ConsumerService consumerService) {
    this.userService = userService;
    this.consumerService = consumerService;
  }

  @Override
  public UserInfo getUser() {
    // 首先尝试从OpenAPI Consumer上下文获取用户信息
    UserInfo consumerUserInfo = getConsumerUserInfo();
    if (consumerUserInfo != null) {
      return consumerUserInfo;
    }

    // 回退到Spring Security上下文
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
   * 从OpenAPI Consumer上下文获取用户信息
   */
  private UserInfo getConsumerUserInfo() {
    try {
      ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attributes == null) {
        return null;
      }

      HttpServletRequest request = attributes.getRequest();
      String requestURI = request.getRequestURI();

      // 只对OpenAPI请求处理Consumer用户信息
      if (!requestURI.startsWith("/openapi/")) {
        return null;
      }

      // 获取Consumer ID
      Object consumerIdObj = request.getAttribute("ApolloConsumerId");
      if (consumerIdObj == null) {
        return null;
      }

      long consumerId = Long.parseLong(consumerIdObj.toString());
      Consumer consumer = consumerService.getConsumerByConsumerId(consumerId);
      if (consumer == null) {
        return null;
      }

      // 构建基于Consumer的用户信息
      UserInfo userInfo = new UserInfo();
      userInfo.setUserId(consumer.getOwnerName());
      userInfo.setName(consumer.getName());
      userInfo.setEmail(consumer.getOwnerEmail());

      return userInfo;
    } catch (Exception e) {
      // 如果获取Consumer信息失败，返回null，让系统回退到默认方式
      return null;
    }
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
