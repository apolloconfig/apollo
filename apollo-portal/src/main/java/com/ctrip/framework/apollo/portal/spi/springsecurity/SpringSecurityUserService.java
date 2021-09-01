/*
 * Copyright 2021 Apollo Authors
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
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.repository.UserRepository;
import com.ctrip.framework.apollo.portal.spi.UserService;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author lepdou 2017-03-10
 */
public class SpringSecurityUserService implements UserService {

  private final List<GrantedAuthority> authorities = Collections
      .unmodifiableList(Arrays.asList(new SimpleGrantedAuthority("ROLE_user")));

  private final PasswordEncoder passwordEncoder;

  private final JdbcUserDetailsManager userDetailsManager;

  private final UserRepository userRepository;

  public SpringSecurityUserService(
      PasswordEncoder passwordEncoder,
      JdbcUserDetailsManager userDetailsManager,
      UserRepository userRepository) {
    this.passwordEncoder = passwordEncoder;
    this.userDetailsManager = userDetailsManager;
    this.userRepository = userRepository;
  }

  @Transactional
  public void createOrUpdate(UserPO user) {
    String username = user.getUsername();

    User userDetails = new User(username, passwordEncoder.encode(user.getPassword()), authorities);

    if (userDetailsManager.userExists(username)) {
      userDetailsManager.updateUser(userDetails);
    } else {
      userDetailsManager.createUser(userDetails);
    }

    UserPO managedUser = userRepository.findByUsername(username);
    managedUser.setEmail(user.getEmail());
    managedUser.setUserDisplayName(user.getUserDisplayName());

    userRepository.save(managedUser);
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
    List<UserPO> users = new ArrayList<>();
    List<UserPO> byUsername = userRepository
        .findByUsernameLikeAndEnabled("%" + keyword + "%", 1);
    List<UserPO> byUserDisplayName = userRepository
        .findByUserDisplayNameLikeAndEnabled("%" + keyword + "%", 1);
    if (!CollectionUtils.isEmpty(byUsername)) {
      users.addAll(byUsername);
    }
    if (!CollectionUtils.isEmpty(byUserDisplayName)) {
      users.addAll(byUserDisplayName);
    }
    return users;
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
