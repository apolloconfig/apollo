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
package com.ctrip.framework.apollo.audit.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.audit.entity.ApolloAuditLog;
import com.ctrip.framework.apollo.audit.repository.ApolloAuditLogRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verifies audit date filtering and pagination against a real JPA repository.
 */
@SpringJUnitConfig(ApolloAuditLogServiceTest.TestConfiguration.class)
@Transactional
class ApolloAuditLogServiceTest {

  private static final String OP_NAME = "App.create";

  @Autowired
  private ApolloAuditLogRepository repository;

  @Autowired
  private ApolloAuditLogService service;

  @BeforeEach
  void setUp() {
    for (int minute = 0; minute < 6; minute++) {
      repository.save(log(OP_NAME, minute));
    }
    repository.save(log("App.update", 3));
    repository.flush();
  }

  @ParameterizedTest
  @MethodSource("dateRanges")
  void filtersInclusiveDateBoundsAndPreservesThemAcrossPages(Date startDate, Date endDate,
      List<String> expected) {
    assertThat(service.findByOpNameAndTime(OP_NAME, startDate, endDate, 0, 10))
        .extracting(ApolloAuditLog::getSpanId).containsExactlyElementsOf(expected);

    List<ApolloAuditLog> pages = new ArrayList<>();
    for (int page = 0; page < 3; page++) {
      pages.addAll(service.findByOpNameAndTime(OP_NAME, startDate, endDate, page, 2));
    }
    assertThat(pages).extracting(ApolloAuditLog::getSpanId).containsExactlyElementsOf(expected);
    assertThat(service.findByOpNameAndTime(OP_NAME, startDate, endDate, 3, 2)).isEmpty();
  }

  static Stream<Arguments> dateRanges() {
    return Stream.of(Arguments.of(date(2), date(4), List.of("minute-4", "minute-3", "minute-2")),
        Arguments.of(date(2), null, List.of("minute-5", "minute-4", "minute-3", "minute-2")),
        Arguments.of(null, date(4),
            List.of("minute-4", "minute-3", "minute-2", "minute-1", "minute-0")),
        Arguments.of(null, null,
            List.of("minute-5", "minute-4", "minute-3", "minute-2", "minute-1", "minute-0")));
  }

  @Test
  void returnsNoLogsWhenTheOperationOrDatesDoNotMatch() {
    assertThat(service.findByOpNameAndTime("App.missing", date(2), null, 0, 10)).isEmpty();
    assertThat(service.findByOpNameAndTime(OP_NAME, date(6), null, 0, 10)).isEmpty();
    assertThat(service.findByOpNameAndTime(OP_NAME, null, date(-1), 0, 10)).isEmpty();
  }

  private static ApolloAuditLog log(String opName, int minute) {
    return ApolloAuditLog.builder().traceId("test-trace").spanId("minute-" + minute).opName(opName)
        .happenedTime(date(minute)).build();
  }

  private static Date date(int minute) {
    return Date.from(Instant.parse("2026-09-27T13:30:00Z").plusSeconds(minute * 60L));
  }

  @Configuration(proxyBeanMethods = false)
  @EnableJpaRepositories(basePackageClasses = ApolloAuditLogRepository.class)
  @Import(ApolloAuditLogService.class)
  static class TestConfiguration {

    @Bean
    DataSource dataSource() {
      return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2)
          .build();
    }

    @Bean
    LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
      LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
      factory.setDataSource(dataSource);
      factory.setPackagesToScan(ApolloAuditLog.class.getPackage().getName());
      factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
      factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
      return factory;
    }

    @Bean
    JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
      return new JpaTransactionManager(entityManagerFactory);
    }
  }
}
