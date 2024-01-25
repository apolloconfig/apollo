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

import com.google.common.base.MoreObjects;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.springframework.web.client.HttpStatusCodeException;

import java.lang.reflect.Type;
import java.util.Map;

public final class ExceptionUtils {

  private static Gson gson = new Gson();

  private static Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

  public static String toString(HttpStatusCodeException e) {
    Map<String, Object> errorAttributes = gson.fromJson(e.getResponseBodyAsString(), mapType);
    if (errorAttributes != null) {
      return MoreObjects.toStringHelper(HttpStatusCodeException.class).omitNullValues()
          .add("status", errorAttributes.get("status"))
          .add("message", errorAttributes.get("message"))
          .add("timestamp", errorAttributes.get("timestamp"))
          .add("exception", errorAttributes.get("exception"))
          .add("errorCode", errorAttributes.get("errorCode"))
          .add("stackTrace", errorAttributes.get("stackTrace")).toString();
    }
    return "";
  }
}
