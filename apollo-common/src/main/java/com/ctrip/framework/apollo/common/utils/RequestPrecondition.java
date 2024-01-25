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


import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.utils.StringUtils;


public class RequestPrecondition {

  private static String CONTAIN_EMPTY_ARGUMENT = "request payload should not be contain empty.";

  private static String ILLEGAL_MODEL = "request model is invalid";

  public static void checkArgumentsNotEmpty(String... args) {
    checkArguments(!StringUtils.isContainEmpty(args), CONTAIN_EMPTY_ARGUMENT);
  }

  public static void checkModel(boolean valid){
    checkArguments(valid, ILLEGAL_MODEL);
  }

  public static void checkArguments(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new BadRequestException(String.valueOf(errorMessage));
    }
  }
}
