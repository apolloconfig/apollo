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

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A thread-safe case-insensitive multimap implementation using ConcurrentHashMap with finer-grained locking
 * compared to synchronized collections. 
 * 
 * <p>This implementation replaces the original synchronized TreeMultimap to eliminate thread blocking issues
 * in high concurrency scenarios. Key differences from the original implementation:
 * <ul>
 *   <li>Uses case-insensitive key handling by normalizing keys to lowercase</li>
 *   <li>Does not maintain value ordering (original used Ordering.natural() but ordering was not functionally required)</li>
 *   <li>Provides fine-grained locking per key instead of global synchronization</li>
 * </ul>
 * 
 * @author Apollo Team
 */
public class CaseInsensitiveConcurrentMultimap<K, V> implements Multimap<K, V> {
  private final ConcurrentMap<String, Set<V>> map = new ConcurrentHashMap<>();

  /**
   * Associates the specified value with the specified key.
   * The key is normalized to lowercase for case-insensitive behavior.
   * 
   * This implementation uses compute() to ensure atomicity and avoid race conditions
   * where a set could be removed after being retrieved but before adding a value.
   */
  @Override
  public boolean put(K key, V value) {
    if (key == null || value == null) {
      return false;
    }
    
    String normalizedKey = normalizeKey(key);
    boolean[] added = new boolean[1];
    
    map.compute(normalizedKey, (k, existingSet) -> {
      Set<V> values = existingSet;
      if (values == null) {
        values = ConcurrentHashMap.newKeySet();
      }
      added[0] = values.add(value);
      return values;
    });
    
    return added[0];
  }

  /**
   * Removes a single key-value pair from the multimap.
   * The key is normalized to lowercase for case-insensitive behavior.
   * 
   * This implementation uses computeIfPresent() to ensure atomicity and avoid race conditions
   * where a value could be added to a set that is being removed from the map.
   */
  @Override
  public boolean remove(Object key, Object value) {
    if (key == null || value == null) {
      return false;
    }
    
    String normalizedKey = normalizeKey(key);
    boolean[] removed = new boolean[1];
    
    map.computeIfPresent(normalizedKey, (k, existingSet) -> {
      removed[0] = existingSet.remove(value);
      // Clean up empty sets to avoid memory leaks
      // Return null to remove the entry from the map
      return existingSet.isEmpty() ? null : existingSet;
    });
    
    return removed[0];
  }

