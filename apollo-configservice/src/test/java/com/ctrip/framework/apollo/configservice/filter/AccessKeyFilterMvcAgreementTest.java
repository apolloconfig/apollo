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
package com.ctrip.framework.apollo.configservice.filter;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.grayReleaseRule.GrayReleaseRulesHolder;
import com.ctrip.framework.apollo.configservice.controller.ConfigController;
import com.ctrip.framework.apollo.configservice.controller.ConfigFileController;
import com.ctrip.framework.apollo.configservice.service.AccessKeyServiceWithCache;
import com.ctrip.framework.apollo.configservice.util.AccessKeyUtil;
import com.ctrip.framework.apollo.configservice.util.NamespaceUtil;
import com.ctrip.framework.apollo.configservice.util.WatchKeysUtil;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;
import com.ctrip.framework.apollo.core.signature.Signature;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * GHSA-v8jm review: the filter and Spring MVC must agree on the appId of a
 * {@code /configfiles/} request. This test drives requests through the real
 * {@link ClientAuthenticationFilter} and the real MVC routing of
 * {@link ConfigFileController} and asserts both sides use the same appId: the
 * filter looks the AccessKey up for the app MVC binds, and a request that MVC
 * would serve to another app is rejected even when it is unsigned for that app.
 */
@RunWith(MockitoJUnitRunner.class)
public class AccessKeyFilterMvcAgreementTest {

  @Mock
  private BizConfig bizConfig;
  @Mock
  private AccessKeyServiceWithCache accessKeyServiceWithCache;
  @Mock
  private ConfigController configController;
  @Mock
  private NamespaceUtil namespaceUtil;
  @Mock
  private WatchKeysUtil watchKeysUtil;
  @Mock
  private GrayReleaseRulesHolder grayReleaseRulesHolder;

  private AccessKeyUtil accessKeyUtil;
  private MockMvc mockMvc;

  @Before
  public void setUp() throws Exception {
    accessKeyUtil = new AccessKeyUtil(accessKeyServiceWithCache);
    ClientAuthenticationFilter filter = new ClientAuthenticationFilter(bizConfig, accessKeyUtil);
    ConfigFileController controller = new ConfigFileController(configController, namespaceUtil,
        watchKeysUtil, grayReleaseRulesHolder);
    mockMvc = MockMvcBuilders.standaloneSetup(controller).addFilter(filter).build();

    when(bizConfig.accessKeyAuthTimeDiffTolerance()).thenReturn(60);
    when(accessKeyServiceWithCache.getAvailableSecrets("victimApp"))
        .thenReturn(Lists.newArrayList("victimSecret"));
    when(accessKeyServiceWithCache.getAvailableSecrets("json"))
        .thenReturn(Lists.newArrayList("jsonSecret"));
    when(accessKeyServiceWithCache.getAvailableSecrets("raw"))
        .thenReturn(Lists.newArrayList("rawSecret"));

    when(namespaceUtil.filterNamespaceName(any()))
        .thenAnswer((Answer<String>) invocation -> invocation.getArgument(0));
    when(namespaceUtil.normalizeNamespace(any(), any()))
        .thenAnswer((Answer<String>) invocation -> invocation.getArgument(1));
    when(grayReleaseRulesHolder.hasGrayReleaseRule(any(), any(), any(), any())).thenReturn(false);
    when(watchKeysUtil.assembleAllWatchKeys(anyString(), anyString(), anyString(),
        nullable(String.class))).thenReturn(Collections.emptySet());

    ApolloConfig apolloConfig = mock(ApolloConfig.class);
    when(apolloConfig.getConfigurations()).thenReturn(ImmutableMap.of("k1", "v1"));
    when(configController.queryConfig(any(), any(), any(), any(), any(), any(), any(), isNull(),
        any(HttpServletRequest.class), any(HttpServletResponse.class))).thenReturn(apolloConfig);
  }

  @Test
  public void testEncodedFormatWordWithoutSignatureIsRejected() throws Exception {
    // MVC decodes the lookup path, so this binds appId=victimApp via the /json
    // route; the filter must demand victimApp's key, not the "json" app's.
    mockMvc
        .perform(getRequest("/configfiles/%6a%73%6f%6e/victimApp/default/application",
            "/configfiles/json/victimApp/default/application", null, null))
        .andExpect(status().isUnauthorized());

    verify(accessKeyServiceWithCache).getAvailableSecrets("victimApp");
  }

