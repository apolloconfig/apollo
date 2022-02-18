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

import com.ctrip.framework.apollo.core.utils.ByteUtil;
import org.junit.Assert;
import org.junit.Test;

public class ByteUtilTest {

  @Test
  public void testInt3() {
    Assert.assertEquals((byte)0, ByteUtil.int3(0));
    Assert.assertEquals((byte)0, ByteUtil.int3(1));
  }

  @Test
  public void testInt2() {
    Assert.assertEquals((byte)0, ByteUtil.int2(0));
    Assert.assertEquals((byte)0, ByteUtil.int2(1));
  }

  @Test
  public void testInt1() {
    Assert.assertEquals((byte)0, ByteUtil.int1(0));
    Assert.assertEquals((byte)0, ByteUtil.int1(1));
  }

  @Test
  public void testInt0() {
    Assert.assertEquals((byte)0, ByteUtil.int0(0));
    Assert.assertEquals((byte)1, ByteUtil.int0(1));
  }

  @Test
  public void testToHexString() {
    Assert.assertEquals("", ByteUtil.toHexString(new byte[] {}));
    Assert.assertEquals("98", ByteUtil.toHexString(new byte[] {(byte)-104}));
  }
}
