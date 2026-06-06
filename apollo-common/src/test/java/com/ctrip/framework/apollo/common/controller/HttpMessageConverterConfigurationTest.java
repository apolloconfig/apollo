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
package com.ctrip.framework.apollo.common.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.google.gson.Gson;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

public class HttpMessageConverterConfigurationTest {

  @Test
  public void gsonShouldSerializeAndDeserializeOffsetDateTimeAsIsoString() {
    Gson gson = new HttpMessageConverterConfiguration().gson();
    OffsetDateTime time = OffsetDateTime.parse("2026-06-06T08:00:00Z");

    String json = gson.toJson(time);
    OffsetDateTime parsed = gson.fromJson(json, OffsetDateTime.class);

    assertEquals("\"2026-06-06T08:00Z\"", json);
    assertEquals(time, parsed);
  }
}
