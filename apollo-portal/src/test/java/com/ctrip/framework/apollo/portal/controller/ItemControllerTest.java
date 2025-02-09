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
package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.component.UserPermissionValidator;
import com.ctrip.framework.apollo.portal.entity.model.NamespaceTextModel;
import com.ctrip.framework.apollo.portal.service.ItemService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ItemControllerTest {

  @Mock
  private ItemService configService;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private UserInfoHolder userInfoHolder;
  @Mock
  private UserPermissionValidator userPermissionValidator;

  @InjectMocks
  private ItemController itemController;

  @Before
  public void setUp() throws Exception {
    itemController = new ItemController(configService, userInfoHolder, userPermissionValidator,
        namespaceService);
  }

  @Test
  public void yamlSyntaxCheckOK() throws Exception {
    String yaml = loadYaml("case1.yaml");

    itemController.doSyntaxCheck(assemble(ConfigFileFormat.YAML.getValue(), yaml));
  }

  @Test(expected = BadRequestException.class)
  public void yamlSyntaxCheckWithDuplicatedValue() throws Exception {
    String yaml = loadYaml("case2.yaml");

    itemController.doSyntaxCheck(assemble(ConfigFileFormat.YAML.getValue(), yaml));
  }

  @Test(expected = BadRequestException.class)
  public void yamlSyntaxCheckWithUnsupportedType() throws Exception {
    String yaml = loadYaml("case3.yaml");

    itemController.doSyntaxCheck(assemble(ConfigFileFormat.YAML.getValue(), yaml));
  }

  private NamespaceTextModel assemble(String format, String content) {
    NamespaceTextModel model = new NamespaceTextModel();
    model.setFormat(format);
    model.setConfigText(content);

    return model;
  }

  private String loadYaml(String caseName) throws IOException {
    File file = new File("src/test/resources/yaml/" + caseName);

    return Files.toString(file, Charsets.UTF_8);
  }
}