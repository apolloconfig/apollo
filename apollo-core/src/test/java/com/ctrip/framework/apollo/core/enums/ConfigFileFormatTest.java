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
package com.ctrip.framework.apollo.core.enums;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ConfigFileFormatTest {

  @Rule
  public ExpectedException expectedEx = ExpectedException.none();

  @Test
  public void testFromStringEqualsOriginal() {
    assertEquals(ConfigFileFormat.Properties,
        ConfigFileFormat.fromString(ConfigFileFormat.Properties.getValue()));
    assertEquals(ConfigFileFormat.XML,
        ConfigFileFormat.fromString(ConfigFileFormat.XML.getValue()));
    assertEquals(ConfigFileFormat.JSON,
        ConfigFileFormat.fromString(ConfigFileFormat.JSON.getValue()));
    assertEquals(ConfigFileFormat.YML,
        ConfigFileFormat.fromString(ConfigFileFormat.YML.getValue()));
    assertEquals(ConfigFileFormat.YAML,
        ConfigFileFormat.fromString(ConfigFileFormat.YAML.getValue()));
    assertEquals(ConfigFileFormat.TXT,
        ConfigFileFormat.fromString(ConfigFileFormat.TXT.getValue()));
  }

  @Test
  public void testNonExistingValueFromString() {
    expectedEx.expect(IllegalArgumentException.class);
    expectedEx.expectMessage("thisShouldNotExistPropertiesXML can not map enum");

    ConfigFileFormat.fromString("thisShouldNotExistPropertiesXML");
  }

  @Test
  public void testEmptyValueFromString() {
    expectedEx.expect(IllegalArgumentException.class);
    expectedEx.expectMessage("value can not be empty");

    ConfigFileFormat.fromString("");
  }

  @Test
  public void testSpacedValueFromString() {
    expectedEx.expect(IllegalArgumentException.class);
    expectedEx.expectMessage(" can not map enum");

    ConfigFileFormat.fromString("    ");
  }

  @Test
  public void testSpecialCharsValueFromString() {
    ArrayList<String> specialChars = new ArrayList<>();
    specialChars.add(" ");
    specialChars.add("\t");
    specialChars.add(" \t");
    specialChars.add(" \t ");
    specialChars.add("\t ");

    specialChars.forEach(item -> {
      assertEquals(ConfigFileFormat.Properties,
          ConfigFileFormat.fromString(item + ConfigFileFormat.Properties.getValue() + item));
      assertEquals(ConfigFileFormat.XML,
          ConfigFileFormat.fromString(item + ConfigFileFormat.XML.getValue() + item));
      assertEquals(ConfigFileFormat.JSON,
          ConfigFileFormat.fromString(item + ConfigFileFormat.JSON.getValue() + item));
      assertEquals(ConfigFileFormat.YML,
          ConfigFileFormat.fromString(item + ConfigFileFormat.YML.getValue() + item));
      assertEquals(ConfigFileFormat.YAML,
          ConfigFileFormat.fromString(item + ConfigFileFormat.YAML.getValue() + item));
      assertEquals(ConfigFileFormat.TXT,
          ConfigFileFormat.fromString(item + ConfigFileFormat.TXT.getValue() + item));
    });
  }

  @Test
  public void testIsValidFormatForOriginalContent() {
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.Properties.getValue()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.XML.getValue()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.JSON.getValue()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.YML.getValue()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.YAML.getValue()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.TXT.getValue()));

    assertTrue(
        ConfigFileFormat.isValidFormat(ConfigFileFormat.Properties.getValue().toUpperCase()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.XML.getValue().toUpperCase()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.JSON.getValue().toUpperCase()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.YML.getValue().toUpperCase()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.YAML.getValue().toUpperCase()));
    assertTrue(ConfigFileFormat.isValidFormat(ConfigFileFormat.TXT.getValue().toUpperCase()));
  }

  @Test
  public void testIsValidFormatForInvalid() {
    assertFalse(ConfigFileFormat.isValidFormat("thisshouldnotexist"));
  }

  @Test
  public void testIfPropertiesCompatible() {
    assertTrue(ConfigFileFormat.isPropertiesCompatible(ConfigFileFormat.YAML));
    assertTrue(ConfigFileFormat.isPropertiesCompatible(ConfigFileFormat.YML));
  }
}
