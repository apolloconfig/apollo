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

import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;

/**
 * @author Shawyeok (shawyeok@outlook.com)
 */
public class CachedCompositePropertySource extends CompositePropertySource
    implements ConfigChangeListener {

  private volatile String[] names;

  public CachedCompositePropertySource(String name) {
    super(name);
  }

  @Override
  public String[] getPropertyNames() {
    String[] propertyNames = this.names;
    if (propertyNames == null) {
      this.names = propertyNames = super.getPropertyNames();
    }
    return propertyNames;
  }

  @Override
  public void addPropertySource(PropertySource<?> propertySource) {
    super.addPropertySource(propertySource);
    if (propertySource instanceof ConfigPropertySource) {
      ((ConfigPropertySource) propertySource).addChangeListener(this);
    }
  }

  @Override
  public void addFirstPropertySource(PropertySource<?> propertySource) {
    super.addFirstPropertySource(propertySource);
    if (propertySource instanceof ConfigPropertySource) {
      ((ConfigPropertySource) propertySource).addChangeListener(this);
    }
  }

  @Override
  public void onChange(ConfigChangeEvent changeEvent) {
    // clear property names cache if any sources has changed
    this.names = null;
  }
}
