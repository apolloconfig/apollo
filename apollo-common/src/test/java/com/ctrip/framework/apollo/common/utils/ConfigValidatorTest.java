package com.ctrip.framework.apollo.common.utils;

import com.ctrip.framework.apollo.common.initializer.ConfigValidatorInitializer;
import java.util.Objects;
import org.junit.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * test ConfigValidator
 *
 * @author Lifeng Lv
 */
@Configuration
@EnableConfigurationProperties
public class ConfigValidatorTest {


  /**
   * validate the database name for apollo-adminservice.
   */
  @Test
  public void validateAdminServiceDbNameSuccess() {
    new ApplicationContextRunner()
        .withBean(ConfigValidatorTest.class)
        .withPropertyValues(
            "spring.datasource.url = jdbc:mysql://127.0.0.1:3306/ApolloConfigDB",
            "spring.application.name = apollo-adminservice",
            "apollo.check.database-name = true"
        )
        .withInitializer(new ConfigValidatorInitializer())
        .run(context -> {
          Assert.notNull(context, "Startup failed!");
          if (Objects.nonNull(context.getStartupFailure())) {
            Assert.isNull(context.getStartupFailure(), context.getStartupFailure().getMessage());
          }
        });
  }

  /**
   * validate the database name for apollo-adminservice.
   */
  @Test
  public void validateAdminServiceDbNameFailed() {
    new ApplicationContextRunner()
        .withBean(ConfigValidatorTest.class)
        .withPropertyValues(
            "spring.datasource.url = jdbc:mysql://127.0.0.1:3306/TestDB?a=b",
            "spring.application.name = apollo-adminservice"
        )
        .withInitializer(new ConfigValidatorInitializer())
        .run(context -> {
          Assert.notNull(context, "Startup failed!");
          if (Objects.nonNull(context.getStartupFailure())) {
            Assert.isNull(context.getStartupFailure(), context.getStartupFailure().getMessage());
          }
        });
  }

  /**
   * validate the database name for apollo-configservice.
   */
  @Test
  public void validateConfigServiceDbName() {
    new ApplicationContextRunner()
        .withBean(ConfigValidatorTest.class)
        .withPropertyValues(
            "spring.datasource.url = jdbc:mysql://127.0.0.1:3306/TestDB?a=b",
            "spring.application.name = apollo-configservice"
        )
        .withInitializer(new ConfigValidatorInitializer())
        .run(context -> {
          Assert.notNull(context, "Startup failed!");
          if (Objects.nonNull(context.getStartupFailure())) {
            Assert.isNull(context.getStartupFailure(), context.getStartupFailure().getMessage());
          }
        });
  }

  /**
   * validate the database name for apollo-portal.
   */
  @Test
  public void validatePortalDbName() {
    new ApplicationContextRunner()
        .withBean(ConfigValidatorTest.class)
        .withPropertyValues(
            "spring.datasource.url = /TestDB?",
            "spring.application.name = apollo-portal"
        )
        .withInitializer(new ConfigValidatorInitializer())
        .run(context -> {
          Assert.notNull(context, "Startup failed!");
          if (Objects.nonNull(context.getStartupFailure())) {
            Assert.isNull(context.getStartupFailure(), context.getStartupFailure().getMessage());
          }
        });
  }

}
