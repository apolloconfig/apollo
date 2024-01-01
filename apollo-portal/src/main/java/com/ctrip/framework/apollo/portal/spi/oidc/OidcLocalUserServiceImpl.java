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
package com.ctrip.framework.apollo.portal.spi.oidc;

import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.repository.UserRepository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class OidcLocalUserServiceImpl implements OidcLocalUserService {

  private final Collection<? extends GrantedAuthority> authorities = Collections
      .singletonList(new SimpleGrantedAuthority("ROLE_USER"));

  private final PasswordEncoder placeholderDelegatingPasswordEncoder = new DelegatingPasswordEncoder(
      PlaceholderPasswordEncoder.ENCODING_ID, Collections
      .singletonMap(PlaceholderPasswordEncoder.ENCODING_ID, new PlaceholderPasswordEncoder()));

  private final JdbcUserDetailsManager userDetailsManager;

  private final UserRepository userRepository;

  public OidcLocalUserServiceImpl(
      JdbcUserDetailsManager userDetailsManager,
      UserRepository userRepository) {
    this.userDetailsManager = userDetailsManager;
    this.userRepository = userRepository;
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void createLocalUser(UserInfo newUserInfo) {
    UserDetails user = new User(newUserInfo.getUserId(),
        this.placeholderDelegatingPasswordEncoder.encode(""), authorities);
    userDetailsManager.createUser(user);
    this.updateUserInfoInternal(newUserInfo);
  }

  private void updateUserInfoInternal(UserInfo newUserInfo) {
    UserPO managedUser = userRepository.findByUsername(newUserInfo.getUserId());
    if (!StringUtils.isBlank(newUserInfo.getEmail())) {
      managedUser.setEmail(newUserInfo.getEmail());
    }
    if (!StringUtils.isBlank(newUserInfo.getName())) {
      managedUser.setUserDisplayName(newUserInfo.getName());
    }
    userRepository.save(managedUser);
  }

  @Transactional(rollbackFor = Exception.class)
  @Override
  public void updateUserInfo(UserInfo newUserInfo) {
    this.updateUserInfoInternal(newUserInfo);
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
      byUsername = userRepository
          .findByUsernameLikeAndEnabled("%" + keyword + "%", 1);
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
    return users.stream().map(UserPO::toUserInfo)
        .collect(Collectors.toList());
  }
}
