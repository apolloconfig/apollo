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
package com.ctrip.framework.apollo.configservice;

import static org.assertj.core.api.Assertions.assertThat;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.repository.GrayReleaseRuleRepository;
import com.ctrip.framework.apollo.biz.service.ReleaseMessageService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.configservice.filter.ClientAuthenticationFilter;
import com.ctrip.framework.apollo.configservice.util.AccessKeyUtil;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

@RunWith(MockitoJUnitRunner.class)
public class ConfigServiceAutoConfigurationTest {

  private ConfigServiceAutoConfiguration configServiceAutoConfiguration;

  @Mock
  private BizConfig bizConfig;
  @Mock
  private ReleaseService releaseService;
  @Mock
  private ReleaseMessageService releaseMessageService;
  @Mock
  private GrayReleaseRuleRepository grayReleaseRuleRepository;
  @Mock
  private MeterRegistry meterRegistry;
  @Mock
  private AccessKeyUtil accessKeyUtil;

  @Before
  public void setUp() {
    configServiceAutoConfiguration = new ConfigServiceAutoConfiguration(bizConfig, releaseService,
        releaseMessageService, grayReleaseRuleRepository, meterRegistry);
  }

  @Test
  public void testClientAuthenticationFilterShouldCoverNotificationV2ExactPath() {
    FilterRegistrationBean<ClientAuthenticationFilter> filterRegistrationBean =
        configServiceAutoConfiguration.clientAuthenticationFilter(accessKeyUtil);

    assertThat(filterRegistrationBean.getUrlPatterns()).containsAll(
        Arrays.asList("/configs/*", "/configfiles/*", "/notifications/v2", "/notifications/v2/*"));
  }
}
