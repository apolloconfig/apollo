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

import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.Marker;

/**
 * Only the following methods implement the delayed log function
 * <pre class="code">
 *  public void debug(String s);
 *  public void debug(String s, Object o);
 *  public void debug(String s, Object... objects);
 *  public void debug(String s, Object o, Object o1);
 *  public void debug(String s, Throwable throwable);
 *  public void info(String s);
 *  public void info(String s, Object o);
 *  public void info(String s, Object... objects);
 *  public void info(String s, Object o, Object o1);
 *  public void info(String s, Throwable throwable);
 *  public void warn(String s);
 *  public void warn(String s, Object o);
 *  public void warn(String s, Object... objects);
 *  public void warn(String s, Object o, Object o1);
 *  public void warn(String s, Throwable throwable)
 *  public void error(String s);
 *  public void error(String s, Object o);
 *  public void error(String s, Object o, Object o1);
 *  public void error(String s, Object... objects);
 *  public void error(String s, Throwable throwable);
 * </pre>
 *
 * @author kl (http://kailing.pub)
 * @since 2021/5/19
 */
public class DeferredLogger implements Logger {

  private final Logger logger;
  /**
   * Delay the opening state of the log -1: Uninitialized 0: Disable 1: Enabled
   */
  private static final AtomicInteger state = new AtomicInteger(-1);

  public static void enable() {
    state.compareAndSet(-1, 1);
  }

  public static void disable() {
    state.set(0);
  }

  public static boolean isEnabled() {
    return state.get() == 1;
  }

  public static void replayTo() {
    disable();
    DeferredLogCache.replayTo();
  }

  public DeferredLogger(Logger logger) {
    this.logger = logger;
  }

  @Override
  public String getName() {
    return logger.getName();
  }

  @Override
  public boolean isTraceEnabled() {
    return logger.isTraceEnabled();
  }

  @Override
  public void trace(String s) {
    logger.trace(s);
  }

  @Override
  public void trace(String s, Object o) {
    logger.trace(s, o);
  }

  @Override
  public void trace(String s, Object o, Object o1) {
    logger.trace(s, o, o1);
  }

  @Override
  public void trace(String s, Object... objects) {
    logger.trace(s, objects);
  }

  @Override
  public void trace(String s, Throwable throwable) {
    logger.trace(s, throwable);
  }

  @Override
  public boolean isTraceEnabled(Marker marker) {
    return logger.isTraceEnabled(marker);
  }

  @Override
  public void trace(Marker marker, String s) {
    logger.trace(marker, s);
  }

  @Override
  public void trace(Marker marker, String s, Object o) {
    logger.trace(marker, s, o);
  }

  @Override
  public void trace(Marker marker, String s, Object o, Object o1) {
    logger.trace(marker, s, o, o1);
  }

  @Override
  public void trace(Marker marker, String s, Object... objects) {
    logger.trace(marker, s, objects);
  }

  @Override
  public void trace(Marker marker, String s, Throwable throwable) {
    logger.trace(marker, s, throwable);
  }

  @Override
  public boolean isDebugEnabled() {
    return logger.isDebugEnabled();
  }

  @Override
  public void debug(String s) {
    if (isEnabled()) {
      DeferredLogCache.debug(logger, s);
    } else {
      logger.debug(s);
    }
  }

  @Override
  public void debug(String s, Object o) {
    if (isEnabled()) {
      DeferredLogCache.debug(logger, s, o);
    } else {
      logger.debug(s, o);
    }
  }

  @Override
  public void debug(String s, Object o, Object o1) {
    if (isEnabled()) {
      DeferredLogCache.debug(logger, s, o, o1);
    } else {
      logger.debug(s, o, o1);
    }
  }

  @Override
  public void debug(String s, Object... objects) {
    if (isEnabled()) {
      DeferredLogCache.debug(logger, s, objects);
    } else {
      logger.debug(s, objects);
    }
  }

  @Override
  public void debug(String s, Throwable throwable) {
    if (isEnabled()) {
      DeferredLogCache.debug(logger, s, throwable);
    } else {
      logger.debug(s, throwable);
    }
  }

  @Override
  public boolean isDebugEnabled(Marker marker) {
    return logger.isDebugEnabled(marker);
  }

  @Override
  public void debug(Marker marker, String s) {
    logger.debug(marker, s);
  }

  @Override
  public void debug(Marker marker, String s, Object o) {
    logger.debug(marker, s, o);
  }

  @Override
  public void debug(Marker marker, String s, Object o, Object o1) {
    logger.debug(marker, s, o, o1);
  }

  @Override
  public void debug(Marker marker, String s, Object... objects) {
    logger.debug(marker, s, objects);
  }

  @Override
  public void debug(Marker marker, String s, Throwable throwable) {
    logger.debug(marker, s, throwable);
  }

  @Override
  public boolean isInfoEnabled() {
    return logger.isInfoEnabled();
  }

  @Override
  public void info(String s) {
    if (isEnabled()) {
      DeferredLogCache.info(logger, s);
    } else {
      logger.info(s);
    }
  }

