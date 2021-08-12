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
package com.ctrip.framework.apollo.core.utils;

import com.ctrip.framework.test.tools.AloneRunner;
import com.ctrip.framework.test.tools.AloneWith;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/11
 */
@RunWith(AloneRunner.class)
@AloneWith(JUnit4.class)
public class DeferredLoggerTest {

  private static ByteArrayOutputStream outContent;
  private static Logger logger = null;
  private static PrintStream printStream;

  @BeforeClass
  public static void init() throws NoSuchFieldException, IllegalAccessException {
    DeferredLoggerTest.outContent = new ByteArrayOutputStream();
    DeferredLoggerTest.printStream = new PrintStream(DeferredLoggerTest.outContent);
    System.setOut(DeferredLoggerTest.printStream);
    DeferredLoggerTest.logger = DeferredLoggerFactory.getLogger("DeferredLoggerTest");
  }

  @Test
  public void testErrorLog() {
    DeferredLoggerTest.logger.error("errorLogger");
    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("errorLogger"));
  }

  @Test
  public void testInfoLog() {
    DeferredLoggerTest.logger.info("inFoLogger");
    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("inFoLogger"));
  }

  @Test
  public void testWarnLog() {
    DeferredLoggerTest.logger.warn("warnLogger");
    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("warnLogger"));
  }

  @Test
  public void testDebugLog() {
    DeferredLoggerTest.logger.warn("debugLogger");
    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("debugLogger"));
  }

  @Test
  public void testDeferredLog() {
    DeferredLogger.enable();

    DeferredLoggerTest.logger.error("errorLogger_testDeferredLog");
    DeferredLoggerTest.logger.info("inFoLogger_testDeferredLog");
    DeferredLoggerTest.logger.warn("warnLogger_testDeferredLog");
    DeferredLoggerTest.logger.debug("debugLogger_testDeferredLog");

    Assert.assertFalse(DeferredLoggerTest.outContent.toString().contains("errorLogger_testDeferredLog"));
    Assert.assertFalse(DeferredLoggerTest.outContent.toString().contains("inFoLogger_testDeferredLog"));
    Assert.assertFalse(DeferredLoggerTest.outContent.toString().contains("warnLogger_testDeferredLog"));
    Assert.assertFalse(DeferredLoggerTest.outContent.toString().contains("debugLogger_testDeferredLog"));

    DeferredLogCache.replayTo();

    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("errorLogger_testDeferredLog"));
    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("inFoLogger_testDeferredLog"));
    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("warnLogger_testDeferredLog"));
    Assert.assertTrue(DeferredLoggerTest.outContent.toString().contains("debugLogger_testDeferredLog"));

  }

}
