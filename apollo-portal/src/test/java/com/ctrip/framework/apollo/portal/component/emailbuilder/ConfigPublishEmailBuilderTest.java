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
package com.ctrip.framework.apollo.portal.component.emailbuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.ctrip.framework.apollo.common.constants.ReleaseOperation;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.Email;
import com.ctrip.framework.apollo.portal.entity.bo.KVEntity;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.enums.ChangeType;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Regression tests for GHSA-47xv-g6gr-fw5c: config item values, release title/comment and
 * gray-release rule fields are interpolated into text/html notification mails. Every
 * interpolated value must be HTML-escaped so a stored payload can never break out of the
 * mail body as markup. The rendered diff module is also passed through
 * Matcher.quoteReplacement in the final replaceAll, so backslashes in config values
 * survive the substitution instead of being eaten as replacement escapes.
 */
@ExtendWith(MockitoExtension.class)
public class ConfigPublishEmailBuilderTest {

  private static final String MALICIOUS_KEY = "<img src=x onerror=alert(1)>";
  private static final String MALICIOUS_OLD_VALUE = "old-<script>alert(2)</script>";
  private static final String MALICIOUS_NEW_VALUE = "new-<img src=x onerror=alert(3)>";
  private static final String MALICIOUS_TITLE = "<b>evil-title</b>";
  private static final String MALICIOUS_COMMENT = "<img src=x onerror=alert(4)>";
  private static final String MALICIOUS_CLIENT_APP_ID = "<img src=x onerror=alert(5)>";
  private static final String MALICIOUS_IP = "<script>alert(6)</script>";
  private static final String BACKSLASH_OLD_VALUE = "C:\\old\\path";
  private static final String BACKSLASH_NEW_VALUE = "C:\\apollo\\config";

  @Mock
  private RolePermissionService rolePermissionService;
  @Mock
  private ReleaseService releaseService;
  @Mock
  private AppNamespaceService appNamespaceService;
  @Mock
  private UserService userService;
  @Mock
  private PortalConfig portalConfig;

  @InjectMocks
  private NormalPublishEmailBuilder normalPublishEmailBuilder;
  @InjectMocks
  private GrayPublishEmailBuilder grayPublishEmailBuilder;

  @BeforeEach
  void setUp() {
    when(portalConfig.emailSender()).thenReturn("apollo@ctrip.com");
    when(portalConfig.portalAddress()).thenReturn("http://apollo:8070");
    when(portalConfig.emailTemplateFramework()).thenReturn(
        "#{appId} #{env} #{clusterName} #{namespaceName} #{operator} #{releaseTime} "
            + "#{releaseId} #{releaseHistoryId} #{releaseTitle} #{releaseComment} "
            + "#{apollo.portal.address} #{diffModule} #{rulesModule}");
    when(portalConfig.emailReleaseDiffModuleTemplate())
        .thenReturn("<table>#{diffContent}</table>");

    when(rolePermissionService.queryUsersWithRole(anyString()))
        .thenReturn(Collections.emptySet());
    when(userService.findByUserIds(anyList())).thenReturn(Collections.emptyList());

    AppNamespace appNamespace = new AppNamespace();
    appNamespace.setFormat(ConfigFileFormat.Properties.getValue());
    when(appNamespaceService.findByAppIdAndName("testApp", "application"))
        .thenReturn(appNamespace);

    ReleaseCompareResult compareResult = new ReleaseCompareResult();
    compareResult.addEntityPair(ChangeType.MODIFIED, new KVEntity(MALICIOUS_KEY,
        MALICIOUS_OLD_VALUE), new KVEntity(MALICIOUS_KEY, MALICIOUS_NEW_VALUE));
    compareResult.addEntityPair(ChangeType.MODIFIED, new KVEntity("config.path",
        BACKSLASH_OLD_VALUE), new KVEntity("config.path", BACKSLASH_NEW_VALUE));
    when(releaseService.compare(eq(Env.DEV), eq(1L), eq(2L))).thenReturn(compareResult);
  }

