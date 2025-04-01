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
package com.ctrip.framework.apollo.openapi.util;

import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Service
public class ConsumerAuthUtil {
  static final String CONSUMER_ID = "ApolloConsumerId";
  private final ConsumerService consumerService;

  public ConsumerAuthUtil(final ConsumerService consumerService) {
    this.consumerService = consumerService;
  }

  public Long getConsumerId(String token) {
    return consumerService.getConsumerIdByToken(token);
  }

  public ConsumerToken getConsumerToken(String token) {
    return consumerService.getConsumerTokenByToken(token);
  }

  public void storeConsumerId(HttpServletRequest request, Long consumerId) {
    request.setAttribute(CONSUMER_ID, consumerId);
  }

  public long retrieveConsumerId(HttpServletRequest request) {
    Object value = request.getAttribute(CONSUMER_ID);

    try {
      return Long.parseLong(value.toString());
    } catch (Throwable ex) {
      throw new IllegalStateException("No consumer id!", ex);
    }
  }

  // retrieve from RequestContextHolder
  public long retrieveConsumerIdFromCtx() {
    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      throw new IllegalStateException("No Request!");
    }
    HttpServletRequest request = attributes.getRequest();
    return retrieveConsumerId(request);
  }
}
