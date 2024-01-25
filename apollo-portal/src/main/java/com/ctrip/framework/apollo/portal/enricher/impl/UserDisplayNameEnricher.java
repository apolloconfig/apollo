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
package com.ctrip.framework.apollo.portal.enricher.impl;

import com.ctrip.framework.apollo.portal.enricher.AdditionalUserInfoEnricher;
import com.ctrip.framework.apollo.portal.enricher.adapter.UserInfoEnrichedAdapter;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
@Component
public class UserDisplayNameEnricher implements AdditionalUserInfoEnricher {

  @Override
  public void enrichAdditionalUserInfo(UserInfoEnrichedAdapter adapter,
      Map<String, UserInfo> userInfoMap) {
    if (StringUtils.hasText(adapter.getFirstUserId())) {
      UserInfo userInfo = userInfoMap.get(adapter.getFirstUserId());
      if (userInfo != null && StringUtils.hasText(userInfo.getName())) {
        adapter.setFirstUserDisplayName(userInfo.getName());
      }
    }
    if (StringUtils.hasText(adapter.getSecondUserId())) {
      UserInfo userInfo = userInfoMap.get(adapter.getSecondUserId());
      if (userInfo != null && StringUtils.hasText(userInfo.getName())) {
        adapter.setSecondUserDisplayName(userInfo.getName());
      }
    }
    if (StringUtils.hasText(adapter.getThirdUserId())) {
      UserInfo userInfo = userInfoMap.get(adapter.getThirdUserId());
      if (userInfo != null && StringUtils.hasText(userInfo.getName())) {
        adapter.setThirdUserDisplayName(userInfo.getName());
      }
    }
  }
}