  @Override
  public void info(String s, Object o) {
    if (isEnabled()) {
      DeferredLogCache.info(logger, s, o);
    } else {
      logger.info(s, o);
    }
  }

  @Override
  public void info(String s, Object o, Object o1) {
    if (isEnabled()) {
      DeferredLogCache.info(logger, s, o, o1);
    } else {
      logger.info(s, o, o1);
    }
  }

  @Override
  public void info(String s, Object... objects) {
    if (isEnabled()) {
      DeferredLogCache.info(logger, s, objects);
    } else {
      logger.info(s, objects);
    }
  }

  @Override
  public void info(String s, Throwable throwable) {
    if (isEnabled()) {
      DeferredLogCache.info(logger, s, throwable);
    } else {
      logger.info(s, throwable);
    }
  }

  @Override
  public boolean isInfoEnabled(Marker marker) {
    return logger.isInfoEnabled(marker);
  }

  @Override
  public void info(Marker marker, String s) {
    logger.info(marker, s);
  }

  @Override
  public void info(Marker marker, String s, Object o) {
    logger.info(marker, s, o);
  }

  @Override
  public void info(Marker marker, String s, Object o, Object o1) {
    logger.info(marker, s, o, o1);
  }

  @Override
  public void info(Marker marker, String s, Object... objects) {
    logger.info(marker, s, objects);
  }

  @Override
  public void info(Marker marker, String s, Throwable throwable) {
    logger.info(marker, s, throwable);
  }

  @Override
  public boolean isWarnEnabled() {
    return logger.isWarnEnabled();
  }

  @Override
  public void warn(String s) {
    if (isEnabled()) {
      DeferredLogCache.warn(logger, s);
    } else {
      logger.warn(s);
    }
  }

  @Override
  public void warn(String s, Object o) {
    if (isEnabled()) {
      DeferredLogCache.warn(logger, s, o);
    } else {
      logger.warn(s, o);
    }
  }

  @Override
  public void warn(String s, Object... objects) {
    if (isEnabled()) {
      DeferredLogCache.warn(logger, s, objects);
    } else {
      logger.warn(s, objects);
    }
  }

  @Override
  public void warn(String s, Object o, Object o1) {
    if (isEnabled()) {
      DeferredLogCache.warn(logger, s, o, o1);
    } else {
      logger.warn(s, o, o1);
    }
  }

  @Override
  public void warn(String s, Throwable throwable) {
    if (isEnabled()) {
      DeferredLogCache.warn(logger, s, throwable);
    } else {
      logger.warn(s, throwable);
    }
  }

  @Override
  public boolean isWarnEnabled(Marker marker) {
    return logger.isWarnEnabled(marker);
  }

  @Override
  public void warn(Marker marker, String s) {
    logger.warn(marker, s);
  }

  @Override
  public void warn(Marker marker, String s, Object o) {
    logger.warn(marker, s, o);
  }

  @Override
  public void warn(Marker marker, String s, Object o, Object o1) {
    logger.warn(marker, s, o, o1);
  }

  @Override
  public void warn(Marker marker, String s, Object... objects) {
    logger.warn(marker, s, objects);
  }

  @Override
  public void warn(Marker marker, String s, Throwable throwable) {
    logger.warn(marker, s, throwable);
  }

  @Override
  public boolean isErrorEnabled() {
    return logger.isErrorEnabled();
  }

  @Override
  public void error(String s) {
    if (isEnabled()) {
      DeferredLogCache.error(logger, s);
    } else {
      logger.error(s);
    }
  }

  @Override
  public void error(String s, Object o) {
    if (isEnabled()) {
      DeferredLogCache.error(logger, s, o);
    } else {
      logger.error(s, o);
    }
  }

  @Override
  public void error(String s, Object o, Object o1) {
    if (isEnabled()) {
      DeferredLogCache.error(logger, s, o, o1);
    } else {
      logger.error(s, o, o1);
    }
  }

  @Override
  public void error(String s, Object... objects) {
    if (isEnabled()) {
      DeferredLogCache.error(logger, s, objects);
    } else {
      logger.error(s, objects);
    }
  }

  @Override
  public void error(String s, Throwable throwable) {
    if (isEnabled()) {
      DeferredLogCache.error(logger, s, throwable);
    } else {
      logger.error(s, throwable);
    }
  }

  @Override
  public boolean isErrorEnabled(Marker marker) {
    return logger.isErrorEnabled(marker);
  }

  @Override
  public void error(Marker marker, String s) {
    logger.error(marker, s);
  }

  @Override
  public void error(Marker marker, String s, Object o) {
    logger.error(marker, s, o);
  }

  @Override
  public void error(Marker marker, String s, Object o, Object o1) {
    logger.error(marker, s, o, o1);
  }

  @Override
  public void error(Marker marker, String s, Object... objects) {
    logger.error(marker, s, objects);
  }

  @Override
  public void error(Marker marker, String s, Throwable throwable) {
    logger.error(marker, s, throwable);
  }
}
