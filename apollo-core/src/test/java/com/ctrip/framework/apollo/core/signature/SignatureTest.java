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
import static org.junit.Assert.assertTrue;

import com.google.common.net.HttpHeaders;
import java.util.Map;
import org.junit.Test;

/**
 * @author nisiyong
 */
public class SignatureTest {

  @Test
  public void testSignature() {
    String timestamp = "1576478257344";
    String pathWithQuery = "/configs/100004458/default/application?ip=10.0.0.1";
    String secret = "df23df3f59884980844ff3dada30fa97";

    String actualSignature = Signature.signature(timestamp, pathWithQuery, secret);

    String expectedSignature = "EoKyziXvKqzHgwx+ijDJwgVTDgE=";
    assertEquals(expectedSignature, actualSignature);
  }

  @Test
  public void testBuildHttpHeaders() {
    String url = "http://10.0.0.1:8080/configs/100004458/default/application?ip=10.0.0.1";
    String appId = "100004458";
    String secret = "df23df3f59884980844ff3dada30fa97";

    Map<String, String> actualHttpHeaders = Signature.buildHttpHeaders(url, appId, secret);

    assertTrue(actualHttpHeaders.containsKey(HttpHeaders.AUTHORIZATION));
    assertTrue(actualHttpHeaders.containsKey(Signature.HTTP_HEADER_TIMESTAMP));
  }
}