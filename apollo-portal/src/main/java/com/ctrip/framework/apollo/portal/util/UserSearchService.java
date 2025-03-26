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
package com.ctrip.framework.apollo.portal.util;

import com.ctrip.framework.apollo.portal.entity.po.UserPO;
import com.ctrip.framework.apollo.portal.repository.UserRepository;
import org.springframework.util.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

    public class UserSearchService {

    private final UserRepository userRepository;

    public UserSearchService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserPO> findUsers(String keyword, boolean includeInactiveUsers) {
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
            byUserDisplayName = userRepository.findByUserDisplayNameLikeAndEnabled("%" + keyword + "%", 1);
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
}
