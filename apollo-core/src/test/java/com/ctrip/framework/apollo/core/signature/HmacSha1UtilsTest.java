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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * @author nisiyong
 */
public class HmacSha1UtilsTest {

  @Test
  public void testSignString() {
    String stringToSign = "1576478257344\n/configs/100004458/default/application?ip=10.0.0.1";
    String accessKeySecret = "df23df3f59884980844ff3dada30fa97";

    String actualSignature = HmacSha1Utils.signString(stringToSign, accessKeySecret);

    String expectedSignature = "EoKyziXvKqzHgwx+ijDJwgVTDgE=";
    assertEquals(expectedSignature, actualSignature);
  }
}