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
package com.ctrip.framework.apollo.common.datasource;

import java.util.List;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.jdbc.init.DataSourceScriptDatabaseInitializer;
import org.springframework.boot.sql.init.DatabaseInitializationMode;
import org.springframework.boot.sql.init.DatabaseInitializationSettings;
import org.springframework.jdbc.datasource.AbstractDriverBasedDataSource;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

public class ApolloDataSourceScriptDatabaseInitializer extends DataSourceScriptDatabaseInitializer {

  private static final Logger log =
      LoggerFactory.getLogger(ApolloDataSourceScriptDatabaseInitializer.class);

  public ApolloDataSourceScriptDatabaseInitializer(DataSource dataSource,
      DatabaseInitializationSettings settings) {
    super(dataSource, settings);
    if (this.isEnabled(settings)) {
      log.info("Apollo DataSource Initialize is enabled");
      if (log.isDebugEnabled()) {
        String jdbcUrl = this.getJdbcUrl(dataSource);
        log.debug("Initialize jdbc url: {}", jdbcUrl);
        List<String> schemaLocations = settings.getSchemaLocations();
        if (!schemaLocations.isEmpty()) {
          for (String schemaLocation : schemaLocations) {
            log.debug("Initialize Schema Location: {}", schemaLocation);
          }
        }
      }
    } else {
      log.info("Apollo DataSource Initialize is disabled");
    }
  }

  private String getJdbcUrl(DataSource dataSource) {
    if (dataSource instanceof AbstractDriverBasedDataSource) {
      AbstractDriverBasedDataSource driverBasedDataSource =
          (AbstractDriverBasedDataSource) dataSource;
      return driverBasedDataSource.getUrl();
    }
    SimpleDriverDataSource simpleDriverDataSource =
        DataSourceBuilder.derivedFrom(dataSource).type(SimpleDriverDataSource.class).build();
    return simpleDriverDataSource.getUrl();
  }

  private boolean isEnabled(DatabaseInitializationSettings settings) {
    if (settings.getMode() == DatabaseInitializationMode.NEVER) {
      return false;
    }
    return settings.getMode() == DatabaseInitializationMode.ALWAYS || this.isEmbeddedDatabase();
  }
}
