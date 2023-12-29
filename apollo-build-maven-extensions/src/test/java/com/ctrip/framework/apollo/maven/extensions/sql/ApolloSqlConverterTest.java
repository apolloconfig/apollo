/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.maven.extensions.sql;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ApolloSqlConverterTest {

  @Test
  void checkSql() throws URISyntaxException {
    String repositoryDir = this.getRepositoryDir();

    Map<String, SqlTemplate> templates = ApolloSqlConverterUtil.getTemplates(repositoryDir);

    String srcDir = repositoryDir + "/scripts/sql-src";
    String checkerParentDir = repositoryDir + "/apollo-build-maven-extensions/target/scripts";
    String repositoryParentDir = repositoryDir + "/scripts";

    // generate checker sql files
    List<String> srcSqlList = ApolloSqlConverter.convert(srcDir, checkerParentDir, templates);

    // compare checker sql files with repository sql files
    this.checkSqlList(srcSqlList, srcDir, checkerParentDir, repositoryParentDir);
  }

  private String getRepositoryDir() throws URISyntaxException {
    ProtectionDomain protectionDomain = ApolloSqlConverterTest.class.getProtectionDomain();
    CodeSource codeSource = protectionDomain.getCodeSource();
    URL location = codeSource.getLocation();
    URI uri = location.toURI();
    Path path = Paths.get(uri);
    String unixClassPath = path.toString().replace("\\", "/");

    Assertions.assertTrue(
        unixClassPath.endsWith("/apollo-build-maven-extensions/target/test-classes"));

    return ApolloSqlConverterUtil.replacePath(unixClassPath,
        "/apollo-build-maven-extensions/target/test-classes", "");
  }

  private void checkSqlList(List<String> srcSqlList, String srcDir, String checkerParentDir,
      String repositoryParentDir) {

    // '/scripts/sql'
    this.checkMainMysqlList(srcSqlList, srcDir, checkerParentDir, repositoryParentDir);

    // '/scripts/sql/assembly/mysql'
    this.checkAssemblyMysqlList(srcSqlList, srcDir, checkerParentDir, repositoryParentDir);

    // '/scripts/sql/assembly/h2'
    this.checkAssemblyH2List(srcSqlList, srcDir, checkerParentDir, repositoryParentDir);
  }

  private void checkMainMysqlList(List<String> srcSqlList, String srcDir,
      String checkerParentDir, String repositoryParentDir) {
    String checkerTargetDir = checkerParentDir + "/sql";
    String repositoryTargetDir = repositoryParentDir + "/sql";

    List<String> checkerSqlList = ApolloSqlConverterUtil.getSqlList(checkerTargetDir);

    Set<String> ignoreDirs = this.getIgnoreDirs(repositoryTargetDir);
    List<String> repositorySqlList = ApolloSqlConverterUtil.getSqlList(repositoryTargetDir,
        ignoreDirs);

    List<String> redundantSqlList = this.findRedundantSqlList(checkerTargetDir, checkerSqlList,
        repositoryTargetDir, repositorySqlList);
    Assertions.assertEquals(0, redundantSqlList.size(),
        "redundant sql files, please add sql files in 'scripts/sql-src' and then run 'mvn compile -Psql-convert' to generated. Do not edit 'scripts/sql' manually !!!\npath: "
            + redundantSqlList);

    List<String> missingSqlList = this.findMissingSqlList(checkerTargetDir, checkerSqlList,
        repositoryTargetDir, repositorySqlList);
    Assertions.assertEquals(0, missingSqlList.size(),
        "missing sql files, please run 'mvn compile -Psql-convert' to regenerated\npath: "
            + missingSqlList);

    for (String srcSql : srcSqlList) {
      String checkerTargetSql = ApolloSqlConverterUtil.replacePath(srcSql, srcDir,
          checkerTargetDir);
      String repositoryTargetSql = ApolloSqlConverterUtil.replacePath(srcSql, srcDir,
          repositoryTargetDir);

      this.doCheck(checkerTargetSql, repositoryTargetSql);
    }
  }

  private Set<String> getIgnoreDirs(String repositoryTargetDir) {
    Set<String> ignoreDirs = new LinkedHashSet<>();
    ignoreDirs.add(repositoryTargetDir + "/delta/v040-v050");
    ignoreDirs.add(repositoryTargetDir + "/delta/v060-v062");
    ignoreDirs.add(repositoryTargetDir + "/delta/v080-v090");
    ignoreDirs.add(repositoryTargetDir + "/delta/v151-v160");
    ignoreDirs.add(repositoryTargetDir + "/delta/v170-v180");
    ignoreDirs.add(repositoryTargetDir + "/delta/v180-v190");
    ignoreDirs.add(repositoryTargetDir + "/delta/v190-v200");
    ignoreDirs.add(repositoryTargetDir + "/delta/v200-v210");
    ignoreDirs.add(repositoryTargetDir + "/delta/v210-v220");
    return ignoreDirs;
  }

  private List<String> findRedundantSqlList(String checkerTargetDir, List<String> checkerSqlList,
      String repositoryTargetDir, List<String> repositorySqlList) {
    // repository - checker
    Map<String, String> missingSqlMap = this.findMissing(
        repositoryTargetDir, repositorySqlList, checkerTargetDir, checkerSqlList);
    return new ArrayList<>(missingSqlMap.keySet());
  }

  private List<String> findMissingSqlList(String checkerTargetDir, List<String> checkerSqlList,
      String repositoryTargetDir, List<String> repositorySqlList) {
    // checker - repository
    Map<String, String> missingSqlMap = this.findMissing(checkerTargetDir, checkerSqlList,
        repositoryTargetDir, repositorySqlList);
    return new ArrayList<>(missingSqlMap.values());
  }

  private Map<String, String> findMissing(String sourceDir, List<String> sourceSqlList,
      String targetDir, List<String> targetSqlList) {
    Map<String, String> missingSqlList = new LinkedHashMap<>();
    Set<String> targetSqlSet = new LinkedHashSet<>(targetSqlList);
    for (String sourceSql : sourceSqlList) {
      String targetSql = ApolloSqlConverterUtil.replacePath(sourceSql, sourceDir, targetDir);
      if (!targetSqlSet.contains(targetSql)) {
        missingSqlList.put(sourceSql, targetSql);
      }
    }
    return missingSqlList;
  }

  private void doCheck(String checkerTargetSql, String repositoryTargetSql) {
    List<String> checkerLines = new ArrayList<>();
    try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(checkerTargetSql),
        StandardCharsets.UTF_8)) {
      for (String line = bufferedReader.readLine(); line != null;
          line = bufferedReader.readLine()) {
        checkerLines.add(line);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    List<String> repositoryLines = new ArrayList<>();
    try (BufferedReader bufferedReader = Files.newBufferedReader(Paths.get(repositoryTargetSql),
        StandardCharsets.UTF_8)) {
      for (String line = bufferedReader.readLine(); line != null;
          line = bufferedReader.readLine()) {
        repositoryLines.add(line);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    Assertions.assertEquals(checkerLines.size(), repositoryLines.size(),
        "invalid sql files, please run 'mvn compile -Psql-convert' to regenerated\npath: "
            + repositoryTargetSql);
    for (int i = 0; i < checkerLines.size(); i++) {
      String checkerLine = checkerLines.get(i);
      String repositoryLine = repositoryLines.get(i);
      int lineNumber = i + 1;
      Assertions.assertEquals(checkerLine, repositoryLine,
          "invalid sql file content, please run 'mvn compile -Psql-convert' to regenerated\npath: "
              + repositoryTargetSql + "(line: " + lineNumber + ")");
    }
  }

  private void checkAssemblyMysqlList(List<String> srcSqlList, String srcDir,
      String checkerParentDir, String repositoryParentDir) {
    String checkerTargetDir = checkerParentDir + "/sql/assembly/mysql";
    String repositoryTargetDir = repositoryParentDir + "/sql/assembly/mysql";
    for (String srcSql : srcSqlList) {
      String checkerTargetSql = ApolloSqlConverterUtil.replacePath(srcSql, srcDir,
          checkerTargetDir);
      String repositoryTargetSql = ApolloSqlConverterUtil.replacePath(srcSql, srcDir,
          repositoryTargetDir);

      this.doCheck(checkerTargetSql, repositoryTargetSql);
    }
  }

  private void checkAssemblyH2List(List<String> srcSqlList, String srcDir,
      String checkerParentDir, String repositoryParentDir) {
    String checkerTargetDir = checkerParentDir + "/sql/assembly/h2";
    String repositoryTargetDir = repositoryParentDir + "/sql/assembly/h2";
    for (String srcSql : srcSqlList) {
      String checkerTargetSql = ApolloSqlConverterUtil.replacePath(srcSql, srcDir,
          checkerTargetDir);
      String repositoryTargetSql = ApolloSqlConverterUtil.replacePath(srcSql, srcDir,
          repositoryTargetDir);

      this.doCheck(checkerTargetSql, repositoryTargetSql);
    }
  }

}