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
package com.ctrip.framework.apollo.common.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * @author kl (http://kailing.pub)
 * @since 2022/8/12
 */
@RunWith(MockitoJUnitRunner.class)
public class WebUtilsTest {

  @Test
  public void testTryToGetClientIp() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("X-FORWARDED-FOR", "172.1.1.1,172.1.1.2");
    request.setRemoteAddr("172.1.1.3");
    String ip = WebUtils.tryToGetClientIp(request);
    assertThat(ip).isEqualTo("172.1.1.1");

    request.removeHeader("X-FORWARDED-FOR");
    ip = WebUtils.tryToGetClientIp(request);
    assertThat(ip).isEqualTo("172.1.1.3");
  }
}
