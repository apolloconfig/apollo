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
