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

import com.ctrip.framework.apollo.core.utils.StringUtils;
import java.util.stream.Stream;

/**
 * This enum represents all the possible Configuration file formats apollo currently supports.
 * <p>
 * Currently the following formats are supported:
 * <ul>
 *   <li>{@link ConfigFileFormat#Properties}</li>
 *   <li>{@link ConfigFileFormat#XML}</li>
 *   <li>{@link ConfigFileFormat#JSON}</li>
 *   <li>{@link ConfigFileFormat#YML}</li>
 *   <li>{@link ConfigFileFormat#YAML}</li>
 *   <li>{@link ConfigFileFormat#TXT}</li>
 * </ul>
 *
 * @author Jason Song(song_s@ctrip.com)
 * @author Diego Krupitza(info@diegokrupitza.com)
 */
public enum ConfigFileFormat {
  Properties("properties"), XML("xml"), JSON("json"), YML("yml"), YAML("yaml"), TXT("txt");

  private final String value;

  ConfigFileFormat(String value) {
    this.value = value;
  }

  /**
   * Cleans a given configFilename so it does not contain leading or trailing spaces and is always
   * lowercase.
   * <p>
   * For example:
   *
   * <table border="1" cellspacing="1">
   *   <tr>
   *   <th>Before</th>
   *   <th>After</th>
   *   </tr>
   *   <tr>
   *     <td>"Properties "</td>
   *     <td>"properties"</td>
   *   </tr>
   *   <tr>
   *    <td>"    "</td>
   *    <td>""</td>
   *    </tr>
   * </table>
   *
   * @param configFileName the name we want to clean
   * @return the cleansed configFileName
   */
  private static String getWellFormedName(String configFileName) {
    if (StringUtils.isBlank(configFileName)) {
      return "";
    }
    return configFileName.trim().toLowerCase();
  }

  /**
   * Transforms a given string to its matching {@link ConfigFileFormat}.
   *
   * @param value the string that matches
   * @return the matching {@link ConfigFileFormat}
   * @throws IllegalArgumentException in case the <code>value</code> is empty or there is no
   *                                  matching {@link ConfigFileFormat}
   */
  public static ConfigFileFormat fromString(String value) {
    if (StringUtils.isEmpty(value)) {
      throw new IllegalArgumentException("value can not be empty");
    }

    final String cleansedName = getWellFormedName(value);

    return Stream.of(ConfigFileFormat.values())
        .filter(item -> cleansedName.equalsIgnoreCase(item.getValue()))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(value + " can not map enum"));
  }

  /**
   * Checks if a given string is a valid {@link ConfigFileFormat}.
   *
   * @param value the string to check on
   * @return is it a valid format
   */
  public static boolean isValidFormat(String value) {
    try {
      fromString(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Checks whether a given {@link ConfigFileFormat} is compatible with {@link
   * ConfigFileFormat#Properties}
   * <p>
   * <b>Note: </b> if you call this method with {@link ConfigFileFormat#Properties} it will return
   * <code>false</code>.
   *
   * @param format the format to check its compatibility
   * @return is it compatible with {@link ConfigFileFormat#Properties}
   */
  public static boolean isPropertiesCompatible(ConfigFileFormat format) {
    return format == YAML || format == YML;
  }

  /**
   * @return The string representation of the given {@link ConfigFileFormat}
   */
  public String getValue() {
    return value;
  }
}
