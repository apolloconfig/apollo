package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.component.PermissionValidator;
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
import org.yaml.snakeyaml.constructor.ConstructorException;

@RunWith(MockitoJUnitRunner.class)
public class ItemControllerTest {

  @Mock
  private ItemService configService;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private UserInfoHolder userInfoHolder;
  @Mock
  private PermissionValidator permissionValidator;

  @InjectMocks
  private ItemController itemController;

  @Before
  public void setUp() throws Exception {
    itemController = new ItemController(configService, userInfoHolder, permissionValidator,
        namespaceService);
  }

  @Test
  public void yamlSyntaxCheckOK() throws Exception {
    String yaml = loadYaml("case1.yaml");

    itemController.doSyntaxCheck(assemble(ConfigFileFormat.YAML.getValue(), yaml));
  }

  @Test(expected = IllegalStateException.class)
  public void yamlSyntaxCheckWithDuplicatedValue() throws Exception {
    String yaml = loadYaml("case2.yaml");

    itemController.doSyntaxCheck(assemble(ConfigFileFormat.YAML.getValue(), yaml));
  }

  @Test(expected = ConstructorException.class)
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