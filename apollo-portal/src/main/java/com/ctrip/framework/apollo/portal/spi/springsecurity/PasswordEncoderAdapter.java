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
package com.ctrip.framework.apollo.portal.spi.springsecurity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
@Deprecated
public class PasswordEncoderAdapter implements PasswordEncoder {

  private static final String PREFIX = "{";

  private static final String SUFFIX = "}";

  private final PasswordEncoder encoder;

  public PasswordEncoderAdapter(
      PasswordEncoder encoder) {
    this.encoder = encoder;
  }

  @Override
  public String encode(CharSequence rawPassword) {
    throw new UnsupportedOperationException("encode is not supported");
  }

  @Override
  public boolean matches(CharSequence rawPassword, String encodedPassword) {
    boolean matches = this.encoder.matches(rawPassword, encodedPassword);
    if (matches) {
      return true;
    }
    String id = this.extractId(encodedPassword);
    if (StringUtils.hasText(id)) {
      throw new IllegalArgumentException(
          "There is no PasswordEncoder mapped for the id \"" + id + "\"");
    }
    return false;
  }

  private String extractId(String prefixEncodedPassword) {
    if (prefixEncodedPassword == null) {
      return null;
    }
    int start = prefixEncodedPassword.indexOf(PREFIX);
    if (start != 0) {
      return null;
    }
    int end = prefixEncodedPassword.indexOf(SUFFIX, start);
    if (end < 0) {
      return null;
    }
    return prefixEncodedPassword.substring(start + 1, end);
  }

}
