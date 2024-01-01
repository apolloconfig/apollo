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

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public interface UserInfoEnrichedAdapter {

  /**
   * get user id from the object
   *
   * @return user id
   */
  String getFirstUserId();

  /**
   * set the user display name for the object
   *
   * @param userDisplayName user display name
   */
  void setFirstUserDisplayName(String userDisplayName);

  /**
   * get operator id from the object
   *
   * @return operator id
   */
  default String getSecondUserId() {
    return null;
  }

  /**
   * set the user display name for the object
   *
   * @param userDisplayName user display name
   */
  default void setSecondUserDisplayName(String userDisplayName) {
  }

  /**
   * get operator id from the object
   *
   * @return operator id
   */
  default String getThirdUserId() {
    return null;
  }

  /**
   * set the user display name for the object
   *
   * @param userDisplayName user display name
   */
  default void setThirdUserDisplayName(String userDisplayName) {
  }
}
