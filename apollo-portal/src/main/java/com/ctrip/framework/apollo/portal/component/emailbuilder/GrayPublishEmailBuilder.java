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

import com.google.common.base.Joiner;
import com.google.gson.Gson;

import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

@Component
public class GrayPublishEmailBuilder extends ConfigPublishEmailBuilder {

  private static final String EMAIL_SUBJECT = "[Apollo] 灰度发布";

  private Gson gson = new Gson();
  private Joiner IP_JOINER = Joiner.on(", ");

  @Override
  protected String subject() {
    return EMAIL_SUBJECT;
  }

  @Override
  public String emailContent(Env env, ReleaseHistoryBO releaseHistory) {
    String result = renderEmailCommonContent(env, releaseHistory);
    return renderGrayReleaseRuleContent(result, releaseHistory);
  }

  @Override
  protected String getTemplateFramework() {
    return portalConfig.emailTemplateFramework();
  }

  @Override
  protected String getDiffModuleTemplate() {
    return portalConfig.emailReleaseDiffModuleTemplate();
  }

  private String renderGrayReleaseRuleContent(String bodyTemplate, ReleaseHistoryBO releaseHistory) {

    Map<String, Object> context = releaseHistory.getOperationContext();
    Object rules = context.get("rules");
    List<GrayReleaseRuleItemDTO>
            ruleItems = rules == null ?
            null : gson.fromJson(rules.toString(), GsonType.RULE_ITEMS);


    if (CollectionUtils.isEmpty(ruleItems)) {
      return bodyTemplate.replaceAll(EMAIL_CONTENT_GRAY_RULES_MODULE, "<br><h4>无灰度规则</h4>");
    }
    StringBuilder rulesHtmlBuilder = new StringBuilder();
    for (GrayReleaseRuleItemDTO ruleItem : ruleItems) {
      String clientAppId = ruleItem.getClientAppId();
      Set<String> ips = ruleItem.getClientIpList();

      rulesHtmlBuilder.append("<b>AppId:&nbsp;</b>")
              .append(clientAppId)
              .append("&nbsp;&nbsp; <b>IP:&nbsp;</b>");

      IP_JOINER.appendTo(rulesHtmlBuilder, ips);
    }
    String grayRulesModuleContent = portalConfig.emailGrayRulesModuleTemplate().replaceAll(EMAIL_CONTENT_GRAY_RULES_CONTENT,
            Matcher.quoteReplacement(rulesHtmlBuilder.toString()));

    return bodyTemplate.replaceAll(EMAIL_CONTENT_GRAY_RULES_MODULE, Matcher.quoteReplacement(grayRulesModuleContent));

  }
}
