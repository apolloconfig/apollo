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
package com.ctrip.framework.apollo.portal.spi.defaultimpl;

import com.google.common.collect.Lists;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.util.CollectionUtils;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultUserService implements UserService {

  private static final String DEFAULT_USER_ID = "apollo";

  @Override
  public List<UserInfo> searchUsers(String keyword, int offset, int limit, boolean includeInactiveUsers) {
    return Collections.singletonList(assembleDefaultUser());
  }

  @Override
  public UserInfo findByUserId(String userId) {
    if (Objects.equals(userId, DEFAULT_USER_ID)) {
      return assembleDefaultUser();
    }
    return null;
  }

  @Override
  public List<UserInfo> findByUserIds(List<String> userIds) {
    if (CollectionUtils.isEmpty(userIds)) {
      return Collections.emptyList();
    }

    if (userIds.contains(DEFAULT_USER_ID)) {
      return Lists.newArrayList(assembleDefaultUser());
    }
    return Collections.emptyList();
  }

  private UserInfo assembleDefaultUser() {
    UserInfo defaultUser = new UserInfo();
    defaultUser.setUserId(DEFAULT_USER_ID);
    defaultUser.setName(DEFAULT_USER_ID);
    defaultUser.setEmail("apollo@acme.com");

    return defaultUser;
  }
}
