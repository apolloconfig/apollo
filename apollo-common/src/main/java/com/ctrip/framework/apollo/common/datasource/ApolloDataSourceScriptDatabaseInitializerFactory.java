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
package com.ctrip.framework.apollo.common.datasource;

import java.net.URL;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.PlatformPlaceholderDatabaseDriverResolver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class ApolloDataSourceScriptDatabaseInitializerFactory {

  public static ApolloDataSourceScriptDatabaseInitializer create(DataSource dataSource,
      ApolloSqlInitializationProperties properties) {
    DataSource determinedDataSource = determineDataSource(dataSource, properties);
    DatabaseInitializationSettings settings = getSettings(dataSource, properties);
    return new ApolloDataSourceScriptDatabaseInitializer(determinedDataSource, settings);
  }

  private static DataSource determineDataSource(DataSource dataSource,
      ApolloSqlInitializationProperties properties) {

    String username = properties.getUsername();
    String password = properties.getPassword();
    if (StringUtils.hasText(username) && StringUtils.hasText(password)) {
      return DataSourceBuilder.derivedFrom(dataSource)
          .username(username)
          .password(password)
          .type(SimpleDriverDataSource.class)
          .build();
    }
    return dataSource;
  }

  private static DatabaseInitializationSettings getSettings(DataSource dataSource,
      ApolloSqlInitializationProperties properties) {

    PlatformPlaceholderDatabaseDriverResolver platformResolver = new PlatformPlaceholderDatabaseDriverResolver().withDriverPlatform(
        DatabaseDriver.MARIADB, "mysql");

    List<String> schemaLocations = resolveLocations(properties.getSchemaLocations(),
        platformResolver,
        dataSource, properties);
    List<String> dataLocations = resolveLocations(properties.getDataLocations(), platformResolver,
        dataSource, properties);

    DatabaseInitializationSettings settings = new DatabaseInitializationSettings();
    settings.setSchemaLocations(
        scriptLocations(schemaLocations, "schema", properties.getPlatform()));
    settings.setDataLocations(scriptLocations(dataLocations, "data", properties.getPlatform()));
    settings.setContinueOnError(properties.isContinueOnError());
    settings.setSeparator(properties.getSeparator());
    settings.setEncoding(properties.getEncoding());
    settings.setMode(properties.getMode());
    return settings;
  }


  private static List<String> resolveLocations(Collection<String> locations,
      PlatformPlaceholderDatabaseDriverResolver platformResolver, DataSource dataSource,
      ApolloSqlInitializationProperties properties) {

    if (CollectionUtils.isEmpty(locations)) {
      return null;
    }

    Collection<String> convertedLocations = convertRepositoryLocations(locations, dataSource);
    if (CollectionUtils.isEmpty(convertedLocations)) {
      return null;
    }

    String platform = properties.getPlatform();
    if (StringUtils.hasText(platform) && !"all".equals(platform)) {
      return platformResolver.resolveAll(platform, convertedLocations.toArray(new String[0]));
    }
    return platformResolver.resolveAll(dataSource, convertedLocations.toArray(new String[0]));
  }

  private static Collection<String> convertRepositoryLocations(Collection<String> locations,
      DataSource dataSource) {
    if (CollectionUtils.isEmpty(locations)) {
      return Collections.emptyList();
    }
    String repositoryDir = findRepositoryDirectory();
    String suffix = findSuffix(dataSource);
    List<String> convertedLocations = new ArrayList<>(locations.size());
    for (String location : locations) {
      String convertedLocation = convertRepositoryLocation(location, repositoryDir, suffix);
      if (StringUtils.hasText(convertedLocation)) {
        convertedLocations.add(convertedLocation);
      }
    }
    return convertedLocations;
  }

  private static String findSuffix(DataSource dataSource) {
    DatabaseDriver databaseDriver = DatabaseDriver.fromDataSource(dataSource);
    if (DatabaseDriver.H2.equals(databaseDriver)) {
      return "-default";
    }
    if (DatabaseDriver.MYSQL.equals(databaseDriver)) {
      return "-database-not-specified";
    }
    return "";
  }

  private static String findRepositoryDirectory() {
    CodeSource codeSource = ApolloDataSourceScriptDatabaseInitializer.class.getProtectionDomain()
        .getCodeSource();
    URL location = codeSource != null ? codeSource.getLocation() : null;
    if (location == null) {
      return null;
    }
    if ("jar".equals(location.getProtocol())) {
      // running with jar
      return "classpath:META-INF/sql";
    }
    if ("file".equals(location.getProtocol())) {
      // running with ide
      String locationText = location.toString();
      if (!locationText.endsWith("/apollo-common/target/classes/")) {
        throw new IllegalStateException(
            "can not determine repository directory from classpath: " + locationText);
      }
      return locationText.replace("/apollo-common/target/classes/", "/scripts/sql");
    }
    return null;
  }

  private static String convertRepositoryLocation(String location, String repositoryDir,
      String suffix) {
    if (!StringUtils.hasText(location)) {
      return location;
    }
    if (!StringUtils.hasText(repositoryDir)) {
      // repository dir not found
      return null;
    }
    return location.replace("@@repository@@", repositoryDir).replace("@@suffix@@", suffix);
  }

  private static List<String> scriptLocations(List<String> locations, String fallback,
      String platform) {
    if (locations != null) {
      return locations;
    }
    List<String> fallbackLocations = new ArrayList<>();
    fallbackLocations.add("optional:classpath*:" + fallback + "-" + platform + ".sql");
    fallbackLocations.add("optional:classpath*:" + fallback + ".sql");
    return fallbackLocations;
  }
}
