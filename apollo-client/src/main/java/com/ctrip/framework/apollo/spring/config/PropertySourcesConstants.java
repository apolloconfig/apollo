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
package com.ctrip.framework.apollo.spring.config;

public interface PropertySourcesConstants {
  String APOLLO_PROPERTY_SOURCE_NAME = "ApolloPropertySources";
  String APOLLO_BOOTSTRAP_PROPERTY_SOURCE_NAME = "ApolloBootstrapPropertySources";
  String APOLLO_BOOTSTRAP_ENABLED = "apollo.bootstrap.enabled";
  String APOLLO_BOOTSTRAP_EAGER_LOAD_ENABLED = "apollo.bootstrap.eagerLoad.enabled";
  String APOLLO_BOOTSTRAP_NAMESPACES = "apollo.bootstrap.namespaces";
}
