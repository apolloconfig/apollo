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

import com.ctrip.framework.test.tools.AloneRunner;
import com.ctrip.framework.test.tools.AloneWith;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author kl (http://kailing.pub)
 * @since 2021/5/21
 */
@RunWith(AloneRunner.class)
@AloneWith(JUnit4.class)
public class DeferredLoggerStateTest {

  @Test
  public void testDeferredState() {
    Assert.assertFalse(DeferredLogger.isEnabled());

    DeferredLogger.enable();
    Assert.assertTrue(DeferredLogger.isEnabled());

    DeferredLogger.replayTo();
    Assert.assertFalse(DeferredLogger.isEnabled());

    DeferredLogger.enable();
    Assert.assertFalse(DeferredLogger.isEnabled());
  }
}
