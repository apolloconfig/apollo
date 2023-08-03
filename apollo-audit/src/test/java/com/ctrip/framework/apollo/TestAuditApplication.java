package com.ctrip.framework.apollo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootTest
@EnableAspectJAutoProxy
@Configuration
@SpringBootApplication
public class TestAuditApplication {

  public static void main(String[] args) {
    ConfigurableApplicationContext cac = SpringApplication.run(TestAuditApplication.class);
  }

}
