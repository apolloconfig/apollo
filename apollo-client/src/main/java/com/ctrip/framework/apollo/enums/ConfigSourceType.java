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
package com.ctrip.framework.apollo.enums;

/**
 * To indicate the config's source type, i.e. where is the config loaded from
 *
 * @since 1.1.0
 */
public enum ConfigSourceType {
  REMOTE("Loaded from remote config service"), LOCAL("Loaded from local cache"), NONE("Load failed");

  private final String description;

  ConfigSourceType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
