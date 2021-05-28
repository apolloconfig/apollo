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
package com.ctrip.framework.foundation.spi.provider;

public interface Provider {
  /**
   * @return the current provider's type
   */
  Class<? extends Provider> getType();

  /**
   * Return the property value with the given name, or {@code defaultValue} if the name doesn't exist.
   *
   * @param name the property name
   * @param defaultValue the default value when name is not found or any error occurred
   * @return the property value
   */
  String getProperty(String name, String defaultValue);

  /**
   * Initialize the provider
   */
  void initialize();
}
