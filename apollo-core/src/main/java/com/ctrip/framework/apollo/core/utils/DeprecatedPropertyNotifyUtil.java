/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.core.utils;

import org.slf4j.Logger;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class DeprecatedPropertyNotifyUtil {

  private static final Logger log =
      DeferredLoggerFactory.getLogger(DeprecatedPropertyNotifyUtil.class);

  public static void warn(String deprecatedProperty, String insteadProperty) {
    log.warn(
        "[{}] is deprecated since 1.9.0 and will be removed in a future version, please use the [{}] instead.",
        deprecatedProperty,
        insteadProperty);
  }
}
