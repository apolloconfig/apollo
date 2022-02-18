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
package com.ctrip.framework.apollo.model;


import com.ctrip.framework.apollo.enums.PropertyChangeType;

/**
 * Holds the information for a config change.
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigChange {
  private final String namespace;
  private final String propertyName;
  private String oldValue;
  private String newValue;
  private PropertyChangeType changeType;

  /**
   * Constructor.
   * @param namespace the namespace of the key
   * @param propertyName the key whose value is changed
   * @param oldValue the value before change
   * @param newValue the value after change
   * @param changeType the change type
   */
  public ConfigChange(String namespace, String propertyName, String oldValue, String newValue,
                      PropertyChangeType changeType) {
    this.namespace = namespace;
    this.propertyName = propertyName;
    this.oldValue = oldValue;
    this.newValue = newValue;
    this.changeType = changeType;
  }

  public String getPropertyName() {
    return propertyName;
  }

  public String getOldValue() {
    return oldValue;
  }

  public String getNewValue() {
    return newValue;
  }

  public PropertyChangeType getChangeType() {
    return changeType;
  }

  public void setOldValue(String oldValue) {
    this.oldValue = oldValue;
  }

  public void setNewValue(String newValue) {
    this.newValue = newValue;
  }

  public void setChangeType(PropertyChangeType changeType) {
    this.changeType = changeType;
  }

  public String getNamespace() {
    return namespace;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ConfigChange{");
    sb.append("namespace='").append(namespace).append('\'');
    sb.append(", propertyName='").append(propertyName).append('\'');
    sb.append(", oldValue='").append(oldValue).append('\'');
    sb.append(", newValue='").append(newValue).append('\'');
    sb.append(", changeType=").append(changeType);
    sb.append('}');
    return sb.toString();
  }
}
