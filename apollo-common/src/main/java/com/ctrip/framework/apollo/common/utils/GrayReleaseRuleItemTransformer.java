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
package com.ctrip.framework.apollo.common.utils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;

import java.lang.reflect.Type;
import java.util.Set;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class GrayReleaseRuleItemTransformer {
  private static final Gson gson = new Gson();
  private static final Type grayReleaseRuleItemsType = new TypeToken<Set<GrayReleaseRuleItemDTO>>() {
  }.getType();

  public static Set<GrayReleaseRuleItemDTO> batchTransformFromJSON(String content) {
    return gson.fromJson(content, grayReleaseRuleItemsType);
  }

  public static String batchTransformToJSON(Set<GrayReleaseRuleItemDTO> ruleItems) {
    return gson.toJson(ruleItems);
  }
}
