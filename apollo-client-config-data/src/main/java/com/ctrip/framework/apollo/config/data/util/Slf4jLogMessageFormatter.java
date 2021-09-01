/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.config.data.util;

import org.slf4j.helpers.MessageFormatter;
import org.springframework.core.log.LogMessage;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class Slf4jLogMessageFormatter {

  /**
   * format log message
   *
   * @param pattern slf4j log message patten
   * @param args    log message args
   * @return string
   */
  public static LogMessage format(String pattern, Object... args) {
    return LogMessage.of(() -> MessageFormatter.arrayFormat(pattern, args, null).getMessage());
  }
}
