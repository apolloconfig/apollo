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
package com.ctrip.framework.apollo.tracer.internals.cat;

import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.dianping.cat.Cat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class CatMessageProducer implements MessageProducer {
  @Override
  public void logError(Throwable cause) {
    try {
      Cat.logError(cause);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void logError(String message, Throwable cause) {
    try {
      Cat.logError( message, cause);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void logEvent(String type, String name) {
    try {
      Cat.logEvent(type, name);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void logEvent(String type, String name, String status, String nameValuePairs) {
    try {
      Cat.logEvent(type, name,
          status, nameValuePairs);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public Transaction newTransaction(String type, String name) {
    try {
      return new CatTransaction(Cat.newTransaction(type, name));
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }
}
