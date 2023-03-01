/*
 * Copyright 2023 Apollo Authors
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
package com.ctrip.framework.apollo.common.datasource;

import com.ctrip.framework.apollo.core.utils.StringUtils;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class TitanCondition implements Condition {

  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    if (!StringUtils.isEmpty(context.getEnvironment().getProperty("fat.titan.url"))) {
      return true;
    }
    if (!StringUtils.isEmpty(context.getEnvironment().getProperty("uat.titan.url"))) {
      return true;
    }
    return !StringUtils.isEmpty(context.getEnvironment().getProperty("pro.titan.url"));
  }

}
