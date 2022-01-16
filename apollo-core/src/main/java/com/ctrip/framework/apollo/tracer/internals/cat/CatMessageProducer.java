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
import java.lang.reflect.Method;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class CatMessageProducer implements MessageProducer {
  private static Class CAT_CLASS;
  private static Method LOG_ERROR_WITH_CAUSE;
  private static Method LOG_ERROR_WITH_MESSAGE_AND_CAUSE;
  private static Method LOG_EVENT_WITH_TYPE_AND_NAME;
  private static Method LOG_EVENT_WITH_TYPE_AND_NAME_AND_STATUS_AND_NAME_VALUE_PAIRS;
  private static Method NEW_TRANSACTION_WITH_TYPE_AND_NAME;

  static {
    try {
      CAT_CLASS = Class.forName(CatNames.CAT_CLASS);
      LOG_ERROR_WITH_CAUSE = CAT_CLASS.getMethod(CatNames.LOG_ERROR_METHOD, Throwable.class);
      LOG_ERROR_WITH_MESSAGE_AND_CAUSE =
          CAT_CLASS.getMethod(CatNames.LOG_ERROR_METHOD, String.class, Throwable.class);
      LOG_EVENT_WITH_TYPE_AND_NAME =
          CAT_CLASS.getMethod(CatNames.LOG_EVENT_METHOD, String.class, String.class);
      LOG_EVENT_WITH_TYPE_AND_NAME_AND_STATUS_AND_NAME_VALUE_PAIRS =
          CAT_CLASS.getMethod(
              CatNames.LOG_EVENT_METHOD, String.class, String.class, String.class, String.class);
      NEW_TRANSACTION_WITH_TYPE_AND_NAME =
          CAT_CLASS.getMethod(CatNames.NEW_TRANSACTION_METHOD, String.class, String.class);
      // eager init CatTransaction
      CatTransaction.init();
    } catch (Throwable ex) {
      throw new IllegalStateException("Initialize Cat message producer failed", ex);
    }
  }

  @Override
  public void logError(Throwable cause) {
    try {
      LOG_ERROR_WITH_CAUSE.invoke(null, cause);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void logError(String message, Throwable cause) {
    try {
      LOG_ERROR_WITH_MESSAGE_AND_CAUSE.invoke(null, message, cause);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void logEvent(String type, String name) {
    try {
      LOG_EVENT_WITH_TYPE_AND_NAME.invoke(null, type, name);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public void logEvent(String type, String name, String status, String nameValuePairs) {
    try {
      LOG_EVENT_WITH_TYPE_AND_NAME_AND_STATUS_AND_NAME_VALUE_PAIRS.invoke(
          null, type, name, status, nameValuePairs);
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }

  @Override
  public Transaction newTransaction(String type, String name) {
    try {
      return new CatTransaction(NEW_TRANSACTION_WITH_TYPE_AND_NAME.invoke(null, type, name));
    } catch (Throwable ex) {
      throw new IllegalStateException(ex);
    }
  }
}
