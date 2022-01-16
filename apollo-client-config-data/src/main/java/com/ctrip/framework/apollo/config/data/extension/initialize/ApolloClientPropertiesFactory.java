/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.config.data.extension.initialize;

import com.ctrip.framework.apollo.config.data.extension.properties.ApolloClientProperties;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2ClientProperties;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;

/**
 * @author vdisk <vdisk@foxmail.com>
 */
public class ApolloClientPropertiesFactory {

  public static final String PROPERTIES_PREFIX = "apollo.client";

  public ApolloClientProperties createApolloClientProperties(
      Binder binder, BindHandler bindHandler) {
    return binder
        .bind(PROPERTIES_PREFIX, Bindable.of(ApolloClientProperties.class), bindHandler)
        .orElse(null);
  }

  public OAuth2ClientProperties createOauth2ClientProperties(
      Binder binder, BindHandler bindHandler) {
    return binder
        .bind(
            "spring.security.oauth2.client", Bindable.of(OAuth2ClientProperties.class), bindHandler)
        .orElse(null);
  }
}
