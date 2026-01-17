/*
 * Copyright 2025 Apollo Authors
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
package com.ctrip.framework.apollo.configservice.wrapper;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Multimap with case-insensitive keys.
 * <p>
 * This class wraps a {@code Map<String, Set<V>>} to provide multimap-like behavior with
 * case-insensitive keys.
 * </p>
 *
 * <p><b>Thread-safety</b>: This class is thread-safe if and only if the supplied delegate map and
 * the sets produced by {@code setSupplier} are thread-safe.
 * </p>
 *
 * <p><b>Views</b>: {@link #get(String)} returns an unmodifiable view of the underlying set.
 * Mutations should be done via {@link #put(String, Object)} and {@link #remove(String, Object)} so
 * that empty sets can be cleaned up.
 * </p>
 */
public class CaseInsensitiveMultimapWrapper<V> {
  private final Map<String, Set<V>> delegate;
  private final Supplier<Set<V>> setSupplier;

  public CaseInsensitiveMultimapWrapper(Map<String, Set<V>> delegate,
      Supplier<Set<V>> setSupplier) {
    this.delegate = delegate;
    this.setSupplier = setSupplier;
  }

  private static String normalizeKey(String key) {
    return Objects.requireNonNull(key, "key").toLowerCase(Locale.ROOT);
  }

  public boolean put(String key, V value) {
    boolean[] added = {false};
    delegate.compute(normalizeKey(key), (k, set) -> {
      if (set == null) {
        set = setSupplier.get();
      }
      added[0] = set.add(value);
      return set;
    });
    return added[0];
  }

  public boolean remove(String key, V value) {
    boolean[] removed = {false};
    delegate.computeIfPresent(normalizeKey(key), (k, set) -> {
      removed[0] = set.remove(value);
      return set.isEmpty() ? null : set;
    });
    return removed[0];
  }

  public Set<V> get(String key) {
    Set<V> set = delegate.get(normalizeKey(key));
    return set != null ? Collections.unmodifiableSet(set) : Collections.emptySet();
  }

  public boolean containsKey(String key) {
    Set<V> set = delegate.get(normalizeKey(key));
    return set != null && !set.isEmpty();
  }

  /**
   * Returns the total number of values across all keys.
   * <p>
   * Note: In concurrent scenarios, the returned value is a best-effort approximation.
   * </p>
   */
  public int size() {
    int size = 0;
    for (Set<V> set : delegate.values()) {
      size += set.size();
    }
    return size;
  }
}
