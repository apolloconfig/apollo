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

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.NamespaceContentSyntaxValidator;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;

/**
 * Checks namespace text syntax shared by Portal WebAPI and OpenAPI controllers. This is fast,
 * non-authoritative feedback for the UI - the actual save is authoritatively enforced in
 * apollo-biz's {@code ItemService}. Both routes share {@link NamespaceContentSyntaxValidator} so
 * a namespace can't pass this check with content the save path would then reject (e.g. a
 * multi-document YAML stream, which Spring's YAML loader accepts but the single-document,
 * authoritative check does not).
 */
public final class NamespaceTextSyntaxChecker {

  private NamespaceTextSyntaxChecker() {}

  public static void check(NamespaceTextModel model) {
    if (StringUtils.isBlank(model.getConfigText())) {
      return;
    }

    ConfigFileFormat format = model.getFormat();
    try {
      NamespaceContentSyntaxValidator.validate(format, model.getConfigText());
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(ex.getMessage());
    }
  }
}