  /**
   * Returns a collection of all values associated with the key.
   * The key is normalized to lowercase for case-insensitive behavior.
   */
  @Override
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
  @Override
  public boolean containsKey(Object key) {
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
  @Override
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
  @Override
  public int size() {
    return map.values().stream().mapToInt(Set::size).sum();
  }

  /**
   * Returns true if the multimap contains no key-value pairs.
   */
  @Override
  public boolean isEmpty() {
    return map.isEmpty() || map.values().stream().allMatch(Set::isEmpty);
  }

  /**
   * Removes all key-value pairs from the multimap.
   */
  @Override
  public void clear() {
    map.clear();
  }

  /**
   * Returns true if the multimap contains the specified key-value pair.
   */
  @Override
  public boolean containsValue(Object value) {
    if (value == null) {
      return false;
    }
    return map.values().stream().anyMatch(values -> values.contains(value));
  }

  /**
   * Returns true if the multimap contains the specified entry.
   */
  @Override
  public boolean containsEntry(Object key, Object value) {
    if (key == null || value == null) {
      return false;
    }
    
    String normalizedKey = normalizeKey(key);
    Set<V> values = map.get(normalizedKey);
    return values != null && values.contains(value);
  }

  /**
   * Removes all values associated with the specified key.
   */
  @Override
  public Collection<V> removeAll(Object key) {
    if (key == null) {
      return Collections.emptyList();
    }
    
    String normalizedKey = normalizeKey(key);
    Set<V> values = map.remove(normalizedKey);
    return values != null ? new ArrayList<>(values) : Collections.emptyList();
  }

  /**
   * Replaces all values associated with the specified key with the provided values.
   */
  @Override
  public Collection<V> replaceValues(K key, Iterable<? extends V> values) {
    if (key == null) {
      return Collections.emptyList();
    }
    
    String normalizedKey = normalizeKey(key);
    Set<V> oldValues = map.remove(normalizedKey);
    
    if (values != null) {
      Set<V> newValues = ConcurrentHashMap.newKeySet();
      for (V value : values) {
        if (value != null) {
          newValues.add(value);
        }
      }
      if (!newValues.isEmpty()) {
        map.put(normalizedKey, newValues);
      }
    }
    
    return oldValues != null ? new ArrayList<>(oldValues) : Collections.emptyList();
  }

  /**
   * Returns a view collection of all distinct keys.
   */
  @Override
  public Multiset<K> keys() {
    // This method returns a multiset with each key repeated according to
    // the number of values associated with it
    Multiset<K> keys = HashMultiset.create();
    for (Map.Entry<String, Set<V>> entry : map.entrySet()) {
      @SuppressWarnings("unchecked")
      K key = (K) entry.getKey();
      keys.add(key, entry.getValue().size());
    }
    return keys;
  }

  /**
   * Returns a set view of all distinct keys.
   */
  @Override
  public Set<K> keySet() {
    // Note: This is a limitation - we can't reconstruct the original key casing
    // Return a view of normalized keys cast to K type
    @SuppressWarnings("unchecked")
    Set<K> result = (Set<K>) new HashSet<>(map.keySet());
    return Collections.unmodifiableSet(result);
  }

  /**
   * Returns a collection view of all key-value pairs as Map.Entry objects.
   */
  @Override
  public Collection<Map.Entry<K, V>> entries() {
    List<Map.Entry<K, V>> entries = new ArrayList<>();
    for (Map.Entry<String, Set<V>> mapEntry : map.entrySet()) {
      @SuppressWarnings("unchecked")
      K key = (K) mapEntry.getKey();
      for (V value : mapEntry.getValue()) {
        entries.add(new SimpleEntry<>(key, value));
      }
    }
    return Collections.unmodifiableCollection(entries);
  }

  /**
   * Returns a Map view where each key is associated with a Collection of values.
   */
  @Override
  public Map<K, Collection<V>> asMap() {
    Map<K, Collection<V>> result = new ConcurrentHashMap<>();
    for (Map.Entry<String, Set<V>> entry : map.entrySet()) {
      @SuppressWarnings("unchecked")
      K key = (K) entry.getKey();
      result.put(key, new ArrayList<>(entry.getValue()));
    }
    return Collections.unmodifiableMap(result);
  }

  /**
   * Adds all key-value pairs from the specified multimap.
   */
  @Override
  public boolean putAll(K key, Iterable<? extends V> values) {
    if (key == null || values == null) {
      return false;
    }
    
    boolean changed = false;
    for (V value : values) {
      if (put(key, value)) {
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Adds all key-value pairs from the specified multimap.
   */
  @Override
  public boolean putAll(Multimap<? extends K, ? extends V> multimap) {
    if (multimap == null) {
      return false;
    }
    
    boolean changed = false;
    for (Map.Entry<? extends K, ? extends V> entry : multimap.entries()) {
      if (put(entry.getKey(), entry.getValue())) {
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Normalizes the key to lowercase for case-insensitive behavior.
   * This matches the behavior of the original TreeMultimap with CASE_INSENSITIVE_ORDER.
   */
  private String normalizeKey(Object key) {
    return key.toString().toLowerCase();
  }

  /**
   * Simple Map.Entry implementation for entries() method.
   */
  private static class SimpleEntry<K, V> implements Map.Entry<K, V> {
    private final K key;
    private V value;

    public SimpleEntry(K key, V value) {
      this.key = key;
      this.value = value;
    }

    @Override
    public K getKey() {
      return key;
    }

    @Override
    public V getValue() {
      return value;
    }

    @Override
    public V setValue(V value) {
      V old = this.value;
      this.value = value;
      return old;
    }
  }
}