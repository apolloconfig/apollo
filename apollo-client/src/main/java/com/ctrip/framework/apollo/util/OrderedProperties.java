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
package com.ctrip.framework.apollo.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * An OrderedProperties instance will keep appearance order in config file.
 *
 * <strong>
 * Warnings: 1. It should be noticed that stream APIs or JDk1.8 APIs( listed in
 * https://github.com/ctripcorp/apollo/pull/2861) are not implemented here. 2. {@link Properties}
 * implementation are different between JDK1.8 and later JDKs. At least, {@link Properties} have an
 * individual implementation in JDK10. Hence, there should be an individual putAll method here.
 * </strong>
 *
 * @author songdragon@zts.io
 */
public class OrderedProperties extends Properties {

  private static final long serialVersionUID = -1741073539526213291L;
  private final Set<String> propertyNames;

  public OrderedProperties() {
    propertyNames = Collections.synchronizedSet(new LinkedHashSet<String>());
  }

  @Override
  public synchronized Object put(Object key, Object value) {
    addPropertyName(key);
    return super.put(key, value);
  }

  private void addPropertyName(Object key) {
    if (key instanceof String) {
      propertyNames.add((String) key);
    }
  }

  @Override
  public Set<String> stringPropertyNames() {
    return propertyNames;
  }

  @Override
  public Enumeration<?> propertyNames() {
    return Collections.enumeration(propertyNames);
  }

  @Override
  public synchronized Enumeration<Object> keys() {
    return new Enumeration<Object>() {
      private final Iterator<String> i = propertyNames.iterator();

      @Override
      public boolean hasMoreElements() {
        return i.hasNext();
      }

      @Override
      public Object nextElement() {
        return i.next();
      }
    };
  }

  @Override
  public Set<Object> keySet() {
    return new LinkedHashSet<Object>(propertyNames);
  }


  @Override
  public Set<Entry<Object, Object>> entrySet() {
    Set<Entry<Object, Object>> original = super.entrySet();
    LinkedHashMap<Object, Entry<Object, Object>> entryMap = new LinkedHashMap<>();
    for (String propertyName : propertyNames) {
      entryMap.put(propertyName, null);
    }

    for (Entry<Object, Object> entry : original) {
      entryMap.put(entry.getKey(), entry);
    }

    return new LinkedHashSet<>(entryMap.values());
  }

  @Override
  public synchronized void putAll(Map<?, ?> t) {
    super.putAll(t);
    for (Object name : t.keySet()) {
      addPropertyName(name);
    }
  }

  @Override
  public synchronized void clear() {
    super.clear();
    this.propertyNames.clear();
  }

  @Override
  public synchronized Object remove(Object key) {
    if (key instanceof String) {
      this.propertyNames.remove(key);
    }
    return super.remove(key);
  }

}
