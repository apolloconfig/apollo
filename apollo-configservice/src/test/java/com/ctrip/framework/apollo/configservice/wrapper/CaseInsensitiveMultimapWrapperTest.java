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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Before;
import org.junit.Test;

public class CaseInsensitiveMultimapWrapperTest {

  private CaseInsensitiveMultimapWrapper<String> multimap;

  @Before
  public void setUp() throws Exception {
    multimap =
        new CaseInsensitiveMultimapWrapper<>(Maps.newConcurrentMap(), Sets::newConcurrentHashSet);
  }

  @Test
  public void testPutAndGet() {
    String key = "SomeKey";
    String value1 = "value1";
    String value2 = "value2";

    assertTrue(multimap.put(key, value1));
    assertTrue(multimap.put(key.toLowerCase(), value2));
    assertFalse(multimap.put(key.toUpperCase(), value1)); // already exists

    Set<String> values = multimap.get(key);
    assertEquals(2, values.size());
    assertTrue(values.contains(value1));
    assertTrue(values.contains(value2));

    Set<String> valuesFromLower = multimap.get(key.toLowerCase());
    assertEquals(values, valuesFromLower);
  }

  @Test
  public void testRemove() {
    String key = "SomeKey";
    String value = "someValue";

    multimap.put(key, value);
    assertTrue(multimap.containsKey(key));

    assertTrue(multimap.remove(key.toUpperCase(), value));
    assertFalse(multimap.containsKey(key));
    assertTrue(multimap.get(key).isEmpty());
  }

  @Test
  public void testContainsKey() {
    String key = "SomeKey";
    String value = "someValue";

    assertFalse(multimap.containsKey(key));
    multimap.put(key, value);
    assertTrue(multimap.containsKey(key.toLowerCase()));
    assertTrue(multimap.containsKey(key.toUpperCase()));
  }

  @Test
  public void testSize() {
    multimap.put("Key1", "v1");
    multimap.put("key1", "v2");
    multimap.put("Key2", "v3");

    assertEquals(3, multimap.size());

    multimap.remove("KEY1", "v1");
    assertEquals(2, multimap.size());
  }

  @Test
  public void testGetEmpty() {
    assertTrue(multimap.get("nonExistent").isEmpty());
  }

  @Test
  public void testConcurrencyRaceCondition() throws InterruptedException {
    final int loopCount = 100000;
    final String key = "RaceKey";
    final String valA = "A";
    final String valB = "B";
    final CountDownLatch latch = new CountDownLatch(2);
    final AtomicBoolean running = new AtomicBoolean(true);
    final AtomicInteger failures = new AtomicInteger(0);
    final AtomicReference<Throwable> threadException = new AtomicReference<>();

    // Thread A: toggles valA
    new Thread(() -> {
      try {
        while (running.get()) {
          multimap.put(key, valA);
          multimap.remove(key, valA);
        }
      } catch (Throwable e) {
        threadException.set(e);
      } finally {
        latch.countDown();
      }
    }).start();

    // Thread B: repeatedly adds and removes valB to test put atomicity
    new Thread(() -> {
      try {
        for (int i = 0; i < loopCount; i++) {
          multimap.put(key, valB);
          if (!multimap.get(key).contains(valB)) {
            failures.incrementAndGet();
          }
          // Remove B to allow the set to become empty again,
          // giving Thread A a chance to trigger the map removal race condition again.
          multimap.remove(key, valB);
        }
      } catch (Throwable e) {
        threadException.set(e);
      } finally {
        running.set(false);
        latch.countDown();
      }
    }).start();

    latch.await();

    if (threadException.get() != null) {
      throw new RuntimeException("Exception in worker thread", threadException.get());
    }

    assertEquals("Value B should not be lost due to race condition", 0, failures.get());
  }

  @Test
  public void testGetReturnsUnmodifiableView() {
    assertTrue(multimap.put("Key", "Value"));

    Set<String> set = multimap.get("key");
    assertTrue(set.contains("Value"));

    try {
      set.add("Another");
      org.junit.Assert.fail("get() should return an unmodifiable view");
    } catch (UnsupportedOperationException expected) {
      // expected
    }
  }
}
