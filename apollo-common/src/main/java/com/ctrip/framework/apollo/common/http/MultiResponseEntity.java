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

import java.util.LinkedList;
import java.util.List;

/**
 * 一个Response中包含多个ResponseEntity
 */
public class MultiResponseEntity<T> {

  private int code;

  private List<RichResponseEntity<T>> entities = new LinkedList<>();

  private MultiResponseEntity(HttpStatus httpCode) {
    this.code = httpCode.value();
  }

  public static <T> MultiResponseEntity<T> instance(HttpStatus statusCode) {
    return new MultiResponseEntity<>(statusCode);
  }

  public static <T> MultiResponseEntity<T> ok() {
    return new MultiResponseEntity<>(HttpStatus.OK);
  }

  public void addResponseEntity(RichResponseEntity<T> responseEntity) {
    if (responseEntity == null){
      throw new IllegalArgumentException("sub response entity can not be null");
    }
    entities.add(responseEntity);

  }

}
