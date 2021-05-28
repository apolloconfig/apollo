/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.demo.spring.springBootDemo.refresh;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.demo.spring.springBootDemo.config.SampleRedisConfig;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.context.scope.refresh.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@ConditionalOnProperty("redis.cache.enabled")
@Component
public class SpringBootApolloRefreshConfig {
  private static final Logger logger = LoggerFactory.getLogger(SpringBootApolloRefreshConfig.class);

  private final SampleRedisConfig sampleRedisConfig;
  private final RefreshScope refreshScope;

  public SpringBootApolloRefreshConfig(
      final SampleRedisConfig sampleRedisConfig,
      final RefreshScope refreshScope) {
    this.sampleRedisConfig = sampleRedisConfig;
    this.refreshScope = refreshScope;
  }

  @ApolloConfigChangeListener(value = {ConfigConsts.NAMESPACE_APPLICATION, "TEST1.apollo", "application.yaml"},
      interestedKeyPrefixes = {"redis.cache."})
  public void onChange(ConfigChangeEvent changeEvent) {
    logger.info("before refresh {}", sampleRedisConfig.toString());
    refreshScope.refresh("sampleRedisConfig");
    logger.info("after refresh {}", sampleRedisConfig.toString());
  }
}
