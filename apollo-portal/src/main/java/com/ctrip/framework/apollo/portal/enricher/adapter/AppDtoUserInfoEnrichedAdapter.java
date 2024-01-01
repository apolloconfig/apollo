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
package com.ctrip.framework.apollo.portal.enricher.adapter;

import com.ctrip.framework.apollo.common.dto.AppDTO;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class AppDtoUserInfoEnrichedAdapter implements UserInfoEnrichedAdapter {

  private final AppDTO dto;

  public AppDtoUserInfoEnrichedAdapter(AppDTO dto) {
    this.dto = dto;
  }

  @Override
  public final String getFirstUserId() {
    return this.dto.getDataChangeCreatedBy();
  }

  @Override
  public final void setFirstUserDisplayName(String userDisplayName) {
    this.dto.setDataChangeCreatedByDisplayName(userDisplayName);
  }

  @Override
  public final String getSecondUserId() {
    return this.dto.getDataChangeLastModifiedBy();
  }

  @Override
  public final void setSecondUserDisplayName(String userDisplayName) {
    this.dto.setDataChangeLastModifiedByDisplayName(userDisplayName);
  }

  @Override
  public final String getThirdUserId() {
    return this.dto.getOwnerName();
  }

  @Override
  public final void setThirdUserDisplayName(String userDisplayName) {
    this.dto.setOwnerDisplayName(userDisplayName);
  }
}
