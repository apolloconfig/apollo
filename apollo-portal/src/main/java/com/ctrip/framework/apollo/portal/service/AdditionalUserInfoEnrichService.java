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

import com.ctrip.framework.apollo.portal.enricher.adapter.UserInfoEnrichedAdapter;
import java.util.List;
import java.util.function.Function;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public interface AdditionalUserInfoEnrichService {

  /**
   * enrich the additional user info for the object list
   *
   * @param list   object with user id
   * @param mapper map the object in the list to {@link UserInfoEnrichedAdapter}
   */
  <T> void enrichAdditionalUserInfo(List<? extends T> list,
      Function<? super T, ? extends UserInfoEnrichedAdapter> mapper);
}
