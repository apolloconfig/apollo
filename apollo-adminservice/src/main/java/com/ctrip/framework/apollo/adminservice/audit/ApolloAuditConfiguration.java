package com.ctrip.framework.apollo.adminservice.audit;

import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApolloAuditConfiguration {

  @Bean
  public ApolloAuditSpanService apolloAuditSpanService() {
    return new BizAuditSpanService();
  }

}
