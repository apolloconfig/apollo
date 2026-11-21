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
package com.ctrip.framework.apollo.configservice.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.configservice.service.AccessKeyServiceWithCache;
import com.google.common.collect.Lists;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
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
  public void testExtractAppIdFromRawConfigFileRequest() {
    when(request.getServletPath()).thenReturn("/configfiles/raw/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithRawAppId() {
    when(request.getServletPath()).thenReturn("/configfiles/raw/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("raw");
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithJsonAppId() {
    when(request.getServletPath()).thenReturn("/configfiles/json/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("json");
  }

  // GHSA-v8jm: Tomcat normalizes getServletPath(), dropping "." / "%2e" segments,
  // so /configfiles/json/someAppId/%2e/application becomes
  // /configfiles/json/someAppId/application and the old code returned "json".
  // The fix parses the raw request URI instead, and a "." cluster is rejected.
  @Test
  public void testExtractAppIdFromJsonConfigFileRequestWithDotClusterSegment() {
    mockConfigFileRequest("/configfiles/json/someAppId/%2e/application",
        "/configfiles/json/someAppId/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromJsonConfigFileRequestWithEncodedFormatWord() {
    // GHSA-v8jm review: MVC decodes the lookup path before matching, so an encoded
    // format word must route the same way as the plain one.
    mockConfigFileRequest("/configfiles/%6a%73%6f%6e/someAppId/default/application",
        "/configfiles/json/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromJsonConfigFileRequestWithMatrixFormatWord() {
    // GHSA-v8jm review: MVC strips ';' matrix content from the raw path, so a
    // matrix-suffixed format word must route the same way as the plain one.
    mockConfigFileRequest("/configfiles/json;v=1/someAppId/default/application",
        "/configfiles/json/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEncodedSemicolonIsNotFormatWord() {
    // MVC strips semicolons from the raw path BEFORE decoding, so an encoded ';'
    // is decoded to a literal ';' afterwards: "json;x" is not the "json" format
    // word, and the generic appId "json;x" is invalid.
    mockConfigFileRequest("/configfiles/json%3Bx/someAppId/default/application",
        "/configfiles/json;x/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithTooManySegments() {
    // {namespace:.+} never spans a '/', so a 5-segment path matches no route.
    mockConfigFileRequest("/configfiles/json/someAppId/default/a/b",
        "/configfiles/json/someAppId/default/a/b");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithTrailingSlash() {
    // A trailing slash leaves an empty namespace segment, which matches no route.
    mockConfigFileRequest("/configfiles/json/someAppId/default/",
        "/configfiles/json/someAppId/default/");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEmptySegment() {
    mockConfigFileRequest("/configfiles/json//default/application",
        "/configfiles/json//default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEncodedSlashInNamespace() {
    // An encoded separator is rejected before decoding: it would split the
    // path differently for the filter and MVC.
    mockConfigFileRequest("/configfiles/json/someAppId/default/application%2Fextra",
        "/configfiles/json/someAppId/default/application/extra");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEncodedSlashInCluster() {
    // Before the encoded-separator rejection this returned victimApp and the
    // filter looked up its key, while MVC decoded %2F into a 5-segment path
    // that matches no route.
    mockConfigFileRequest("/configfiles/json/victimApp/def%2Flt/application",
        "/configfiles/json/victimApp/def/lt/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEncodedSlashInFormatWord() {
    mockConfigFileRequest("/configfiles/json%2Fraw/victimApp/default/application",
        "/configfiles/json/raw/victimApp/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEncodedSlashInAppId() {
    mockConfigFileRequest("/configfiles/json/victim%2Fapp/default/application",
        "/configfiles/json/victim/app/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEncodedBackslashInCluster() {
    mockConfigFileRequest("/configfiles/json/victimApp/def%5Clt/application",
        "/configfiles/json/victimApp/def/lt/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithDoubleEncodedSlashInAppId() {
    // %252F decodes to the literal text "%2F", which is not a separator for
    // MVC either, so it is not rejected by the separator check; the appId is
    // still invalid because '%' is not an allowed appId character.
    mockConfigFileRequest("/configfiles/json/victim%252Fapp/default/application",
        "/configfiles/json/victim%2Fapp/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithEncodedSlashGuardingSeparatorCheck() {
    // Guard test for the encoded-separator rejection: the raw path has only
    // 2 segments, so without the check it would decode to the valid generic
    // route /configfiles/victimApp/default/application and return
    // "victimApp" — the segment-count rejection alone would not catch it.
    mockConfigFileRequest("/configfiles/victimApp%2Fdefault/application",
        "/configfiles/victimApp/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithDotFormatWord() {
    // A decoded '.' as the first segment matches no route.
    mockConfigFileRequest("/configfiles/%2e/json/someAppId/default/application",
        "/configfiles/json/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithDoubleDotClusterSegment() {
    mockConfigFileRequest("/configfiles/json/someAppId/%2e%2e/application",
        "/configfiles/json/someAppId/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromJsonConfigFileRequestWithEncodedDotClusterVariants() {
    String[] dotSegments = {".", "%2E", ".;x"};
    for (String dot : dotSegments) {
      mockConfigFileRequest("/configfiles/json/someAppId/" + dot + "/application",
          "/configfiles/json/someAppId/application");

      assertThat(accessKeyUtil.extractAppIdFromRequest(request)).isNull();
    }
  }

  @Test
  public void testExtractAppIdFromRawConfigFileRequestWithDotClusterSegment() {
    mockConfigFileRequest("/configfiles/raw/someAppId/%2e/application",
        "/configfiles/raw/someAppId/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestPrefersRawRequestUri() {
    // Proves the appId comes from getRequestURI(), not the normalized servlet path:
    // the servlet path lost the "default" segment, the raw URI did not.
    mockConfigFileRequest("/configfiles/json/someAppId/default/application",
        "/configfiles/json/someAppId/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithDottedClusterName() {
    // A cluster name merely containing a dot (e.g. "gray-1.0") is legitimate.
    mockConfigFileRequest("/configfiles/json/someAppId/gray-1.0/application",
        "/configfiles/json/someAppId/gray-1.0/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromConfigFileRequestWithRawUri() {
    mockConfigFileRequest("/configfiles/raw/someAppId/default/application",
        "/configfiles/raw/someAppId/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  private void mockConfigFileRequest(String requestUri, String servletPath) {
    when(request.getRequestURI()).thenReturn(requestUri);
    when(request.getContextPath()).thenReturn("");
    when(request.getServletPath()).thenReturn(servletPath);
  }

  @Test
  public void testExtractAppIdFromRawConfigFileRequestShouldRejectAppIdWithTrailingSpace() {
    when(request.getServletPath()).thenReturn("/configfiles/raw/mysql.example.test /default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromRequest5() {
    when(request.getServletPath()).thenReturn("/notifications/v2");
    when(request.getParameter("appId")).thenReturn("someAppId");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromV1NotificationRequest() {
    when(request.getServletPath()).thenReturn("/notifications");
    when(request.getParameter("appId")).thenReturn("someAppId");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isEqualTo("someAppId");
  }

  @Test
  public void testExtractAppIdFromV1NotificationRequestShouldRejectAppIdWithTrailingSpace() {
    when(request.getServletPath()).thenReturn("/notifications");
    when(request.getParameter("appId")).thenReturn("mysql.example.test ");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromRequestShouldRejectAppIdWithDiacritic() {
    when(request.getServletPath()).thenReturn("/configs/mysql.exámple.test/default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromRequestShouldRejectAppIdWithTrailingSpace() {
    when(request.getServletPath()).thenReturn("/configs/mysql.example.test /default/application");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
  }

  @Test
  public void testExtractAppIdFromNotificationRequestShouldRejectAppIdWithTrailingSpace() {
    when(request.getServletPath()).thenReturn("/notifications/v2");
    when(request.getParameter("appId")).thenReturn("mysql.example.test ");

    String appId = accessKeyUtil.extractAppIdFromRequest(request);

    assertThat(appId).isNull();
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
