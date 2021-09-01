/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.util.factory;

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import java.util.Properties;

/**
 * Factory interface to construct Properties instances.
 *
 * @author songdragon@zts.io
 */
public interface PropertiesFactory {

  /**
   * Configuration to keep properties order as same as line order in .yml/.yaml/.properties file.
   */
  String APOLLO_PROPERTY_ORDER_ENABLE = ApolloClientSystemConsts.APOLLO_PROPERTY_ORDER_ENABLE;

  /**
   * <pre>
   * Default implementation:
   * 1. if {@link APOLLO_PROPERTY_ORDER_ENABLE} is true return a new
   * instance of {@link com.ctrip.framework.apollo.util.OrderedProperties}.
   * 2. else return a new instance of {@link Properties}
   * </pre>
   *
   * @return
   */
  Properties getPropertiesInstance();
}
