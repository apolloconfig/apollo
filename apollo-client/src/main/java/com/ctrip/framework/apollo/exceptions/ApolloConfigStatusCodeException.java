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
package com.ctrip.framework.apollo.exceptions;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ApolloConfigStatusCodeException extends RuntimeException{
  private final int m_statusCode;

  public ApolloConfigStatusCodeException(int statusCode, String message) {
    super(String.format("[status code: %d] %s", statusCode, message));
    this.m_statusCode = statusCode;
  }

  public ApolloConfigStatusCodeException(int statusCode, Throwable cause) {
    super(cause);
    this.m_statusCode = statusCode;
  }

  public int getStatusCode() {
    return m_statusCode;
  }
}
