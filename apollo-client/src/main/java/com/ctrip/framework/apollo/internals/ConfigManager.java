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
package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigManager {
  /**
   * Get the config instance for the namespace specified.
   * @param namespace the namespace
   * @return the config instance for the namespace
   */
  Config getConfig(String namespace);

  /**
   * Get the config file instance for the namespace specified.
   * @param namespace the namespace
   * @param configFileFormat the config file format
   * @return the config file instance for the namespace
   */
  ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat);
}
