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
package com.ctrip.framework.apollo.common.config;

import org.springframework.core.env.MapPropertySource;

import java.util.Map;

public abstract class RefreshablePropertySource extends MapPropertySource {


  public RefreshablePropertySource(String name, Map<String, Object> source) {
    super(name, source);
  }

  @Override
  public Object getProperty(String name) {
    return this.source.get(name);
  }

  /**
   * refresh property
   */
  protected abstract void refresh();

}
