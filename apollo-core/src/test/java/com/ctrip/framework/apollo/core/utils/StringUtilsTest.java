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

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

public class StringUtilsTest {

  @Test
  public void testEqualsIgnoreCase() {
    Assert.assertFalse(StringUtils.equalsIgnoreCase(",", "foo"));
    Assert.assertFalse(StringUtils.equalsIgnoreCase(null, "??"));
    Assert.assertTrue(StringUtils.equalsIgnoreCase(null, null));
    Assert.assertTrue(StringUtils.equalsIgnoreCase("foo", "Foo"));
  }

  @Test
  public void testEquals() {
    Assert.assertFalse(StringUtils.equals(null, ""));
    Assert.assertTrue(StringUtils.equals(null, null));
    Assert.assertTrue(StringUtils.equals("3", "3"));
  }

  @Test
  public void testIsBlank() {
    Assert.assertFalse(StringUtils.isBlank("\'"));
    Assert.assertTrue(StringUtils.isBlank(""));
    Assert.assertTrue(StringUtils.isBlank(null));
  }

  @Test
  public void testIsContainEmpty() {
    Assert.assertFalse(StringUtils.isContainEmpty(null));
    Assert.assertFalse(StringUtils.isContainEmpty(new String[] {}));
    Assert.assertFalse(StringUtils.isContainEmpty(new String[] {"1"}));
    Assert.assertTrue(StringUtils.isContainEmpty(new String[] {null}));
  }

  @Test
  public void testIsEmpty() {
    Assert.assertFalse(StringUtils.isEmpty("1"));
    Assert.assertTrue(StringUtils.isEmpty(null));
    Assert.assertTrue(StringUtils.isEmpty(""));
  }

  @Test
  public void testIsNumeric() {
    Assert.assertFalse(StringUtils.isNumeric(null));
    Assert.assertFalse(StringUtils.isNumeric("\'"));
    Assert.assertTrue(StringUtils.isNumeric("1"));
  }

  @Test
  public void testStartsWithIgnoreCase() {
    Assert.assertFalse(StringUtils.startsWithIgnoreCase("A1B2C3", "BAZ"));
    Assert.assertFalse(StringUtils.startsWithIgnoreCase(",", "BAZ"));
    Assert.assertTrue(StringUtils.startsWithIgnoreCase("bar", "BAR"));
  }

  @Test
  public void testStartsWith() {
    Assert.assertFalse(StringUtils.startsWith("1234", "1a 2b 3c"));
    Assert.assertTrue(StringUtils.startsWith("1a 2b 3c", "1a 2b 3c"));
    Assert.assertTrue(StringUtils.startsWith(null, null));
  }

  @Test
  public void testTrim() {
    Assert.assertEquals("1234", StringUtils.trim("1234"));
    Assert.assertNull(StringUtils.trim(null));
  }

  @Test
  public void testTrimToEmpty() {
    Assert.assertEquals("1234", StringUtils.trimToEmpty("1234"));
    Assert.assertEquals("", StringUtils.trimToEmpty(null));
  }

  @Test
  public void trimToNull() {
    Assert.assertNull(StringUtils.trimToNull(null));
    Assert.assertEquals("foo", StringUtils.trimToNull("foo"));
  }
}
