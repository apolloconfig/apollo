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
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.representer.Representer;

/**
 * Checks namespace text syntax shared by Portal WebAPI and OpenAPI controllers. This is fast,
 * non-authoritative feedback for the UI - the actual save is authoritatively enforced in
 * apollo-biz's {@code ItemService}.
 */
public final class NamespaceTextSyntaxChecker {

  private NamespaceTextSyntaxChecker() {}

  public static void check(NamespaceTextModel model) {
    if (StringUtils.isBlank(model.getConfigText())) {
      return;
    }

    ConfigFileFormat format = model.getFormat();
    if (format == ConfigFileFormat.YAML || format == ConfigFileFormat.YML) {
      checkYamlSyntax(model.getConfigText());
    } else if (format == ConfigFileFormat.JSON) {
      checkJsonSyntax(model.getConfigText());
    }
  }

  private static void checkYamlSyntax(String configText) {
    TypeLimitedYamlPropertiesFactoryBean yamlPropertiesFactoryBean =
        new TypeLimitedYamlPropertiesFactoryBean();
    yamlPropertiesFactoryBean
        .setResources(new ByteArrayResource(configText.getBytes(StandardCharsets.UTF_8)));
    try {
      yamlPropertiesFactoryBean.getObject();
    } catch (Exception ex) {
      throw new BadRequestException(ex.getMessage());
    }
  }

  private static void checkJsonSyntax(String configText) {
    try {
      NamespaceContentSyntaxValidator.validate(ConfigFileFormat.JSON, configText);
    } catch (IllegalArgumentException ex) {
      throw new BadRequestException(ex.getMessage());
    }
  }

  private static class TypeLimitedYamlPropertiesFactoryBean extends YamlPropertiesFactoryBean {

    @Override
    protected Yaml createYaml() {
      LoaderOptions loaderOptions = new LoaderOptions();
      loaderOptions.setAllowDuplicateKeys(false);
      DumperOptions dumperOptions = new DumperOptions();
      return new Yaml(new SafeConstructor(loaderOptions), new Representer(dumperOptions),
          dumperOptions, loaderOptions);
    }
  }
}
