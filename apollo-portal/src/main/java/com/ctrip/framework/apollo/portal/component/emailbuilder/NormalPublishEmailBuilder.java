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


import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.entity.bo.ReleaseHistoryBO;

import org.springframework.stereotype.Component;


@Component
public class NormalPublishEmailBuilder extends ConfigPublishEmailBuilder {

  private static final String EMAIL_SUBJECT = "[Apollo] 配置发布";


  @Override
  protected String subject() {
    return EMAIL_SUBJECT;
  }

  @Override
  protected String emailContent(Env env, ReleaseHistoryBO releaseHistory) {
    return renderEmailCommonContent(env, releaseHistory);
  }

  @Override
  protected String getTemplateFramework() {
    return portalConfig.emailTemplateFramework();
  }

  @Override
  protected String getDiffModuleTemplate() {
    return portalConfig.emailReleaseDiffModuleTemplate();
  }
}
