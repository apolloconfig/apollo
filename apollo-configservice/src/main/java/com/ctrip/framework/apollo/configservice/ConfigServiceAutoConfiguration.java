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
package com.ctrip.framework.apollo.configservice;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.grayReleaseRule.GrayReleaseRulesHolder;
import com.ctrip.framework.apollo.biz.message.ReleaseMessageScanner;
import com.ctrip.framework.apollo.biz.repository.GrayReleaseRuleRepository;
import com.ctrip.framework.apollo.biz.repository.ReleaseMessageRepository;
import com.ctrip.framework.apollo.biz.service.ReleaseMessageService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.configservice.controller.ConfigFileController;
import com.ctrip.framework.apollo.configservice.controller.NotificationController;
import com.ctrip.framework.apollo.configservice.controller.NotificationControllerV2;
import com.ctrip.framework.apollo.configservice.filter.ClientAuthenticationFilter;
import com.ctrip.framework.apollo.configservice.service.ReleaseMessageServiceWithCache;
import com.ctrip.framework.apollo.configservice.service.config.ConfigService;
import com.ctrip.framework.apollo.configservice.service.config.ConfigServiceWithCache;
import com.ctrip.framework.apollo.configservice.service.config.DefaultConfigService;
import com.ctrip.framework.apollo.configservice.util.AccessKeyUtil;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Configuration
public class ConfigServiceAutoConfiguration {

  private final BizConfig bizConfig;
  private final ReleaseService releaseService;
  private final ReleaseMessageService releaseMessageService;
  private final GrayReleaseRuleRepository grayReleaseRuleRepository;
  private final MeterRegistry meterRegistry;

  public ConfigServiceAutoConfiguration(final BizConfig bizConfig,
                                        final ReleaseService releaseService,
                                        final ReleaseMessageService releaseMessageService,
                                        final GrayReleaseRuleRepository grayReleaseRuleRepository,
                                        final MeterRegistry meterRegistry) {
    this.bizConfig = bizConfig;
    this.releaseService = releaseService;
    this.releaseMessageService = releaseMessageService;
    this.grayReleaseRuleRepository = grayReleaseRuleRepository;
    this.meterRegistry = meterRegistry;
  }

  @Bean
  public GrayReleaseRulesHolder grayReleaseRulesHolder() {
    return new GrayReleaseRulesHolder(grayReleaseRuleRepository, bizConfig);
  }

  @Bean
  public ConfigService configService() {
    if (bizConfig.isConfigServiceCacheEnabled()) {
      return new ConfigServiceWithCache(releaseService, releaseMessageService,
          grayReleaseRulesHolder(), bizConfig, meterRegistry);
    }
    return new DefaultConfigService(releaseService, grayReleaseRulesHolder());
  }

  @Bean
  public static NoOpPasswordEncoder passwordEncoder() {
    return (NoOpPasswordEncoder) NoOpPasswordEncoder.getInstance();
  }

  @Bean
  public FilterRegistrationBean<ClientAuthenticationFilter> clientAuthenticationFilter(AccessKeyUtil accessKeyUtil) {
    FilterRegistrationBean<ClientAuthenticationFilter> filterRegistrationBean = new FilterRegistrationBean<>();

    filterRegistrationBean.setFilter(new ClientAuthenticationFilter(bizConfig, accessKeyUtil));
    filterRegistrationBean.addUrlPatterns("/configs/*");
    filterRegistrationBean.addUrlPatterns("/configfiles/*");
    filterRegistrationBean.addUrlPatterns("/notifications/v2/*");

    return filterRegistrationBean;
  }

  @Configuration
  static class MessageScannerConfiguration {
    private final NotificationController notificationController;
    private final ConfigFileController configFileController;
    private final NotificationControllerV2 notificationControllerV2;
    private final GrayReleaseRulesHolder grayReleaseRulesHolder;
    private final ReleaseMessageServiceWithCache releaseMessageServiceWithCache;
    private final ConfigService configService;
    private final BizConfig bizConfig;
    private final ReleaseMessageRepository releaseMessageRepository;

    public MessageScannerConfiguration(
        final NotificationController notificationController,
        final ConfigFileController configFileController,
        final NotificationControllerV2 notificationControllerV2,
        final GrayReleaseRulesHolder grayReleaseRulesHolder,
        final ReleaseMessageServiceWithCache releaseMessageServiceWithCache,
        final ConfigService configService,
        final BizConfig bizConfig,
        final ReleaseMessageRepository releaseMessageRepository) {
      this.notificationController = notificationController;
      this.configFileController = configFileController;
      this.notificationControllerV2 = notificationControllerV2;
      this.grayReleaseRulesHolder = grayReleaseRulesHolder;
      this.releaseMessageServiceWithCache = releaseMessageServiceWithCache;
      this.configService = configService;
      this.bizConfig = bizConfig;
      this.releaseMessageRepository = releaseMessageRepository;
    }

    @Bean
    public ReleaseMessageScanner releaseMessageScanner() {
      ReleaseMessageScanner releaseMessageScanner = new ReleaseMessageScanner(bizConfig,
          releaseMessageRepository);
      //0. handle release message cache
      releaseMessageScanner.addMessageListener(releaseMessageServiceWithCache);
      //1. handle gray release rule
      releaseMessageScanner.addMessageListener(grayReleaseRulesHolder);
      //2. handle server cache
      releaseMessageScanner.addMessageListener(configService);
      releaseMessageScanner.addMessageListener(configFileController);
      //3. notify clients
      releaseMessageScanner.addMessageListener(notificationControllerV2);
      releaseMessageScanner.addMessageListener(notificationController);
      return releaseMessageScanner;
    }
  }

}
