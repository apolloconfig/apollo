package com.ctrip.framework.apollo.portal;

import com.ctrip.framework.apollo.common.ApolloCommonConfig;
import com.ctrip.framework.apollo.openapi.PortalOpenApiConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ldap.LdapAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableAspectJAutoProxy
@Configuration
@EnableAutoConfiguration(exclude = {LdapAutoConfiguration.class})
@EnableTransactionManagement
@ComponentScan(basePackageClasses = {ApolloCommonConfig.class,
    PortalApplication.class, PortalOpenApiConfig.class})
public class PortalApplication {

  public static void main(String[] args) throws Exception {
    SpringApplication.run(PortalApplication.class, args);
  }
}
