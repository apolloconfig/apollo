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
package com.ctrip.framework.apollo.assembly.datasource;

import com.ctrip.framework.apollo.assembly.ApolloApplication;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.sql.init.SqlDataSourceScriptDatabaseInitializer;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationProperties;
import org.springframework.boot.jdbc.DatabaseDriver;
import org.springframework.boot.jdbc.init.PlatformPlaceholderDatabaseDriverResolver;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.MetaDataAccessException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

public class ApolloAssemblyDataSourceScriptDatabaseInitializer extends
    SqlDataSourceScriptDatabaseInitializer {

  public ApolloAssemblyDataSourceScriptDatabaseInitializer(DataSource dataSource,
      SqlInitializationProperties properties) {
    super(dataSource, getSettings(dataSource, properties));
  }

  public static DatabaseInitializationSettings getSettings(DataSource dataSource,
      SqlInitializationProperties properties) {

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
      SqlInitializationProperties properties) {

    if (CollectionUtils.isEmpty(locations)) {
      return null;
    }

    Collection<String> convertedLocations = convertRepositoryLocations(locations);
    if (CollectionUtils.isEmpty(convertedLocations)) {
      return null;
    }

    String platform = properties.getPlatform();
    if (StringUtils.hasText(platform) && !"all".equals(platform)) {
      return platformResolver.resolveAll(platform, convertedLocations.toArray(new String[0]));
    }
    return platformResolver.resolveAll(dataSource, convertedLocations.toArray(new String[0]));
  }

  private static Collection<String> convertRepositoryLocations(Collection<String> locations) {
    if (CollectionUtils.isEmpty(locations)) {
      return null;
    }
    String repositoryDir = findRepositoryDirectory();
    List<String> convertedLocations = new ArrayList<>(locations.size());
    for (String location : locations) {
      String convertedLocation = convertRepositoryLocation(location, repositoryDir);
      if (StringUtils.hasText(convertedLocation)) {
        convertedLocations.add(convertedLocation);
      }
    }
    return convertedLocations;
  }

  private static String findRepositoryDirectory() {
    CodeSource codeSource = ApolloApplication.class.getProtectionDomain().getCodeSource();
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
      return locationText.replace("/apollo-assembly/target/classes/", "/scripts/sql");
    }
    return null;
  }

  private static String convertRepositoryLocation(String location, String repositoryDir) {
    if (!StringUtils.hasText(location) || !location.contains("@@repository@@")) {
      return location;
    }
    if (!StringUtils.hasText(repositoryDir)) {
      // repository dir not found
      return null;
    }
    return location.replace("@@repository@@", repositoryDir);
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

  @Override
  protected void customize(ResourceDatabasePopulator populator) {
    DataSource dataSource = this.getDataSource();
    DatabaseDriver databaseDriver = DatabaseDriver.fromDataSource(dataSource);
    if (DatabaseDriver.MYSQL.equals(databaseDriver)) {
      JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
      String database = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
      if (database != null) {
        populator.setScripts();
      }
      System.out.println(database);
    }
  }
}
