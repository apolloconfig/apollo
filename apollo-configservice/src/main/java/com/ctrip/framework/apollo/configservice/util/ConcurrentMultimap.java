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
package com.ctrip.framework.apollo.configservice.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe multimap implementation using ConcurrentHashMap with finer-grained locking
 * compared to synchronized collections. Provides case-insensitive key handling.
 * 
 * @author Apollo Team
 */
public class ConcurrentMultimap<K, V> {
  private final ConcurrentMap<String, Set<V>> map = new ConcurrentHashMap<>();

  /**
   * Associates the specified value with the specified key.
   * The key is normalized to lowercase for case-insensitive behavior.
   */
  public boolean put(K key, V value) {
    if (key == null || value == null) {
      return false;
    }
    
    String normalizedKey = normalizeKey(key);
    Set<V> values = map.computeIfAbsent(normalizedKey, k -> ConcurrentHashMap.newKeySet());
    return values.add(value);
  }

  /**
   * Removes a single key-value pair from the multimap.
   * The key is normalized to lowercase for case-insensitive behavior.
   */
  public boolean remove(K key, V value) {
    if (key == null || value == null) {
      return false;
    }
    
    String normalizedKey = normalizeKey(key);
    Set<V> values = map.get(normalizedKey);
    if (values == null) {
      return false;
    }
    
    boolean removed = values.remove(value);
    
    // Clean up empty sets to avoid memory leaks  
    if (removed && values.isEmpty()) {
      map.remove(normalizedKey, values);
    }
    
    return removed;
  }

  /**
   * Returns a collection of all values associated with the key.
   * The key is normalized to lowercase for case-insensitive behavior.
   */
  public Collection<V> get(K key) {
    if (key == null) {
      return Collections.emptyList();
    }
    
    String normalizedKey = normalizeKey(key);
    Set<V> values = map.get(normalizedKey);
    return values != null ? new ArrayList<>(values) : Collections.emptyList();
  }

  /**
   * Returns true if the multimap contains the specified key.
   * The key is normalized to lowercase for case-insensitive behavior.
   */
  public boolean containsKey(K key) {
    if (key == null) {
      return false;
    }
    
    String normalizedKey = normalizeKey(key);
    Set<V> values = map.get(normalizedKey);
    return values != null && !values.isEmpty();
  }

  /**
   * Returns all values in the multimap.
   */
  public Collection<V> values() {
    List<V> allValues = new ArrayList<>();
    for (Set<V> values : map.values()) {
      allValues.addAll(values);
    }
    return allValues;
  }

  /**
   * Returns the total number of key-value pairs in the multimap.
   */
  public int size() {
    return map.values().stream().mapToInt(Set::size).sum();
  }

  /**
   * Returns true if the multimap contains no key-value pairs.
   */
  public boolean isEmpty() {
    return map.isEmpty() || map.values().stream().allMatch(Set::isEmpty);
  }

  /**
   * Removes all key-value pairs from the multimap.
   */
  public void clear() {
    map.clear();
  }

  /**
   * Normalizes the key to lowercase for case-insensitive behavior.
   * This matches the behavior of the original TreeMultimap with CASE_INSENSITIVE_ORDER.
   */
  private String normalizeKey(K key) {
    return key.toString().toLowerCase();
  }
}