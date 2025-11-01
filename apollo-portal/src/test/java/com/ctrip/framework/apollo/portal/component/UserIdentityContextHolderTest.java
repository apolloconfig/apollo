/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.portal.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserIdentityContextHolderTest {

  @BeforeEach
  public void setUp() {
    // Clear ThreadLocal before each test
    UserIdentityContextHolder.clear();
  }

  @Test
  public void setAuthType_NonNullAuthType_ShouldSetCorrectly() {
    String authType = "testAuthType";
    UserIdentityContextHolder.setAuthType(authType);
    assertEquals(authType, UserIdentityContextHolder.getAuthType());
  }

  @Test
  public void setAuthType_NullAuthType_ShouldSetCorrectly() {
    UserIdentityContextHolder.setAuthType(null);
    assertNull(UserIdentityContextHolder.getAuthType());
  }

  @Test
  public void getAuthType_WhenNotSet_ShouldReturnNull() {
    // Test: getAuthType should return null when AUTH_TYPE_HOLDER is not set
    assertNull(UserIdentityContextHolder.getAuthType(),
        "Expected null when AUTH_TYPE_HOLDER is not set");
  }

  @Test
  public void getAuthType_WhenSet_ShouldReturnCorrectValue() {
    // Setup: Set a value in AUTH_TYPE_HOLDER
    UserIdentityContextHolder.setAuthType("TestAuthType");

    // Test: getAuthType should return the set value
    assertEquals("TestAuthType", UserIdentityContextHolder.getAuthType(),
        "Expected 'TestAuthType' when AUTH_TYPE_HOLDER is set");
  }

  @Test
  public void clear_ShouldRemoveAuthTypeHolderValue() {
    // Step 1: Set authentication type
    UserIdentityContextHolder.setAuthType("testValue");
    assertEquals("testValue", UserIdentityContextHolder.getAuthType()); // Verify setup success

    // Step 2: Call clear() method
    UserIdentityContextHolder.clear();

    // Step 3: Verify result after clearing (get value via public method)
    assertNull(UserIdentityContextHolder.getAuthType()); // Directly verify getAuthType() returns
                                                         // null
  }
}
