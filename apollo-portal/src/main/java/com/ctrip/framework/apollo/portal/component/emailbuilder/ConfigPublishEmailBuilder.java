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
package com.ctrip.framework.apollo.portal.component.emailbuilder;

import com.google.common.collect.Lists;

import com.ctrip.framework.apollo.common.constants.ReleaseOperation;
import com.ctrip.framework.apollo.common.constants.ReleaseOperationContext;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.RoleType;
import com.ctrip.framework.apollo.portal.entity.bo.Email;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.vo.Change;
import com.ctrip.framework.apollo.portal.entity.vo.ReleaseCompareResult;
import com.ctrip.framework.apollo.portal.service.AppNamespaceService;
import com.ctrip.framework.apollo.portal.service.ReleaseService;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;

import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public abstract class ConfigPublishEmailBuilder {

  private static final String EMERGENCY_PUBLISH_TAG = "<span style='color:red'>(紧急发布)</span>";

  //email content common field placeholder
  private static final String EMAIL_CONTENT_FIELD_APPID = "#\\{appId\\}";
  private static final String EMAIL_CONTENT_FIELD_ENV = "#\\{env}";
  private static final String EMAIL_CONTENT_FIELD_CLUSTER = "#\\{clusterName}";
  private static final String EMAIL_CONTENT_FIELD_NAMESPACE = "#\\{namespaceName}";
  private static final String EMAIL_CONTENT_FIELD_OPERATOR = "#\\{operator}";
  private static final String EMAIL_CONTENT_FIELD_RELEASE_TIME = "#\\{releaseTime}";
  private static final String EMAIL_CONTENT_FIELD_RELEASE_ID = "#\\{releaseId}";
  private static final String EMAIL_CONTENT_FIELD_RELEASE_HISTORY_ID = "#\\{releaseHistoryId}";
  private static final String EMAIL_CONTENT_FIELD_RELEASE_TITLE = "#\\{releaseTitle}";
  private static final String EMAIL_CONTENT_FIELD_RELEASE_COMMENT = "#\\{releaseComment}";
  private static final String EMAIL_CONTENT_FIELD_APOLLO_SERVER_ADDRESS = "#\\{apollo.portal.address}";
  private static final String EMAIL_CONTENT_FIELD_DIFF_CONTENT = "#\\{diffContent}";
  private static final String EMAIL_CONTENT_FIELD_EMERGENCY_PUBLISH = "#\\{emergencyPublish}";

  private static final String EMAIL_CONTENT_DIFF_MODULE = "#\\{diffModule}";
  protected static final String EMAIL_CONTENT_GRAY_RULES_MODULE = "#\\{rulesModule}";

  //email content special field placeholder
  protected static final String EMAIL_CONTENT_GRAY_RULES_CONTENT = "#\\{rulesContent}";

  //set config's value max length to protect email.
  protected static final int VALUE_MAX_LENGTH = 100;

  protected FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

  @Autowired
  private RolePermissionService rolePermissionService;
  @Autowired
  private ReleaseService releaseService;
  @Autowired
  private AppNamespaceService appNamespaceService;
  @Autowired
  private UserService userService;
  @Autowired
  protected PortalConfig portalConfig;

  /**
   * email subject
   */
  protected abstract String subject();

  /**
   * email body content
   */
  protected abstract String emailContent(Env env, ReleaseHistoryBO releaseHistory);

  /**
   * email body template framework
   */
  protected abstract String getTemplateFramework();

  /**
   * email body diff module template
   */
  protected abstract String getDiffModuleTemplate();


  public Email build(Env env, ReleaseHistoryBO releaseHistory) {

    Email email = new Email();

    email.setSubject(subject());
    email.setSenderEmailAddress(portalConfig.emailSender());
    email.setRecipients(recipients(releaseHistory.getAppId(), releaseHistory.getNamespaceName(), env.toString()));

    String emailBody = emailContent(env, releaseHistory);
    //clear not used module
    emailBody = emailBody.replaceAll(EMAIL_CONTENT_DIFF_MODULE, "");
    emailBody = emailBody.replaceAll(EMAIL_CONTENT_GRAY_RULES_MODULE, "");
    email.setBody(emailBody);

    return email;
  }

  protected String renderEmailCommonContent(Env env, ReleaseHistoryBO releaseHistory) {
    String template = getTemplateFramework();
    String renderResult = renderReleaseBasicInfo(template, env, releaseHistory);
    renderResult = renderDiffModule(renderResult, env, releaseHistory);
    return renderResult;
  }

  private String renderReleaseBasicInfo(String template, Env env, ReleaseHistoryBO releaseHistory) {
    String renderResult = template;

    Map<String, Object> operationContext = releaseHistory.getOperationContext();
    boolean isEmergencyPublish = operationContext.containsKey(ReleaseOperationContext.IS_EMERGENCY_PUBLISH) &&
                                 (boolean) operationContext.get(ReleaseOperationContext.IS_EMERGENCY_PUBLISH);
    if (isEmergencyPublish) {
      renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_EMERGENCY_PUBLISH, Matcher.quoteReplacement(EMERGENCY_PUBLISH_TAG));
    } else {
      renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_EMERGENCY_PUBLISH, "");
    }

    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_APPID, Matcher.quoteReplacement(releaseHistory.getAppId()));
    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_ENV, Matcher.quoteReplacement(env.toString()));
    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_CLUSTER, Matcher.quoteReplacement(releaseHistory.getClusterName()));
    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_NAMESPACE, Matcher.quoteReplacement(releaseHistory.getNamespaceName()));
    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_OPERATOR, Matcher.quoteReplacement(releaseHistory.getOperator()));
    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_RELEASE_TITLE, Matcher.quoteReplacement(releaseHistory.getReleaseTitle()));
    renderResult =
            renderResult.replaceAll(EMAIL_CONTENT_FIELD_RELEASE_ID, String.valueOf(releaseHistory.getReleaseId()));
    renderResult =
            renderResult.replaceAll(EMAIL_CONTENT_FIELD_RELEASE_HISTORY_ID, String.valueOf(releaseHistory.getId()));
    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_RELEASE_COMMENT, Matcher.quoteReplacement(releaseHistory.getReleaseComment() == null ? "" : releaseHistory.getReleaseComment()));
    renderResult = renderResult.replaceAll(EMAIL_CONTENT_FIELD_APOLLO_SERVER_ADDRESS, getApolloPortalAddress());
    return renderResult
            .replaceAll(EMAIL_CONTENT_FIELD_RELEASE_TIME, dateFormat.format(releaseHistory.getReleaseTime()));
  }

  private String renderDiffModule(String bodyTemplate, Env env, ReleaseHistoryBO releaseHistory) {
    String appId = releaseHistory.getAppId();
    String namespaceName = releaseHistory.getNamespaceName();

    AppNamespace appNamespace = appNamespaceService.findByAppIdAndName(appId, namespaceName);
    if (appNamespace == null) {
      appNamespace = appNamespaceService.findPublicAppNamespace(namespaceName);
    }

    //don't show diff content if namespace's format is file
    if (appNamespace == null ||
            !appNamespace.getFormat().equals(ConfigFileFormat.Properties.getValue())) {

      return bodyTemplate.replaceAll(EMAIL_CONTENT_DIFF_MODULE, "<br><h4>变更内容请点击链接到Apollo上查看</h4>");
    }

    ReleaseCompareResult result = getReleaseCompareResult(env, releaseHistory);

    if (!result.hasContent()) {
      return bodyTemplate.replaceAll(EMAIL_CONTENT_DIFF_MODULE, "<br><h4>无配置变更</h4>");
    }

    List<Change> changes = result.getChanges();
    StringBuilder changesHtmlBuilder = new StringBuilder();
    for (Change change : changes) {
      String key = change.getEntity().getFirstEntity().getKey();
      String oldValue = change.getEntity().getFirstEntity().getValue();
      String newValue = change.getEntity().getSecondEntity().getValue();
      newValue = newValue == null ? "" : newValue;

      changesHtmlBuilder.append("<tr>");
      changesHtmlBuilder.append("<td width=\"10%\">").append(change.getType().toString()).append("</td>");
      changesHtmlBuilder.append("<td width=\"20%\">").append(cutOffString(key)).append("</td>");
      changesHtmlBuilder.append("<td width=\"35%\">").append(cutOffString(oldValue)).append("</td>");
      changesHtmlBuilder.append("<td width=\"35%\">").append(cutOffString(newValue)).append("</td>");

      changesHtmlBuilder.append("</tr>");
    }

    String diffContent = Matcher.quoteReplacement(changesHtmlBuilder.toString());
    String diffModuleTemplate = getDiffModuleTemplate();
    String diffModuleRenderResult = diffModuleTemplate.replaceAll(EMAIL_CONTENT_FIELD_DIFF_CONTENT, diffContent);
    return bodyTemplate.replaceAll(EMAIL_CONTENT_DIFF_MODULE, diffModuleRenderResult);
  }

  private ReleaseCompareResult getReleaseCompareResult(Env env, ReleaseHistoryBO releaseHistory) {
    if (releaseHistory.getOperation() == ReleaseOperation.GRAY_RELEASE
            && releaseHistory.getPreviousReleaseId() == 0) {
      ReleaseDTO masterLatestActiveRelease = releaseService.loadLatestRelease(
              releaseHistory.getAppId(), env, releaseHistory.getClusterName(), releaseHistory.getNamespaceName());
      ReleaseDTO branchLatestActiveRelease = releaseService.findReleaseById(env, releaseHistory.getReleaseId());

      return releaseService.compare(masterLatestActiveRelease, branchLatestActiveRelease);
    }

    return releaseService.compare(env, releaseHistory.getPreviousReleaseId(), releaseHistory.getReleaseId());
  }

  private List<String> recipients(String appId, String namespaceName, String env) {
    Set<UserInfo> modifyRoleUsers =
            rolePermissionService
                    .queryUsersWithRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, RoleType.MODIFY_NAMESPACE));
    Set<UserInfo> envModifyRoleUsers =
        rolePermissionService
            .queryUsersWithRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, RoleType.MODIFY_NAMESPACE, env));
    Set<UserInfo> releaseRoleUsers =
            rolePermissionService
                    .queryUsersWithRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, RoleType.RELEASE_NAMESPACE));
    Set<UserInfo> envReleaseRoleUsers =
        rolePermissionService
            .queryUsersWithRole(RoleUtils.buildNamespaceRoleName(appId, namespaceName, RoleType.RELEASE_NAMESPACE, env));
    Set<UserInfo> owners = rolePermissionService.queryUsersWithRole(RoleUtils.buildAppMasterRoleName(appId));

    Set<String> userIds = new HashSet<>(modifyRoleUsers.size() + releaseRoleUsers.size() + owners.size());

    for (UserInfo userInfo : modifyRoleUsers) {
      userIds.add(userInfo.getUserId());
    }

    for (UserInfo userInfo : envModifyRoleUsers) {
      userIds.add(userInfo.getUserId());
    }

    for (UserInfo userInfo : releaseRoleUsers) {
      userIds.add(userInfo.getUserId());
    }

    for (UserInfo userInfo : envReleaseRoleUsers) {
      userIds.add(userInfo.getUserId());
    }

    for (UserInfo userInfo : owners) {
      userIds.add(userInfo.getUserId());
    }

    List<UserInfo> userInfos = userService.findByUserIds(Lists.newArrayList(userIds));

    if (CollectionUtils.isEmpty(userInfos)) {
      return Collections.emptyList();
    }

    List<String> recipients = new ArrayList<>(userInfos.size());
    for (UserInfo userInfo : userInfos) {
      recipients.add(userInfo.getEmail());
    }

    return recipients;
  }

  protected String getApolloPortalAddress() {
    return portalConfig.portalAddress();
  }

  private String cutOffString(String source) {
    if (source.length() > VALUE_MAX_LENGTH) {
      return source.substring(0, VALUE_MAX_LENGTH) + "...";
    }
    return source;
  }

}
