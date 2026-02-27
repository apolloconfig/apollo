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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SignInControllerTest {

  private final SignInController signInController = new SignInController();

  @Test
  public void shouldAlwaysReturnLoginPage() {
    assertEquals("login.html", signInController.login(null, null));
    assertEquals("login.html", signInController.login("error", null));
    assertEquals("login.html", signInController.login(null, "logout"));
  }
}
