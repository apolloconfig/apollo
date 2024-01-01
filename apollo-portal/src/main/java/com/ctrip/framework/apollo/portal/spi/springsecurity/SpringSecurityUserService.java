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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
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
  public void create(UserPO user) {
    String username = user.getUsername();
    String newPassword = passwordEncoder.encode(user.getPassword());
    UserPO managedUser = userRepository.findByUsername(username);
    if (managedUser != null) {
      throw BadRequestException.userAlreadyExists(username);
    }
    //create
    user.setPassword(newPassword);
    user.setEnabled(user.getEnabled());
    userRepository.save(user);

    //save authorities
    Authority authority = new Authority();
    authority.setUsername(username);
    authority.setAuthority("ROLE_user");
    authorityRepository.save(authority);
  }

  @Transactional
  public void update(UserPO user) {
    String username = user.getUsername();
    String newPassword = passwordEncoder.encode(user.getPassword());
    UserPO managedUser = userRepository.findByUsername(username);
    if (managedUser == null) {
      throw BadRequestException.userNotExists(username);
    }
    managedUser.setPassword(newPassword);
    managedUser.setEmail(user.getEmail());
    managedUser.setUserDisplayName(user.getUserDisplayName());
    managedUser.setEnabled(user.getEnabled());
    userRepository.save(managedUser);
  }

  @Transactional
  public void changeEnabled(UserPO user) {
    String username = user.getUsername();
    UserPO managedUser = userRepository.findByUsername(username);
    managedUser.setEnabled(user.getEnabled());
    userRepository.save(managedUser);
  }

  @Override
  public List<UserInfo> searchUsers(String keyword, int offset, int limit,
      boolean includeInactiveUsers) {
    List<UserPO> users = this.findUsers(keyword, includeInactiveUsers);
    if (CollectionUtils.isEmpty(users)) {
      return Collections.emptyList();
    }
    return users.stream().map(UserPO::toUserInfo)
        .collect(Collectors.toList());
  }

  private List<UserPO> findUsers(String keyword, boolean includeInactiveUsers) {
    Map<Long, UserPO> users = new HashMap<>();
    List<UserPO> byUsername;
    List<UserPO> byUserDisplayName;
    if (includeInactiveUsers) {
      if (StringUtils.isEmpty(keyword)) {
        return (List<UserPO>) userRepository.findAll();
      }
      byUsername = userRepository.findByUsernameLike("%" + keyword + "%");
      byUserDisplayName = userRepository.findByUserDisplayNameLike("%" + keyword + "%");
    } else {
      if (StringUtils.isEmpty(keyword)) {
        return userRepository.findFirst20ByEnabled(1);
      }
      byUsername = userRepository.findByUsernameLikeAndEnabled("%" + keyword + "%", 1);
      byUserDisplayName = userRepository
          .findByUserDisplayNameLikeAndEnabled("%" + keyword + "%", 1);
    }
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
