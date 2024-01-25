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
package com.ctrip.framework.apollo.configservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.configservice.service.AccessKeyServiceWithCache;
import com.google.common.collect.Lists;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * @author nisiyong
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessKeyUtilTest {

  private AccessKeyUtil accessKeyUtil;

  @Mock
  private AccessKeyServiceWithCache accessKeyServiceWithCache;
  @Mock
  private HttpServletRequest request;

  @Before
  public void setUp() {
    accessKeyUtil = new AccessKeyUtil(accessKeyServiceWithCache);
  }

  @Test
  public void testFindAvailableSecret() {
    String appId = "someAppId";
    List<String> returnSecrets = Lists.newArrayList("someSecret");

    when(accessKeyServiceWithCache.getAvailableSecrets(appId)).thenReturn(returnSecrets);

    List<String> availableSecret = accessKeyUtil.findAvailableSecret(appId);

    assertThat(availableSecret).containsExactly("someSecret");
    verify(accessKeyServiceWithCache).getAvailableSecrets(appId);
  }

  @Test
  public void testExtractAppIdFromRequest1() {
    when(request.getServletPath()).thenReturn("/configs/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromRequest2() {
    when(request.getServletPath()).thenReturn("/configfiles/json/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromRequest3() {
    when(request.getServletPath()).thenReturn("/configfiles/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromRequest4() {
    when(request.getServletPath()).thenReturn("/notifications/v2");
    when(request.getParameter("appId")).thenReturn("someAppId");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void buildSignature() {
    String path = "/configs/someAppId/default/application";
    String query = "ip=10.0.0.1";
    String timestamp = "1575018989200";
    String secret = "someSecret";

    String actualSignature = accessKeyUtil.buildSignature(path, query, timestamp, secret);

    String expectedSignature = "WYjjyJFei6DYiaMlwZjew2O/Yqk=";
    assertThat(actualSignature).isEqualTo(expectedSignature);
  }
}
