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
package com.ctrip.framework.apollo.assembly;

import com.ctrip.framework.apollo.adminservice.AdminServiceApplication;
import com.ctrip.framework.apollo.audit.configuration.ApolloAuditAutoConfiguration;
import com.ctrip.framework.apollo.configservice.ConfigServiceApplication;
import com.ctrip.framework.apollo.portal.PortalApplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
    ApolloAuditAutoConfiguration.class,})
public class ApolloApplication {

  private static final Logger logger = LoggerFactory.getLogger(ApolloApplication.class);

  public static void main(String[] args) throws Exception {
    /**
     * Common
     */
    MDC.put("starting_context", "[starting:common] ");
    logger.info("commonContext starting...");
    ConfigurableApplicationContext commonContext =
        new SpringApplicationBuilder(ApolloApplication.class).web(WebApplicationType.NONE)
            .run(args);
    logger.info("commonContext [{}] isActive: {}", commonContext.getId(), commonContext.isActive());

    /**
     * ConfigService
     */
    MDC.put("starting_context", "[starting:config] ");
    logger.info("configContext starting...");
    ConfigurableApplicationContext configContext =
        new SpringApplicationBuilder(ConfigServiceApplication.class).parent(commonContext)
            .profiles("assembly").sources(RefreshScope.class).run(args);
    logger.info("configContext [{}] isActive: {}", configContext.getId(), configContext.isActive());

    /**
     * AdminService
     */
    MDC.put("starting_context", "[starting:admin] ");
    logger.info("adminContext starting...");
    ConfigurableApplicationContext adminContext =
        new SpringApplicationBuilder(AdminServiceApplication.class).parent(commonContext)
            .profiles("assembly").sources(RefreshScope.class).run(args);
    logger.info("adminContext [{}] isActive: {}", adminContext.getId(), adminContext.isActive());

    /**
     * Portal
     */
    MDC.put("starting_context", "[starting:portal] ");
    logger.info("portalContext starting...");
    ConfigurableApplicationContext portalContext =
        new SpringApplicationBuilder(PortalApplication.class).parent(commonContext)
            .profiles("assembly").sources(RefreshScope.class).run(args);
    logger.info("portalContext [{}] isActive: {}", portalContext.getId(), portalContext.isActive());

    MDC.clear();
  }

}
