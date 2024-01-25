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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.enricher.AdditionalUserInfoEnricher;
import com.ctrip.framework.apollo.portal.enricher.adapter.UserInfoEnrichedAdapter;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
@Service
public class AdditionalUserInfoEnrichServiceImpl implements AdditionalUserInfoEnrichService {

  private final UserService userService;

  private final List<AdditionalUserInfoEnricher> enricherList;

  public AdditionalUserInfoEnrichServiceImpl(
      UserService userService,
      List<AdditionalUserInfoEnricher> enricherList) {
    this.userService = userService;
    this.enricherList = enricherList;
  }

  @Override
  public <T> void enrichAdditionalUserInfo(List<? extends T> list,
      Function<? super T, ? extends UserInfoEnrichedAdapter> mapper) {
    if (CollectionUtils.isEmpty(list)) {
      return;
    }
    if (CollectionUtils.isEmpty(this.enricherList)) {
      return;
    }
    List<UserInfoEnrichedAdapter> adapterList = this.adapt(list, mapper);
    if (CollectionUtils.isEmpty(adapterList)) {
      return;
    }
    Set<String> userIdSet = this.extractOperatorId(adapterList);
    if (CollectionUtils.isEmpty(userIdSet)) {
      return;
    }
    List<UserInfo> userInfoList = this.userService.findByUserIds(new ArrayList<>(userIdSet));
    if (CollectionUtils.isEmpty(userInfoList)) {
      return;
    }
    Map<String, UserInfo> userInfoMap = userInfoList.stream()
        .collect(Collectors.toMap(UserInfo::getUserId, Function.identity()));
    for (UserInfoEnrichedAdapter adapter : adapterList) {
      for (AdditionalUserInfoEnricher enricher : this.enricherList) {
        enricher.enrichAdditionalUserInfo(adapter, userInfoMap);
      }
    }
  }

  private <T> List<UserInfoEnrichedAdapter> adapt(List<? extends T> dtoList,
      Function<? super T, ? extends UserInfoEnrichedAdapter> mapper) {
    List<UserInfoEnrichedAdapter> adapterList = new ArrayList<>(dtoList.size());
    for (T dto : dtoList) {
      if (dto == null) {
        continue;
      }
      UserInfoEnrichedAdapter enrichedAdapter = mapper.apply(dto);
      adapterList.add(enrichedAdapter);
    }
    return adapterList;
  }

  private <T> Set<String> extractOperatorId(List<UserInfoEnrichedAdapter> adapterList) {
    Set<String> operatorIdSet = new HashSet<>();
    for (UserInfoEnrichedAdapter adapter : adapterList) {
      if (StringUtils.hasText(adapter.getFirstUserId())) {
        operatorIdSet.add(adapter.getFirstUserId());
      }
      if (StringUtils.hasText(adapter.getSecondUserId())) {
        operatorIdSet.add(adapter.getSecondUserId());
      }
      if (StringUtils.hasText(adapter.getThirdUserId())) {
        operatorIdSet.add(adapter.getThirdUserId());
      }
    }
    return operatorIdSet;
  }
}
