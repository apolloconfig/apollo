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
package com.ctrip.framework.apollo.spring.events;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.springframework.context.ApplicationEvent;

/**
 * A Spring Application Event that is fired when Apollo config changes.
 */
public class ApolloConfigChangeEvent extends ApplicationEvent {

  public ApolloConfigChangeEvent(ConfigChangeEvent source) {
    super(source);
  }

  public ConfigChangeEvent getConfigChangeEvent() {
    return (ConfigChangeEvent) getSource();
  }
}
