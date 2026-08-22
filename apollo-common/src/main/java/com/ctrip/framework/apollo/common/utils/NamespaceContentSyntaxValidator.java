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

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Validates that a {@code json}/{@code yml}/{@code yaml} namespace's content is well-formed.
 * This checks syntax only - whether the text actually parses as JSON/YAML - it does not check
 * the content against any particular structure.
 *
 * <p>This class is intentionally shared (apollo-common) so it can be used both as the
 * authoritative, server-side check in apollo-biz's {@code ItemService}, and as fast, non
 * -authoritative feedback in apollo-portal's syntax-check endpoints.</p>
 */
public final class NamespaceContentSyntaxValidator {

  // readTree() only parses the first JSON value in the input and silently ignores anything
  // after it by default - e.g. "{\"a\":1} garbage" would otherwise be accepted as well-formed
  private static final ObjectMapper OBJECT_MAPPER =
      JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

  private NamespaceContentSyntaxValidator() {}

  /**
   * @return {@code true} if {@code format}'s content is strictly checked for well-formedness by
   *     {@link #validate}; {@code false} for properties/xml/txt, which aren't checked here.
   */
  public static boolean isStrictlyValidated(ConfigFileFormat format) {
    return format == ConfigFileFormat.JSON || format == ConfigFileFormat.YAML
        || format == ConfigFileFormat.YML;
  }

  /**
   * Validates that {@code content} is well-formed for the given {@code format}. A no-op for
   * formats {@link #isStrictlyValidated} doesn't cover, and for blank content.
   *
   * @throws IllegalArgumentException if {@code content} is not valid JSON/YAML
   */
  public static void validate(ConfigFileFormat format, String content) {
    if (!isStrictlyValidated(format) || StringUtils.isBlank(content)) {
      return;
    }
    try {
      if (format == ConfigFileFormat.JSON) {
        OBJECT_MAPPER.readTree(content);
      } else {
        // YAML / YML: must be a single well-formed document - not a multi-document
        // (`---`-separated) stream, and without silently-overwritten duplicate keys
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);
        Yaml yaml = new Yaml(new SafeConstructor(loaderOptions));
        yaml.load(content);
      }
    } catch (Exception ex) {
      throw new IllegalArgumentException(
          "invalid " + format.getValue() + " content: " + ex.getMessage(), ex);
    }
  }
}
