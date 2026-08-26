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
package com.ctrip.framework.apollo.common.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import org.junit.jupiter.api.Test;

class NamespaceContentSyntaxValidatorTest {

  @Test
  void isStrictlyValidatedTrueForJsonYamlYml() {
    assertTrue(NamespaceContentSyntaxValidator.isStrictlyValidated(ConfigFileFormat.JSON));
    assertTrue(NamespaceContentSyntaxValidator.isStrictlyValidated(ConfigFileFormat.YAML));
    assertTrue(NamespaceContentSyntaxValidator.isStrictlyValidated(ConfigFileFormat.YML));
  }

  @Test
  void isStrictlyValidatedFalseForOtherFormats() {
    assertFalse(NamespaceContentSyntaxValidator.isStrictlyValidated(ConfigFileFormat.Properties));
    assertFalse(NamespaceContentSyntaxValidator.isStrictlyValidated(ConfigFileFormat.XML));
    assertFalse(NamespaceContentSyntaxValidator.isStrictlyValidated(ConfigFileFormat.TXT));
  }

  @Test
  void validJsonPasses() {
    assertDoesNotThrow(() -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.JSON,
        "{\"name\": \"apollo\", \"age\": 1}"));
  }

  @Test
  void malformedJsonThrows() {
    assertThrows(IllegalArgumentException.class,
        () -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.JSON, "{\"name\": "));
  }

  @Test
  void jsonWithTrailingTokensThrows() {
    // readTree() otherwise silently ignores anything after the first well-formed JSON value
    assertThrows(IllegalArgumentException.class, () -> NamespaceContentSyntaxValidator
        .validate(ConfigFileFormat.JSON, "{\"name\": \"apollo\"} garbage"));
  }

  @Test
  void jsonWithTrailingJsonValueThrows() {
    assertThrows(IllegalArgumentException.class, () -> NamespaceContentSyntaxValidator
        .validate(ConfigFileFormat.JSON, "{\"name\": \"apollo\"} {\"name\": \"apollo2\"}"));
  }

  @Test
  void validYamlPasses() {
    assertDoesNotThrow(() -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.YAML,
        "name: apollo\nage: 1"));
    assertDoesNotThrow(() -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.YML,
        "name: apollo\nage: 1"));
  }

  @Test
  void malformedYamlThrows() {
    assertThrows(IllegalArgumentException.class, () -> NamespaceContentSyntaxValidator
        .validate(ConfigFileFormat.YAML, "name: apollo\n  bad indent: - ["));
  }

  @Test
  void yamlWithTrailingDocumentThrows() {
    // a multi-document (`---`-separated) YAML stream is not a single well-formed document
    assertThrows(IllegalArgumentException.class, () -> NamespaceContentSyntaxValidator
        .validate(ConfigFileFormat.YAML, "name: apollo\n---\nname: apollo2"));
  }

  @Test
  void yamlWithDuplicateKeysThrows() {
    assertThrows(IllegalArgumentException.class, () -> NamespaceContentSyntaxValidator
        .validate(ConfigFileFormat.YAML, "name: apollo\nname: apollo2"));
  }

  @Test
  void nonStrictFormatsAreNeverValidated() {
    // garbage content on properties/xml/txt namespaces is not this validator's concern
    assertDoesNotThrow(() -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.Properties,
        "{ not json, not properties either"));
    assertDoesNotThrow(
        () -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.XML, "not xml at all"));
    assertDoesNotThrow(
        () -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.TXT, "anything goes"));
  }

  @Test
  void blankContentIsAlwaysValid() {
    assertDoesNotThrow(() -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.JSON, ""));
    assertDoesNotThrow(() -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.JSON, null));
    assertDoesNotThrow(() -> NamespaceContentSyntaxValidator.validate(ConfigFileFormat.YAML, "  "));
  }
}
