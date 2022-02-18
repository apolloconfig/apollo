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
package com.ctrip.framework.apollo.core.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Delayed log printing utility class, used only for logging when Apollo is initialized
 *
 * @author kl (http://kailing.pub)
 * @since 2021/5/11
 */
final class DeferredLogCache {

  public static final int MAX_LOG_SIZE = 1000;
  private static final AtomicInteger LOG_INDEX = new AtomicInteger(0);
  private static final Cache<Integer, Line> LOG_CACHE = CacheBuilder.newBuilder()
      .maximumSize(MAX_LOG_SIZE)
      .build();

  private DeferredLogCache() {
  }


  public static void debug(Logger logger, String message, Throwable throwable) {
    add(logger, Level.DEBUG, message, null, throwable);
  }

  public static void debug(Logger logger, String message, Object... objects) {
    add(logger, Level.DEBUG, message, objects, null);
  }

  public static void info(Logger logger, String message, Throwable throwable) {
    add(logger, Level.INFO, message, null, throwable);
  }

  public static void info(Logger logger, String message, Object... objects) {
    add(logger, Level.INFO, message, objects, null);
  }

  public static void warn(Logger logger, String message, Object... objects) {
    add(logger, Level.WARN, message, objects, null);
  }

  public static void warn(Logger logger, String message, Throwable throwable) {
    add(logger, Level.WARN, message, null, throwable);
  }


  public static void error(Logger logger, String message, Throwable throwable) {
    add(logger, Level.ERROR, message, null, throwable);
  }

  public static void error(Logger logger, String message, Object... objects) {
    add(logger, Level.ERROR, message, objects, null);
  }

  private static void add(Logger logger, Level level, String message, Object[] objects,
      Throwable throwable) {
    Line logLine = new Line(level, message, objects, throwable, logger);
    LOG_CACHE.put(LOG_INDEX.incrementAndGet(), logLine);
  }

  static void replayTo() {
    for (int i = 1; i <= LOG_INDEX.get(); i++) {
      Line logLine = LOG_CACHE.getIfPresent(i);
      if (logLine == null) {
        continue;
      }
      Logger logger = logLine.getLogger();
      Level level = logLine.getLevel();
      String message = logLine.getMessage();
      Object[] objects = logLine.getObjects();
      Throwable throwable = logLine.getThrowable();
      logTo(logger, level, message, objects, throwable);
    }
    clear();
  }

  static void clear() {
    LOG_CACHE.invalidateAll();
    LOG_INDEX.set(0);
  }

  static long logSize() {
    return LOG_CACHE.size();
  }

  static void logTo(Logger logger, Level level, String message, Object[] objects,
      Throwable throwable) {
    switch (level) {
      case DEBUG:
        if (throwable != null) {
          logger.debug(message, throwable);
        } else {
          logger.debug(message, objects);
        }
        return;
      case INFO:
        if (throwable != null) {
          logger.info(message, throwable);
        } else {
          logger.info(message, objects);
        }
        return;
      case WARN:
        if (throwable != null) {
          logger.warn(message, throwable);
        } else {
          logger.warn(message, objects);
        }
        return;
      case ERROR:
        if (throwable != null) {
          logger.error(message, throwable);
        } else {
          logger.error(message, objects);
        }
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + level);
    }
  }

  private static class Line {

    private final Level level;

    private final String message;

    private final Object[] objects;

    private final Throwable throwable;

    private final Logger logger;

    Line(Level level, String message, Object[] objects, Throwable throwable, Logger logger) {
      this.level = level;
      this.message = message;
      this.objects = objects;
      this.throwable = throwable;
      this.logger = logger;
    }

    public Object[] getObjects() {
      return objects;
    }

    public Logger getLogger() {
      return logger;
    }

    Level getLevel() {
      return this.level;
    }

    String getMessage() {
      return this.message;
    }

    Throwable getThrowable() {
      return this.throwable;
    }

  }
}
