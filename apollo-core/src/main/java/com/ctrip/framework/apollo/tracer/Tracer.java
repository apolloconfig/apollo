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
package com.ctrip.framework.apollo.tracer;

import com.ctrip.framework.apollo.tracer.internals.NullMessageProducerManager;
import com.ctrip.framework.apollo.tracer.spi.MessageProducer;
import com.ctrip.framework.apollo.tracer.spi.MessageProducerManager;
import com.ctrip.framework.apollo.tracer.spi.Transaction;

import com.ctrip.framework.foundation.internals.ServiceBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public abstract class Tracer {
  private static final Logger logger = LoggerFactory.getLogger(Tracer.class);
  private static final MessageProducerManager NULL_MESSAGE_PRODUCER_MANAGER =
      new NullMessageProducerManager();
  private static volatile MessageProducerManager producerManager;
  private static Object lock = new Object();

  static {
    getProducer();
  }

  private static MessageProducer getProducer() {
    try {
      if (producerManager == null) {
        synchronized (lock) {
          if (producerManager == null) {
            producerManager = ServiceBootstrap.loadFirst(MessageProducerManager.class);
          }
        }
      }
    } catch (Throwable ex) {
      logger.error(
          "Failed to initialize message producer manager, use null message producer manager.", ex);
      producerManager = NULL_MESSAGE_PRODUCER_MANAGER;
    }
    return producerManager.getProducer();
  }

  public static void logError(String message, Throwable cause) {
    try {
      getProducer().logError(message, cause);
    } catch (Throwable ex) {
      logger.warn("Failed to log error for message: {}, cause: {}", message, cause, ex);
    }
  }

  public static void logError(Throwable cause) {
    try {
      getProducer().logError(cause);
    } catch (Throwable ex) {
      logger.warn("Failed to log error for cause: {}", cause, ex);
    }
  }

  public static void logEvent(String type, String name) {
    try {
      getProducer().logEvent(type, name);
    } catch (Throwable ex) {
      logger.warn("Failed to log event for type: {}, name: {}", type, name, ex);
    }
  }

  public static void logEvent(String type, String name, String status, String nameValuePairs) {
    try {
      getProducer().logEvent(type, name, status, nameValuePairs);
    } catch (Throwable ex) {
      logger.warn("Failed to log event for type: {}, name: {}, status: {}, nameValuePairs: {}",
          type, name, status, nameValuePairs, ex);
    }
  }

  public static Transaction newTransaction(String type, String name) {
    try {
      return getProducer().newTransaction(type, name);
    } catch (Throwable ex) {
      logger.warn("Failed to create transaction for type: {}, name: {}", type, name, ex);
      return NULL_MESSAGE_PRODUCER_MANAGER.getProducer().newTransaction(type, name);
    }
  }
}
