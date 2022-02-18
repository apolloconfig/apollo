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

import static org.junit.Assert.*;

import org.junit.Test;

public class ClassLoaderUtilTest {
  private static boolean shouldFailInInitialization = false;
  @Test
  public void testGetClassLoader() {
    assertNotNull(ClassLoaderUtil.getLoader());
  }

  @Test
  public void testIsClassPresent() {
    assertTrue(ClassLoaderUtil.isClassPresent("java.lang.String"));
  }

  @Test
  public void testIsClassPresentWithClassNotFound() {
    assertFalse(ClassLoaderUtil.isClassPresent("java.lang.StringNotFound"));
  }

  @Test
  public void testIsClassPresentWithLinkageError() {
    shouldFailInInitialization = true;
    assertFalse(ClassLoaderUtil.isClassPresent(ClassWithInitializationError.class.getName()));
  }

  public static class ClassWithInitializationError {
    static {
      if (ClassLoaderUtilTest.shouldFailInInitialization) {
        throw new RuntimeException("Some initialization exception");
      }
    }
  }
}