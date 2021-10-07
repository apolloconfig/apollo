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
package com.ctrip.framework.apollo.portal.util;

import com.ctrip.framework.apollo.portal.util.checker.AuthUserPasswordChecker;
import com.ctrip.framework.apollo.portal.util.checker.CheckResult;
import java.util.Arrays;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AuthUserPasswordCheckerTest {

  private AuthUserPasswordChecker checker;

  @Before
  public void setup() {
    checker = new AuthUserPasswordChecker();
  }

  @Test
  public void testRegexMatch() {
    List<String> unMatchList = Arrays.asList(
        "11111111",
        "oibjdiel",
        "oso87b6",
        "0vb9xibowkd8bz9dsxbef"
    );
    String exceptedErrMsg = "Password needs a number and letter and between 8~20 characters";

    for (String p : unMatchList) {
      CheckResult res = checker.checkWeakPassword(p);
      Assert.assertFalse(res.isSuccess());
      Assert.assertEquals(exceptedErrMsg, res.getMessage());
    }

    List<String> matchList = Arrays.asList(
        "pziv0g87",
        "8f7zjpf8sci93",
        "Upz4jF8u2yjV3wn8zp6c"
    );

    for (String p : matchList) {
      CheckResult res = checker.checkWeakPassword(p);
      Assert.assertTrue(res.isSuccess());
    }
  }

  @Test
  public void testIsWeakPassword() {
    List<String> weakPwdList = Arrays.asList(
        "a1234567", "b98765432", "c11111111", "d2222222", "e3333333", "f4444444",
        "g5555555", "h6666666", "i7777777", "j8888888", "k9999999", "l0000000",
        "1q2w3e4r", "qwertyuiop1", "asdfghjkl2", "asdfghjkl3", "abcd1234"
    );
    String exceptedErrMsg =
        "Passwords cannot be consecutive, regular letters or numbers. And cannot be commonly used.";

    for (String p : weakPwdList) {
      CheckResult res = checker.checkWeakPassword(p);
      Assert.assertFalse(res.isSuccess());
      Assert.assertTrue(res.getMessage().startsWith(exceptedErrMsg));
    }

    CheckResult res = checker.checkWeakPassword("1s39gvisk");
    Assert.assertTrue(res.isSuccess());
  }

}