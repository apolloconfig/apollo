/*
 * Copyright 2022 Apollo Authors
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

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Authority;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.repository.AuthorityRepository;
import com.ctrip.framework.apollo.portal.repository.UserRepository;
import com.ctrip.framework.apollo.portal.spi.UserService;

import java.util.HashMap;
import java.util.Map;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lepdou 2017-03-10
 */
public class SpringSecurityUserService implements UserService {

  private final PasswordEncoder passwordEncoder;

  private final UserRepository userRepository;

  private final AuthorityRepository authorityRepository;

  public SpringSecurityUserService(
      PasswordEncoder passwordEncoder,
      UserRepository userRepository,
      AuthorityRepository authorityRepository) {
    this.passwordEncoder = passwordEncoder;
    this.userRepository = userRepository;
    this.authorityRepository = authorityRepository;
  }

  @Transactional
  public void createOrUpdate(UserPO user) {
    String username = user.getUsername();
    String newPassword = passwordEncoder.encode(user.getPassword());

    UserPO managedUser = userRepository.findByUsername(username);
    if (managedUser == null) {
      user.setPassword(newPassword);
      user.setEnabled(1);
      userRepository.save(user);

      //save authorities
      Authority authority = new Authority();
      authority.setUsername(username);
      authority.setAuthority("ROLE_user");
      authorityRepository.save(authority);
    } else {
      managedUser.setPassword(newPassword);
      managedUser.setEmail(user.getEmail());
      managedUser.setUserDisplayName(user.getUserDisplayName());
      userRepository.save(managedUser);
    }
  }

  @Override
  public List<UserInfo> searchUsers(String keyword, int offset, int limit) {
    List<UserPO> users = this.findUsers(keyword);
    if (CollectionUtils.isEmpty(users)) {
      return Collections.emptyList();
    }
    return users.stream().map(UserPO::toUserInfo)
        .collect(Collectors.toList());
  }

  private List<UserPO> findUsers(String keyword) {
    if (StringUtils.isEmpty(keyword)) {
      return userRepository.findFirst20ByEnabled(1);
    }
    Map<Long, UserPO> users = new HashMap<>();
    List<UserPO> byUsername = userRepository
        .findByUsernameLikeAndEnabled("%" + keyword + "%", 1);
    List<UserPO> byUserDisplayName = userRepository
        .findByUserDisplayNameLikeAndEnabled("%" + keyword + "%", 1);
    if (!CollectionUtils.isEmpty(byUsername)) {
      for (UserPO user : byUsername) {
        users.put(user.getId(), user);
      }
    }
    if (!CollectionUtils.isEmpty(byUserDisplayName)) {
      for (UserPO user : byUserDisplayName) {
        users.put(user.getId(), user);
      }
    }
    return new ArrayList<>(users.values());
  }

  @Override
  public UserInfo findByUserId(String userId) {
    UserPO userPO = userRepository.findByUsername(userId);
    return userPO == null ? null : userPO.toUserInfo();
  }

  @Override
  public List<UserInfo> findByUserIds(List<String> userIds) {
    List<UserPO> users = userRepository.findByUsernameIn(userIds);

    if (CollectionUtils.isEmpty(users)) {
      return Collections.emptyList();
    }

    return users.stream().map(UserPO::toUserInfo).collect(Collectors.toList());
  }
}
