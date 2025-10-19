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
package com.ctrip.framework.apollo.portal;

import com.ctrip.framework.apollo.common.datasource.ApolloDataSourceScriptDatabaseInitializer;
import com.ctrip.framework.apollo.common.datasource.ApolloDataSourceScriptDatabaseInitializerFactory;
import com.ctrip.framework.apollo.common.datasource.ApolloSqlInitializationProperties;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("assembly")
@Configuration
public class PortalAssemblyConfiguration {

  @Primary
  @ConfigurationProperties(prefix = "spring.portal-datasource")
  @Bean
  public static DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @ConfigurationProperties(prefix = "spring.sql.portal-init")
  @Bean
  public static ApolloSqlInitializationProperties apolloSqlInitializationProperties() {
    return new ApolloSqlInitializationProperties();
  }

  @Bean
  public static ApolloDataSourceScriptDatabaseInitializer apolloDataSourceScriptDatabaseInitializer(
      DataSource dataSource, ApolloSqlInitializationProperties properties) {
    return ApolloDataSourceScriptDatabaseInitializerFactory.create(dataSource, properties);
  }
}
