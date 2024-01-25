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

import com.ctrip.framework.apollo.core.utils.StringUtils;

import java.util.regex.Pattern;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class InputValidator {
  public static final String INVALID_CLUSTER_NAMESPACE_MESSAGE = "Only digits, alphabets and symbol - _ . (except single .) are allowed";
  public static final String INVALID_NAMESPACE_NAMESPACE_MESSAGE = "not allowed to end with .json, .yml, .yaml, .xml, .properties";
  public static final String CLUSTER_NAMESPACE_VALIDATOR = "[0-9a-zA-Z_-]+[0-9a-zA-Z_.-]*";
  private static final String APP_NAMESPACE_VALIDATOR = "[a-zA-Z0-9_-]+[a-zA-Z0-9._-]*(?<!\\.(json|yml|yaml|xml|properties))$";
  private static final Pattern CLUSTER_NAMESPACE_PATTERN = Pattern.compile(CLUSTER_NAMESPACE_VALIDATOR);
  private static final Pattern APP_NAMESPACE_PATTERN = Pattern.compile(APP_NAMESPACE_VALIDATOR);

  public static boolean isValidClusterNamespace(String name) {
    if (StringUtils.isEmpty(name)){
      return false;
    }
    return CLUSTER_NAMESPACE_PATTERN.matcher(name).matches();
  }

  public static boolean isValidAppNamespace(String name){
    if (StringUtils.isEmpty(name)){
      return false;
    }
    return APP_NAMESPACE_PATTERN.matcher(name).matches();
  }
}