  @Test
  public void testEncodedFormatWordWithVictimAppSignaturePasses() throws Exception {
    mockMvc
        .perform(getRequest("/configfiles/%6a%73%6f%6e/victimApp/default/application",
            "/configfiles/json/victimApp/default/application", "victimApp", "victimSecret"))
        .andExpect(status().isOk()).andExpect(content().string(containsString("k1")));

    // The config lookup must receive the same appId the key lookup did.
    verify(accessKeyServiceWithCache).getAvailableSecrets("victimApp");
    verify(configController).queryConfig(eq("victimApp"), any(), any(), any(), any(), any(), any(),
        isNull(), any(HttpServletRequest.class), any(HttpServletResponse.class));
  }

  @Test
  public void testMatrixFormatWordWithoutSignatureIsRejected() throws Exception {
    // MVC strips ';' matrix content, so this also binds appId=victimApp.
    mockMvc
        .perform(getRequest("/configfiles/json;v=1/victimApp/default/application",
            "/configfiles/json/victimApp/default/application", null, null))
        .andExpect(status().isUnauthorized());

    verify(accessKeyServiceWithCache).getAvailableSecrets("victimApp");
  }

  @Test
  public void testDotClusterIsRejected() throws Exception {
    mockMvc
        .perform(getRequest("/configfiles/json/victimApp/%2e/application",
            "/configfiles/json/victimApp/application", null, null))
        .andExpect(status().isBadRequest());

    verify(accessKeyServiceWithCache, never()).getAvailableSecrets(anyString());
  }

  @Test
  public void testAppNamedJsonOnGenericRoute() throws Exception {
    // "/configfiles/json/default/application" is the generic route with appId
    // "json": it must keep working once signed for that app.
    mockMvc
        .perform(getRequest("/configfiles/json/default/application",
            "/configfiles/json/default/application", "json", "jsonSecret"))
        .andExpect(status().isOk()).andExpect(content().string(containsString("k1=v1")));

    verify(accessKeyServiceWithCache).getAvailableSecrets("json");
    verify(configController).queryConfig(eq("json"), any(), any(), any(), any(), any(), any(),
        isNull(), any(HttpServletRequest.class), any(HttpServletResponse.class));
  }

  @Test
  public void testAppNamedRawOnGenericRoute() throws Exception {
    mockMvc
        .perform(getRequest("/configfiles/raw/default/application",
            "/configfiles/raw/default/application", "raw", "rawSecret"))
        .andExpect(status().isOk()).andExpect(content().string(containsString("k1=v1")));

    verify(accessKeyServiceWithCache).getAvailableSecrets("raw");
    verify(configController).queryConfig(eq("raw"), any(), any(), any(), any(), any(), any(),
        isNull(), any(HttpServletRequest.class), any(HttpServletResponse.class));
  }

  @Test
  public void testPlainJsonRouteWithoutSignatureIsRejected() throws Exception {
    mockMvc
        .perform(getRequest("/configfiles/json/victimApp/default/application",
            "/configfiles/json/victimApp/default/application", null, null))
        .andExpect(status().isUnauthorized());

    verify(accessKeyServiceWithCache).getAvailableSecrets("victimApp");
  }

  @Test
  public void testMultiSegmentNamespaceIsRejected() throws Exception {
    // {namespace:.+} never spans a '/', so this matches no route.
    mockMvc
        .perform(getRequest("/configfiles/json/victimApp/default/a/b",
            "/configfiles/json/victimApp/default/a/b", null, null))
        .andExpect(status().isBadRequest());

    verify(accessKeyServiceWithCache, never()).getAvailableSecrets(anyString());
  }

  /**
   * Build a request the way a real container presents it: the raw URI as sent
   * by the client, and the servlet path as normalized by the container. The
   * builder is given the normalized path (it re-encodes '%' sequences), and the
   * raw URI is installed afterwards so the filter and MVC see exactly what the
   * client sent.
   */
  private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getRequest(
      String rawUri, String servletPath, String signingAppId, String signingSecret) {
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder =
        get(servletPath).servletPath(servletPath);
    builder.with(request -> {
      request.setRequestURI(rawUri);
      return request;
    });
    if (signingSecret != null) {
      String timestamp = Long.toString(System.currentTimeMillis());
      String signature = accessKeyUtil.buildSignature(rawUri, null, timestamp, signingSecret);
      builder.header(Signature.HTTP_HEADER_TIMESTAMP, timestamp);
      builder.header(HttpHeaders.AUTHORIZATION, "Apollo " + signingAppId + ":" + signature);
    }
    return builder;
  }
}
