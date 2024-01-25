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
package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.AbstractIntegrationTest;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

public class AppNamespaceServiceTest extends AbstractIntegrationTest {

  @Autowired
  private AppNamespaceService appNamespaceService;

  private final String APP = "app-test";

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindPublicAppNamespace() {

    List<AppNamespace> appNamespaceList = appNamespaceService.findPublicAppNamespaces();

    Assert.assertNotNull(appNamespaceList);
    Assert.assertEquals(5, appNamespaceList.size());

  }

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindPublicAppNamespaceByName() {

    Assert.assertNotNull(appNamespaceService.findPublicAppNamespace("datasourcexml"));

    Assert.assertNull(appNamespaceService.findPublicAppNamespace("TFF.song0711-02"));
  }

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testFindPublicAppNamespaceByAppAndName() {

    Assert.assertNotNull(appNamespaceService.findByAppIdAndName("100003173", "datasourcexml"));

    Assert.assertNull(appNamespaceService.findByAppIdAndName("100003173", "TFF.song0711-02"));
  }


  @Test
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreateDefaultAppNamespace() {
    appNamespaceService.createDefaultAppNamespace(APP);

    AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(APP, ConfigConsts.NAMESPACE_APPLICATION);

    Assert.assertNotNull(appNamespace);
    Assert.assertEquals(ConfigFileFormat.Properties.getValue(), appNamespace.getFormat());

  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePublicAppNamespaceExisted() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(true);
    appNamespace.setName("old");
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());

    appNamespaceService.createAppNamespaceInLocal(appNamespace);
  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePublicAppNamespaceExistedAsPrivateAppNamespace() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(true);
    appNamespace.setName("private-01");
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());

    appNamespaceService.createAppNamespaceInLocal(appNamespace);
  }

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePublicAppNamespaceNotExistedWithNoAppendnamespacePrefix() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(true);
    appNamespace.setName("old");
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());

    AppNamespace createdAppNamespace = appNamespaceService.createAppNamespaceInLocal(appNamespace, false);

    Assert.assertNotNull(createdAppNamespace);
    Assert.assertEquals(appNamespace.getName(), createdAppNamespace.getName());
  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePublicAppNamespaceExistedWithNoAppendnamespacePrefix() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(true);
    appNamespace.setName("datasource");
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());

    appNamespaceService.createAppNamespaceInLocal(appNamespace, false);
  }

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePublicAppNamespaceNotExisted() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(true);

    appNamespaceService.createAppNamespaceInLocal(appNamespace);

    AppNamespace createdAppNamespace = appNamespaceService.findPublicAppNamespace(appNamespace.getName());

    Assert.assertNotNull(createdAppNamespace);
    Assert.assertEquals(appNamespace.getName(), createdAppNamespace.getName());
  }

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePublicAppNamespaceWithWrongFormatNotExisted() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(true);
    appNamespace.setFormat(ConfigFileFormat.YAML.getValue());

    appNamespaceService.createAppNamespaceInLocal(appNamespace);

    AppNamespace createdAppNamespace = appNamespaceService.findPublicAppNamespace(appNamespace.getName());

    Assert.assertNotNull(createdAppNamespace);
    Assert.assertEquals(appNamespace.getName(), createdAppNamespace.getName());
    Assert.assertEquals(ConfigFileFormat.YAML.getValue(), createdAppNamespace.getFormat());
  }


  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePrivateAppNamespaceExisted() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(false);
    appNamespace.setName("datasource");
    appNamespace.setAppId("100003173");

    appNamespaceService.createAppNamespaceInLocal(appNamespace);
  }

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePrivateAppNamespaceExistedInAnotherAppId() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(false);
    appNamespace.setName("datasource");
    appNamespace.setAppId("song0711-01");

    appNamespaceService.createAppNamespaceInLocal(appNamespace);

    AppNamespace createdAppNamespace =
        appNamespaceService.findByAppIdAndName(appNamespace.getAppId(), appNamespace.getName());

    Assert.assertNotNull(createdAppNamespace);
    Assert.assertEquals(appNamespace.getName(), createdAppNamespace.getName());
  }

  @Test(expected = BadRequestException.class)
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePrivateAppNamespaceExistedInAnotherAppIdAsPublic() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(false);
    appNamespace.setName("SCC.song0711-03");
    appNamespace.setAppId("100003173");
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());

    appNamespaceService.createAppNamespaceInLocal(appNamespace);
  }

  @Test
  @Sql(scripts = "/sql/appnamespaceservice/init-appnamespace.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testCreatePrivateAppNamespaceNotExisted() {
    AppNamespace appNamespace = assembleBaseAppNamespace();
    appNamespace.setPublic(false);

    appNamespaceService.createAppNamespaceInLocal(appNamespace);

    AppNamespace createdAppNamespace =
        appNamespaceService.findByAppIdAndName(appNamespace.getAppId(), appNamespace.getName());

    Assert.assertNotNull(createdAppNamespace);
    Assert.assertEquals(appNamespace.getName(), createdAppNamespace.getName());
  }

  private AppNamespace assembleBaseAppNamespace() {
    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setName("appNamespace");
    appNamespace.setAppId("1000");
    appNamespace.setFormat(ConfigFileFormat.XML.getValue());
    return appNamespace;
  }

}
