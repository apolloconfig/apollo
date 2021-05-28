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
package com.ctrip.framework.apollo.tracer.spi;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface Transaction {
  String SUCCESS = "0";

  /**
   * Set the message status.
   *
   * @param status message status. "0" means success, otherwise error code.
   */
  void setStatus(String status);

  /**
   * Set the message status with exception class name.
   *
   * @param e exception.
   */
  void setStatus(Throwable e);

  /**
   * add one key-value pair to the message.
   */
  void addData(String key, Object value);

  /**
   * Complete the message construction.
   */
  void complete();
}
