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
package com.ctrip.framework.apollo.spi;

/**
 * The manually config registry, use with caution!
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigRegistry {
  /**
   * Register the config factory for the namespace specified.
   *
   * @param namespace the namespace
   * @param factory   the factory for this namespace
   */
  void register(String namespace, ConfigFactory factory);

  /**
   * Get the registered config factory for the namespace.
   *
   * @param namespace the namespace
   * @return the factory registered for this namespace
   */
  ConfigFactory getFactory(String namespace);
}
