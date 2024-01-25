/*
 * Copyright 2024 Apollo Authors
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

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.google.common.base.Strings;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class AuthUserPasswordChecker implements UserPasswordChecker {

  private static final Pattern PWD_PATTERN = Pattern
      .compile("^(?=.*[0-9].*)(?=.*[a-zA-Z].*).{8,20}$");

  private final PortalConfig portalConfig;

  public AuthUserPasswordChecker(final PortalConfig portalConfig) {
    this.portalConfig = portalConfig;
  }

  @Override
  public CheckResult checkWeakPassword(String password) {
    if (Strings.isNullOrEmpty(password) || !PWD_PATTERN.matcher(password).matches()) {
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
   * @return The password contains code fragment.
   */
  private boolean isCommonlyUsed(String password) {
    List<String> list = portalConfig.getUserPasswordNotAllowList();
    if (list == null || list.isEmpty()) {
      return false;
    }
    for (String s : list) {
      if (password.toLowerCase().contains(s)) {
        return true;
      }
    }
    return false;
  }
}
