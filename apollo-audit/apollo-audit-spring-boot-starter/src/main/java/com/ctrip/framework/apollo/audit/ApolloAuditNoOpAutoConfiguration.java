/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.audit;

import com.ctrip.framework.apollo.audit.api.ApolloAuditLogApi;
import com.ctrip.framework.apollo.audit.component.ApolloAuditHttpInterceptor;
import com.ctrip.framework.apollo.audit.component.ApolloAuditLogApiNoOpImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "apollo.audit.log", name = "enabled", havingValue = "false", matchIfMissing = true)
public class ApolloAuditNoOpAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(ApolloAuditLogApi.class)
  public ApolloAuditLogApi apolloAuditLogApi() {
    return new ApolloAuditLogApiNoOpImpl();
  }

  @Bean
  @ConditionalOnMissingBean(ApolloAuditHttpInterceptor.class)
  public ApolloAuditHttpInterceptor apolloAuditLogHttpTracerInterceptor(ApolloAuditLogApi api) {
    return new ApolloAuditHttpInterceptor(api);
  }

}
