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
package com.ctrip.framework.apollo.openapi.client.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.ctrip.framework.apollo.openapi.utils.UrlUtils;
import org.junit.Test;

/**
 * @author huanghousheng
 * @since 9/7/22
 */
public class  UrlUtilsTest {

  @Test
  public void testSlash() {
    String someKey = "protocol//:host:port";
    assertTrue(UrlUtils.hasIllegalChar(someKey));
  }

  @Test
  public void testBackslash() {
    String someKey = "a\\c";
    assertTrue(UrlUtils.hasIllegalChar(someKey));
  }

  @Test
  public void testNormalKey() {
    String someKey = "someKey";
    assertFalse(UrlUtils.hasIllegalChar(someKey));
  }

  @Test
  public void testDot() {
    String someKey = "a.b";
    assertFalse(UrlUtils.hasIllegalChar(someKey));
  }

}
