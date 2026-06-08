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
package com.ctrip.framework.apollo.portal.util;

import com.ctrip.framework.apollo.portal.entity.po.UserToken;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class UserTokenAuthUtil {

  public static final String USER_TOKEN = "ApolloUserToken";

  public void storeUserToken(HttpServletRequest request, UserToken userToken) {
    request.setAttribute(USER_TOKEN, userToken);
  }

  public UserToken retrieveUserToken(HttpServletRequest request) {
    Object value = request.getAttribute(USER_TOKEN);
    if (value instanceof UserToken) {
      return (UserToken) value;
    }
    throw new IllegalStateException("No user token!");
  }

  public UserToken retrieveUserTokenFromCtx() {
    ServletRequestAttributes attributes =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attributes == null) {
      throw new IllegalStateException("No Request!");
    }
    return retrieveUserToken(attributes.getRequest());
  }
}
