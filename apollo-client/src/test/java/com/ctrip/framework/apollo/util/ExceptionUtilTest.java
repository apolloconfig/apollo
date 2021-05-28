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
package com.ctrip.framework.apollo.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ExceptionUtilTest {

  @Test
  public void testGetDetailMessageWithNoCause() throws Exception {
    String someMessage = "some message";
    Throwable ex = new Throwable(someMessage);
    assertEquals(someMessage, ExceptionUtil.getDetailMessage(ex));
  }

  @Test
  public void testGetDetailMessageWithCauses() throws Exception {
    String causeMsg1 = "some cause";
    String causeMsg2 = "another cause";
    String someMessage = "some message";

    Throwable cause2 = new Throwable(causeMsg2);
    Throwable cause1 = new Throwable(causeMsg1, cause2);
    Throwable ex = new Throwable(someMessage, cause1);

    String expected = someMessage + " [Cause: " + causeMsg1 + " [Cause: " + causeMsg2 + "]]";
    assertEquals(expected, ExceptionUtil.getDetailMessage(ex));
  }

  @Test
  public void testGetDetailMessageWithCauseMessageNull() throws Exception {
    String someMessage = "some message";
    Throwable cause = new Throwable();
    Throwable ex = new Throwable(someMessage, cause);

    assertEquals(someMessage, ExceptionUtil.getDetailMessage(ex));
  }

  @Test
  public void testGetDetailMessageWithNullMessage() throws Exception {
    Throwable ex = new Throwable();

    assertEquals("", ExceptionUtil.getDetailMessage(ex));
    assertEquals("", ExceptionUtil.getDetailMessage(null));

  }
}
