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
package com.ctrip.framework.apollo.core.signature;

import com.google.common.io.BaseEncoding;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author nisiyong
 */
public class HmacSha1Utils {

  private static final String ALGORITHM_NAME = "HmacSHA1";
  private static final String ENCODING = "UTF-8";

  public static String signString(String stringToSign, String accessKeySecret) {
    try {
      Mac mac = Mac.getInstance(ALGORITHM_NAME);
      mac.init(new SecretKeySpec(
          accessKeySecret.getBytes(ENCODING),
          ALGORITHM_NAME
      ));
      byte[] signData = mac.doFinal(stringToSign.getBytes(ENCODING));
      return BaseEncoding.base64().encode(signData);
    } catch (NoSuchAlgorithmException | UnsupportedEncodingException | InvalidKeyException e) {
      throw new IllegalArgumentException(e.toString());
    }
  }
}
