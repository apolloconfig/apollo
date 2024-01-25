/*
 * Copyright 2024 Apollo Authors
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
package com.ctrip.framework.apollo.biz.eureka;


import com.ctrip.framework.apollo.biz.config.BizConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Component
@Primary
@ConditionalOnProperty(value = {"eureka.client.enabled"}, havingValue = "true", matchIfMissing = true)
public class ApolloEurekaClientConfig extends EurekaClientConfigBean {

  private final BizConfig bizConfig;
  private final RefreshScope refreshScope;
  private static final String EUREKA_CLIENT_BEAN_NAME = "eurekaClient";

  public ApolloEurekaClientConfig(final BizConfig bizConfig, final RefreshScope refreshScope) {
    this.bizConfig = bizConfig;
    this.refreshScope = refreshScope;
  }

  /**
   * Assert only one zone: defaultZone, but multiple environments.
   */
  @Override
  public List<String> getEurekaServerServiceUrls(String myZone) {
    List<String> urls = bizConfig.eurekaServiceUrls();
    return CollectionUtils.isEmpty(urls) ? super.getEurekaServerServiceUrls(myZone) : urls;
  }

  @EventListener
  public void listenApplicationReadyEvent(ApplicationReadyEvent event) {
    this.refreshEurekaClient();
  }

  private void refreshEurekaClient() {
    if (!super.isFetchRegistry()) {
        super.setFetchRegistry(true);
        super.setRegisterWithEureka(true);
        refreshScope.refresh(EUREKA_CLIENT_BEAN_NAME);
    }
  }

  @Override
  public boolean equals(Object o) {
    return super.equals(o);
  }
}
