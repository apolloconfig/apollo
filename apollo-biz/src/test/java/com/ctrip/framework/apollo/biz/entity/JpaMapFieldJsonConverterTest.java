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
package com.ctrip.framework.apollo.biz.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class JpaMapFieldJsonConverterTest {

  private final JpaMapFieldJsonConverter converter = new JpaMapFieldJsonConverter();

  static String readAllContentOf(String path) throws IOException {
    ClassPathResource classPathResource = new ClassPathResource(path);
    byte[] bytes = Files.readAllBytes(classPathResource.getFile().toPath());
    return new String(bytes, StandardCharsets.UTF_8);
  }

  @Test
  void convertToDatabaseColumn_null() {
    assertEquals("null", this.converter.convertToDatabaseColumn(null));
  }

  @Test
  void convertToDatabaseColumn_empty() {
    assertEquals("{}", this.converter.convertToDatabaseColumn(new HashMap<>(4)));
  }

  @Test
  void convertToDatabaseColumn_oneElement() throws IOException {
    Map<String, String> map = new HashMap<>(8);
    map.put("a", "1");

    String expected = readAllContentOf("json/converter/element.1.json");
    assertEquals(expected, this.converter.convertToDatabaseColumn(map));
  }

  @Test
  void convertToDatabaseColumn_twoElement() throws IOException {
    Map<String, String> map = new LinkedHashMap<>(8);
    map.put("a", "1");
    map.put("disableCheck", "true");

    String expected = readAllContentOf("json/converter/element.2.json");
    assertEquals(expected, this.converter.convertToDatabaseColumn(map));
  }

  @Test
  void convertToEntityAttribute_null() {
    assertNull(this.converter.convertToEntityAttribute(null));
    assertNull(this.converter.convertToEntityAttribute("null"));
  }

  @Test
  void convertToEntityAttribute_null_oneElement() throws IOException {
    Map<String, String> map = new HashMap<>(8);
    map.put("a", "1");

    String content = readAllContentOf("json/converter/element.1.json");
    assertEquals(map, this.converter.convertToEntityAttribute(content));
  }

  @Test
  void convertToEntityAttribute_null_twoElement() throws IOException {
    Map<String, String> map = new HashMap<>(8);
    map.put("a", "1");
    map.put("disableCheck", "true");

    String content = readAllContentOf("json/converter/element.2.json");
    assertEquals(map, this.converter.convertToEntityAttribute(content));
  }
}