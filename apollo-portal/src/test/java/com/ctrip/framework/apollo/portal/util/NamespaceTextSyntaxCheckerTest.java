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
package com.ctrip.framework.apollo.portal.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import org.junit.jupiter.api.Test;

class NamespaceTextSyntaxCheckerTest {

  @Test
  void blankConfigTextIsAlwaysValid() {
    assertDoesNotThrow(() -> NamespaceTextSyntaxChecker.check(model("yaml", "")));
    assertDoesNotThrow(() -> NamespaceTextSyntaxChecker.check(model("json", null)));
  }

  @Test
  void nonCheckedFormatsAreNeverValidated() {
    assertDoesNotThrow(
        () -> NamespaceTextSyntaxChecker.check(model("properties", "not = properties [[[")));
    assertDoesNotThrow(() -> NamespaceTextSyntaxChecker.check(model("xml", "not xml at all")));
    assertDoesNotThrow(() -> NamespaceTextSyntaxChecker.check(model("txt", "anything goes")));
  }

  @Test
  void validYamlPasses() {
    assertDoesNotThrow(
        () -> NamespaceTextSyntaxChecker.check(model("yaml", "name: apollo\nage: 1")));
    assertDoesNotThrow(
        () -> NamespaceTextSyntaxChecker.check(model("yml", "name: apollo\nage: 1")));
  }

  @Test
  void malformedYamlThrows() {
    assertThrows(BadRequestException.class,
        () -> NamespaceTextSyntaxChecker.check(model("yaml", "name: apollo\n  bad indent: - [")));
  }

  @Test
  void yamlWithMultipleDocumentsThrows() {
    // Spring's YamlPropertiesFactoryBean (the portal's old syntax check) treats `---`-separated
    // documents as valid, merged config - but the authoritative save path requires a single
    // document, so this must be rejected here too, not just at save time
    assertThrows(BadRequestException.class,
        () -> NamespaceTextSyntaxChecker.check(model("yaml", "name: apollo\n---\nname: apollo2")));
    assertThrows(BadRequestException.class,
        () -> NamespaceTextSyntaxChecker.check(model("yml", "name: apollo\n---\nname: apollo2")));
  }

  @Test
  void yamlWithDuplicateKeysThrows() {
    assertThrows(BadRequestException.class,
        () -> NamespaceTextSyntaxChecker.check(model("yaml", "name: apollo\nname: apollo2")));
  }

  @Test
  void validJsonPasses() {
    assertDoesNotThrow(() -> NamespaceTextSyntaxChecker
        .check(model("json", "{\"name\": \"apollo\", \"age\": 1}")));
  }

  @Test
  void malformedJsonThrows() {
    assertThrows(BadRequestException.class,
        () -> NamespaceTextSyntaxChecker.check(model("json", "{\"name\": ")));
  }

  @Test
  void jsonWithTrailingTokensThrows() {
    // readTree() otherwise silently ignores anything after the first well-formed JSON value
    assertThrows(BadRequestException.class,
        () -> NamespaceTextSyntaxChecker.check(model("json", "{\"name\": \"apollo\"} garbage")));
  }

  private static NamespaceTextModel model(String format, String configText) {
    NamespaceTextModel model = new NamespaceTextModel();
    model.setFormat(format);
    model.setConfigText(configText);
    return model;
  }
}