  @Test
  void normalPublishEmail_shouldEscapeHtmlInDiffAndReleaseMetadata() {
    ReleaseHistoryBO releaseHistory =
        releaseHistory(ReleaseOperation.NORMAL_RELEASE, new HashMap<>());

    Email email = normalPublishEmailBuilder.build(Env.DEV, releaseHistory);

    String body = email.getBody();
    assertFalse(body.contains("<img"), "raw img tag must not reach the mail body");
    assertFalse(body.contains("<script"), "raw script tag must not reach the mail body");
    assertFalse(body.contains("<b>evil-title</b>"), "raw markup must not reach the mail body");
    assertTrue(body.contains("&lt;img src=x onerror=alert(1)&gt;"));
    assertTrue(body.contains("old-&lt;script&gt;alert(2)&lt;/script&gt;"));
    assertTrue(body.contains("new-&lt;img src=x onerror=alert(3)&gt;"));
    assertTrue(body.contains("&lt;b&gt;evil-title&lt;/b&gt;"));
    assertTrue(body.contains("&lt;img src=x onerror=alert(4)&gt;"));
  }

  @Test
  void normalPublishEmail_shouldPreserveBackslashesInDiffValues() {
    ReleaseHistoryBO releaseHistory =
        releaseHistory(ReleaseOperation.NORMAL_RELEASE, new HashMap<>());

    Email email = normalPublishEmailBuilder.build(Env.DEV, releaseHistory);

    String body = email.getBody();
    assertTrue(body.contains("C:\\old\\path"),
        "backslashes in the old value must survive the final replaceAll");
    assertTrue(body.contains("C:\\apollo\\config"),
        "backslashes in the new value must survive the final replaceAll");
  }

  @Test
  void grayPublishEmail_shouldEscapeHtmlInGrayRules() {
    when(portalConfig.emailGrayRulesModuleTemplate()).thenReturn("<div>#{rulesContent}</div>");
    String rulesJson = new Gson().toJson(Collections.singletonList(
        new GrayReleaseRuleItemDTO(MALICIOUS_CLIENT_APP_ID,
            Sets.newHashSet("1.2.3.4", MALICIOUS_IP), Sets.newHashSet())));
    Map<String, Object> operationContext = new HashMap<>();
    operationContext.put("rules", rulesJson);
    ReleaseHistoryBO releaseHistory = releaseHistory(ReleaseOperation.GRAY_RELEASE,
        operationContext);

    Email email = grayPublishEmailBuilder.build(Env.DEV, releaseHistory);

    String body = email.getBody();
    assertFalse(body.contains("<img"), "raw img tag must not reach the mail body");
    assertFalse(body.contains("<script"), "raw script tag must not reach the mail body");
    assertTrue(body.contains("&lt;img src=x onerror=alert(5)&gt;"));
    assertTrue(body.contains("&lt;script&gt;alert(6)&lt;/script&gt;"));
    assertTrue(body.contains("1.2.3.4"));
    // the shared render path (diff + release metadata) is escaped as well
    assertTrue(body.contains("&lt;b&gt;evil-title&lt;/b&gt;"));
    assertTrue(body.contains("&lt;img src=x onerror=alert(4)&gt;"));
  }

  private ReleaseHistoryBO releaseHistory(int operation, Map<String, Object> operationContext) {
    ReleaseHistoryBO releaseHistory = new ReleaseHistoryBO();
    releaseHistory.setId(1L);
    releaseHistory.setAppId("testApp");
    releaseHistory.setClusterName("default");
    releaseHistory.setNamespaceName("application");
    releaseHistory.setOperator("operator");
    releaseHistory.setReleaseId(2L);
    releaseHistory.setReleaseTitle(MALICIOUS_TITLE);
    releaseHistory.setReleaseComment(MALICIOUS_COMMENT);
    releaseHistory.setReleaseTime(new Date());
    releaseHistory.setPreviousReleaseId(1L);
    releaseHistory.setOperation(operation);
    releaseHistory.setOperationContext(operationContext);
    return releaseHistory;
  }
}
