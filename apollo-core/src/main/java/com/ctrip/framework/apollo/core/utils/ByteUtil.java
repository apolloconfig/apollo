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

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ByteUtil {
  private static final char[] HEX_CHARS = new char[] {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

  public static byte int3(final int x) {
    return (byte) (x >> 24);
  }

  public static byte int2(final int x) {
    return (byte) (x >> 16);
  }

  public static byte int1(final int x) {
    return (byte) (x >> 8);
  }

  public static byte int0(final int x) {
    return (byte) (x);
  }

  public static String toHexString(byte[] bytes) {
    char[] chars = new char[bytes.length * 2];
    int i = 0;
    for (byte b : bytes) {
      chars[i++] = HEX_CHARS[b >> 4 & 0xF];
      chars[i++] = HEX_CHARS[b & 0xF];
    }
    return new String(chars);
  }
}
