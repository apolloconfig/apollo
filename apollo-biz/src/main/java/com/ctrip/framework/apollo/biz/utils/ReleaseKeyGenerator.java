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
package com.ctrip.framework.apollo.biz.utils;


import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.common.utils.UniqueKeyGenerator;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ReleaseKeyGenerator extends UniqueKeyGenerator {


  /**
   * Generate the release key in the format: timestamp+appId+cluster+namespace+hash(ipAsInt+counter)
   *
   * @param namespace the namespace of the release
   * @return the unique release key
   */
  public static String generateReleaseKey(Namespace namespace) {
    return generate(namespace.getAppId(), namespace.getClusterName(), namespace.getNamespaceName());
  }
}
