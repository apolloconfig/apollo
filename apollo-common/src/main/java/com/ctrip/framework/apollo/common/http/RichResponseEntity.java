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
package com.ctrip.framework.apollo.common.http;

import org.springframework.http.HttpStatus;

public class RichResponseEntity<T>{

  private int code;
  private Object message;
  private T body;

  public static <T> RichResponseEntity<T> ok(T body){
    RichResponseEntity<T> richResponseEntity = new RichResponseEntity<>();
    richResponseEntity.message = HttpStatus.OK.getReasonPhrase();
    richResponseEntity.code = HttpStatus.OK.value();
    richResponseEntity.body = body;
    return richResponseEntity;
  }

  public static <T> RichResponseEntity<T> error(HttpStatus httpCode, Object message){
    RichResponseEntity<T> richResponseEntity = new RichResponseEntity<>();
    richResponseEntity.message = message;
    richResponseEntity.code = httpCode.value();
    return richResponseEntity;
  }

  public int getCode() {
    return code;
  }

  public Object getMessage() {
    return message;
  }

  public T getBody() {
    return body;
  }
}
