package com.ctrip.framework.apollo.portal.audit;

import com.ctrip.framework.apollo.audit.spi.ApolloAuditSpanService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApolloAuditConfiguration {

//  @Bean
//  public ApolloAuditLogService apolloAuditLogService(AuditLogRepository auditLogRepository,
//      AuditLogDataInfluenceRepository auditLogDataInfluenceRepository) {
//    return new PortalAuditLogService(auditLogRepository, auditLogDataInfluenceRepository);
//  }

  @Bean
  public ApolloAuditSpanService apolloAuditSpanService(UserInfoHolder userInfoHolder) {
    return new PortalAuditSpanService(userInfoHolder);
  }

}
