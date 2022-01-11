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
package com.ctrip.framework.apollo.portal.util.checker;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AuthUserPasswordChecker implements UserPasswordChecker {

  private static final Pattern PWD_PATTERN = Pattern
      .compile("^(?=.*[0-9].*)(?=.*[a-zA-Z].*).{8,20}$");

  private static final List<String> LIST_OF_CODE_FRAGMENT = Arrays.asList(
      "111", "222", "333", "444", "555", "666", "777", "888", "999", "000",
      "001122", "112233", "223344", "334455", "445566", "556677", "667788", "778899", "889900",
      "009988", "998877", "887766", "776655", "665544", "554433", "443322", "332211", "221100",
      "0123", "1234", "2345", "3456", "4567", "5678", "6789", "7890",
      "0987", "9876", "8765", "7654", "6543", "5432", "4321", "3210",
      "1q2w", "2w3e", "3e4r", "5t6y", "abcd", "qwer", "asdf", "zxcv"
  );

  @Override
  public CheckResult checkWeakPassword(String password) {
    if (!PWD_PATTERN.matcher(password).matches()) {
      return new CheckResult(Boolean.FALSE,
          "Password needs a number and letter and between 8~20 characters");
    }
    if (isCommonlyUsed(password)) {
      return new CheckResult(Boolean.FALSE,
          "Passwords cannot be consecutive, regular letters or numbers. And cannot be commonly used. "
              + "e.g: abcd1234, 1234qwer, 1q2w3e4r, 1234asdfghjk, ...");
    }
    return new CheckResult(Boolean.TRUE, null);
  }

  /**
   * @return The password contains code fragment or is blank.
   */
  private boolean isCommonlyUsed(String password) {
    if (Strings.isNullOrEmpty(password)) {
      return true;
    }
    for (String s : LIST_OF_CODE_FRAGMENT) {
      if (password.toLowerCase().contains(s)) {
        return true;
      }
    }
    return false;
  }
}
