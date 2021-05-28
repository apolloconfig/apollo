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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/20
 */
@RunWith(AloneRunner.class)
@AloneWith(JUnit4.class)
public class DeferredLogCacheTest {

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String logMsg = "hello kl";

    @Test
    public void testDeferredLogCacheMaxLogSize() {
        for (int i = 0; i < 20000; i++) {
            DeferredLogCache.info(logger, "DeferredLogUtilTest");
        }
        Assert.assertEquals(DeferredLogCache.logSize(), DeferredLogCache.MAX_LOG_SIZE);
    }

    @Test
    public void testDisableDeferred() {
        DeferredLogCache.clear();
        DeferredLogger.disable();
        final Logger defaultLogger = DeferredLoggerFactory.getLogger(DeferredLoggerTest.class);
        defaultLogger.info(logMsg);
        defaultLogger.debug(logMsg);
        defaultLogger.warn(logMsg);
        Assert.assertEquals(0, DeferredLogCache.logSize());

    }

    @Test
    public void testEnableDeferred() {
        final Logger defaultLogger = DeferredLoggerFactory.getLogger(DeferredLoggerTest.class);
        DeferredLogger.enable();

        defaultLogger.info(logMsg);
        defaultLogger.debug(logMsg);
        defaultLogger.warn(logMsg);
        Assert.assertEquals(3, DeferredLogCache.logSize());
    }
}
