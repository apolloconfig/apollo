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
package com.ctrip.framework.apollo.build.sql.converter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

class ApolloSqlConverterH2Test {

  @Test
  void checkH2() {
    String repositoryDir = ApolloSqlConverterUtil.getRepositoryDir();

    String srcDir = repositoryDir + "/scripts/sql/src";
    String checkerParentDir =
        repositoryDir + "/apollo-build-sql-converter/target/scripts/sql/checker-h2";

    String testSrcDir =
        repositoryDir + "/apollo-build-sql-converter/src/test/resources/META-INF/sql/h2-test";
    String testCheckerParentDir =
        repositoryDir + "/apollo-build-sql-converter/target/scripts/sql/checker-h2-test";

    // generate checker sql files
    ApolloSqlConverterUtil.deleteDir(Paths.get(checkerParentDir));
    SqlTemplateGist gists = ApolloSqlConverterUtil.getGists(repositoryDir);
    SqlTemplateGist h2TestGist = gists.toBuilder().h2Function("\n" + "\n" + "-- H2 Function\n"
        + "-- ------------------------------------------------------------\n"
        + "CREATE ALIAS IF NOT EXISTS UNIX_TIMESTAMP FOR \"com.ctrip.framework.apollo.build.sql.converter.TestH2Function.unixTimestamp\";\n")
        .build();
    List<String> srcSqlList =
        ApolloSqlConverter.convert(repositoryDir, srcDir, checkerParentDir, h2TestGist);
    List<String> checkerSqlList = new ArrayList<>(srcSqlList.size());
    for (String srcSql : srcSqlList) {
      String checkerSql = ApolloSqlConverterUtil.replacePath(srcSql, srcDir,
          checkerParentDir + "/sql/profiles/h2-default");
      checkerSqlList.add(checkerSql);
    }

    // generate test checker sql files
    ApolloSqlConverterUtil.deleteDir(Paths.get(testCheckerParentDir));
    List<String> testSrcSqlList =
        ApolloSqlConverter.convert(repositoryDir, testSrcDir, testCheckerParentDir, h2TestGist);
    List<String> testCheckerSqlList = new ArrayList<>(testSrcSqlList.size());
    for (String testSrcSql : testSrcSqlList) {
      String testCheckerSql = ApolloSqlConverterUtil.replacePath(testSrcSql, testSrcDir,
          testCheckerParentDir + "/sql/profiles/h2-default");
      testCheckerSqlList.add(testCheckerSql);
    }

    this.checkSort(testSrcSqlList);

    String h2Path = "test-h2";
    this.checkConfigDatabase(h2Path, checkerSqlList, testCheckerSqlList);

    this.checkPortalDatabase(h2Path, checkerSqlList, testCheckerSqlList);
  }

  private void checkSort(List<String> testSrcSqlList) {
    int baseIndex = this.getIndex(testSrcSqlList, "apolloconfigdb-v000-v010-base.sql");
    Assertions.assertTrue(baseIndex >= 0);
    int beforeIndex = this.getIndex(testSrcSqlList, "apolloconfigdb-v000-v010-before.sql");
    Assertions.assertTrue(beforeIndex >= 0);
    int deltaIndex = this.getIndex(testSrcSqlList, "apolloconfigdb-v000-v010.sql");
    Assertions.assertTrue(deltaIndex >= 0);
    int afterIndex = this.getIndex(testSrcSqlList, "apolloconfigdb-v000-v010-after.sql");
    Assertions.assertTrue(afterIndex >= 0);

    // base < before < delta < after
    Assertions.assertTrue(baseIndex < beforeIndex);
    Assertions.assertTrue(beforeIndex < deltaIndex);
    Assertions.assertTrue(deltaIndex < afterIndex);
  }

  private int getIndex(List<String> srcSqlList, String fileName) {
    for (int i = 0; i < srcSqlList.size(); i++) {
      String sqlFile = srcSqlList.get(i);
      if (sqlFile.endsWith(fileName)) {
        return i;
      }
    }
    return -1;
  }

  private void checkConfigDatabase(String h2Path, List<String> checkerSqlList,
      List<String> testCheckerSqlList) {
    SimpleDriverDataSource configDataSource = new SimpleDriverDataSource();
    configDataSource.setUrl("jdbc:h2:mem:~/" + h2Path
        + "/apollo-config-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE");
    configDataSource.setDriverClass(org.h2.Driver.class);

    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setContinueOnError(false);
    populator.setSeparator(";");
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());

    for (String sqlFile : testCheckerSqlList) {
      if (sqlFile.contains("apolloconfigdb-")) {
        populator.addScript(new PathResource(Paths.get(sqlFile)));
      }
    }

    for (String sqlFile : checkerSqlList) {
      if (sqlFile.contains("apolloconfigdb-")) {
        populator.addScript(new PathResource(Paths.get(sqlFile)));
      }
    }

    DatabasePopulatorUtils.execute(populator, configDataSource);
  }

  private void checkPortalDatabase(String h2Path, List<String> checkerSqlList,
      List<String> testCheckerSqlList) {
    SimpleDriverDataSource portalDataSource = new SimpleDriverDataSource();
    portalDataSource.setUrl("jdbc:h2:mem:~/" + h2Path
        + "/apollo-portal-db;mode=mysql;DB_CLOSE_ON_EXIT=FALSE;DB_CLOSE_DELAY=-1;BUILTIN_ALIAS_OVERRIDE=TRUE;DATABASE_TO_UPPER=FALSE");
    portalDataSource.setDriverClass(org.h2.Driver.class);

    ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
    populator.setContinueOnError(false);
    populator.setSeparator(";");
    populator.setSqlScriptEncoding(StandardCharsets.UTF_8.name());

    for (String sqlFile : testCheckerSqlList) {
      if (sqlFile.contains("apolloportaldb-")) {
        populator.addScript(new PathResource(Paths.get(sqlFile)));
      }
    }

    for (String sqlFile : checkerSqlList) {
      if (sqlFile.contains("apolloportaldb-")) {
        populator.addScript(new PathResource(Paths.get(sqlFile)));
      }
    }

    DatabasePopulatorUtils.execute(populator, portalDataSource);
  }
}
