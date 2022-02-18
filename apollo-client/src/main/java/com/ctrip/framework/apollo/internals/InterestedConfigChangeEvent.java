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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * In {@link ApolloConfigChangeListener} you set some interested key's rule, you can get those keys
 * by this class's instance.
 *
 * @author wxq
 */
class InterestedConfigChangeEvent extends ConfigChangeEvent {

  /**
   * @see Config#addChangeListener(ConfigChangeListener, Set)
   * @see Config#addChangeListener(ConfigChangeListener, Set, Set)
   * @see ApolloConfigChangeListener#interestedKeys()
   * @see ApolloConfigChangeListener#interestedKeyPrefixes()
   */
  private final Set<String> m_interestedChangedKeys;

  public InterestedConfigChangeEvent(String namespace,
      Map<String, ConfigChange> changes, Set<String> interestedChangedKeys) {
    super(namespace, changes);
    this.m_interestedChangedKeys = interestedChangedKeys;
  }

  /**
   * @return interested and changed keys
   */
  @Override
  public Set<String> interestedChangedKeys() {
    return Collections.unmodifiableSet(this.m_interestedChangedKeys);
  }
}
