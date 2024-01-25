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
package com.ctrip.framework.apollo.common.condition;

import com.google.common.collect.Sets;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class OnProfileCondition implements Condition {
  @Override
  public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
    Set<String> activeProfiles = Sets.newHashSet(context.getEnvironment().getActiveProfiles());

    Set<String> requiredActiveProfiles = retrieveAnnotatedProfiles(metadata, ConditionalOnProfile.class.getName());
    Set<String> requiredInactiveProfiles = retrieveAnnotatedProfiles(metadata, ConditionalOnMissingProfile.class
        .getName());

    return Sets.difference(requiredActiveProfiles, activeProfiles).isEmpty()
        && Sets.intersection(requiredInactiveProfiles, activeProfiles).isEmpty();
  }

  private Set<String> retrieveAnnotatedProfiles(AnnotatedTypeMetadata metadata, String annotationType) {
    if (!metadata.isAnnotated(annotationType)) {
      return Collections.emptySet();
    }

    MultiValueMap<String, Object> attributes = metadata.getAllAnnotationAttributes(annotationType);

    if (attributes == null) {
      return Collections.emptySet();
    }

    Set<String> profiles = Sets.newHashSet();
    List<?> values = attributes.get("value");

    if (values != null) {
      for (Object value : values) {
        if (value instanceof String[]) {
          Collections.addAll(profiles, (String[]) value);
        }
        else {
          profiles.add((String) value);
        }
      }
    }

    return profiles;
  }
}
