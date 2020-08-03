package com.ctrip.framework.apollo.adminservice;

import com.ctrip.framework.apollo.biz.ApolloBizConfig;
import com.ctrip.framework.apollo.common.ApolloCommonConfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
@EnableAspectJAutoProxy
@Configuration
@PropertySource(value = {"classpath:adminservice.properties"})
@EnableAutoConfiguration
@EnableTransactionManagement
@ComponentScan(basePackageClasses = {ApolloCommonConfig.class,
    ApolloBizConfig.class,
    AdminServiceApplication.class})
public class AdminServiceApplication {
  public static void main(String[] args) {
    SpringApplication.run(AdminServiceApplication.class, args);
  }
}
