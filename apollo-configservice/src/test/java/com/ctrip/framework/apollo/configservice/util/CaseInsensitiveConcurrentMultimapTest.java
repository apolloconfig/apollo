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

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Test for CaseInsensitiveConcurrentMultimap
 * 
 * @author Apollo Team
 */
public class CaseInsensitiveConcurrentMultimapTest {
  private CaseInsensitiveConcurrentMultimap<String, String> multimap;

  @Before
  public void setUp() {
    multimap = new CaseInsensitiveConcurrentMultimap<>();
  }

  @Test
  public void testBasicOperations() {
    // Test put
    assertTrue(multimap.put("key1", "value1"));
    assertTrue(multimap.put("key1", "value2"));
    assertFalse(multimap.put("key1", "value1")); // duplicate value

    // Test get
    Collection<String> values = multimap.get("key1");
    assertEquals(2, values.size());
    assertTrue(values.contains("value1"));
    assertTrue(values.contains("value2"));

    // Test containsKey
    assertTrue(multimap.containsKey("key1"));
    assertFalse(multimap.containsKey("nonexistent"));

    // Test remove
    assertTrue(multimap.remove("key1", "value1"));
    assertFalse(multimap.remove("key1", "value1")); // already removed
    
    values = multimap.get("key1");
    assertEquals(1, values.size());
    assertTrue(values.contains("value2"));
  }

  @Test
  public void testCaseInsensitivity() {
    multimap.put("KEY1", "value1");
    multimap.put("key1", "value2");
    multimap.put("Key1", "value3");

    Collection<String> values = multimap.get("key1");
    assertEquals(3, values.size());
    assertTrue(values.contains("value1"));
    assertTrue(values.contains("value2"));
    assertTrue(values.contains("value3"));

    // Test case insensitive containsKey
    assertTrue(multimap.containsKey("KEY1"));
    assertTrue(multimap.containsKey("key1"));
    assertTrue(multimap.containsKey("Key1"));

    // Test case insensitive remove
    assertTrue(multimap.remove("KEY1", "value1"));
    values = multimap.get("key1");
    assertEquals(2, values.size());
    assertFalse(values.contains("value1"));
  }

  @Test
  public void testNullHandling() {
    assertFalse(multimap.put(null, "value"));
    assertFalse(multimap.put("key", null));
    assertFalse(multimap.remove(null, "value"));
    assertFalse(multimap.remove("key", null));
    
    assertTrue(multimap.get(null).isEmpty());
    assertFalse(multimap.containsKey(null));
  }

  @Test
  public void testEmptyOperations() {
    assertTrue(multimap.isEmpty());
    assertEquals(0, multimap.size());
    assertTrue(multimap.get("nonexistent").isEmpty());
    assertFalse(multimap.containsKey("nonexistent"));
    assertTrue(multimap.values().isEmpty());
  }

  @Test
  public void testClearAndCleanup() {
    multimap.put("key1", "value1");
    multimap.put("key2", "value2");
    
    assertFalse(multimap.isEmpty());
    assertEquals(2, multimap.size());
    
    multimap.clear();
    
    assertTrue(multimap.isEmpty());
    assertEquals(0, multimap.size());
    assertTrue(multimap.values().isEmpty());
  }

  @Test
  public void testConcurrentAccess() throws InterruptedException {
    final int threadCount = 10;
    final int operationsPerThread = 100;
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch endLatch = new CountDownLatch(threadCount);
    final AtomicInteger successfulPuts = new AtomicInteger(0);
    final AtomicInteger successfulRemoves = new AtomicInteger(0);
    
    ExecutorService executor = Executors.newFixedThreadPool(threadCount);
    
    // Start multiple threads that perform concurrent operations
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      executor.submit(() -> {
        try {
          startLatch.await();
          
          for (int j = 0; j < operationsPerThread; j++) {
            String key = "key" + (j % 5); // Use 5 different keys
            String value = "thread" + threadId + "_value" + j;
            
            if (multimap.put(key, value)) {
              successfulPuts.incrementAndGet();
            }
            
            // Occasionally remove values
            if (j % 10 == 0) {
              Collection<String> values = multimap.get(key);
              if (!values.isEmpty()) {
                String valueToRemove = values.iterator().next();
                if (multimap.remove(key, valueToRemove)) {
                  successfulRemoves.incrementAndGet();
                }
              }
            }
          }
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        } finally {
          endLatch.countDown();
        }
      });
    }
    
    // Start all threads at once
    startLatch.countDown();
    
    // Wait for all threads to complete
    assertTrue("All threads should complete within 10 seconds", 
               endLatch.await(10, TimeUnit.SECONDS));
    
    executor.shutdown();
    
    // Verify that operations completed successfully
    assertTrue("Should have successful puts", successfulPuts.get() > 0);
    assertTrue("Total operations should be reasonable", 
               successfulPuts.get() <= threadCount * operationsPerThread);
    
    // Verify multimap is in consistent state
    int totalValues = multimap.values().size();
    assertEquals("Size should match actual values", totalValues, multimap.size());
  }

  @Test
  public void testMemoryCleanup() {
    // Test that empty sets are cleaned up to prevent memory leaks
    multimap.put("key1", "value1");
    assertTrue(multimap.containsKey("key1"));
    
    multimap.remove("key1", "value1");
    assertFalse(multimap.containsKey("key1"));
    assertTrue(multimap.get("key1").isEmpty());
  }

  @Test
  public void testConcurrentPutAndRemoveRaceCondition() throws InterruptedException {
    // This test specifically targets the race condition described in the issue:
    // Thread A checks if values set is empty
    // Thread B retrieves the values set
    // Thread A removes the values set from the map
    // Thread B adds a value to the now-removed set
    
    final int iterations = 1000;
    final String testKey = "testKey";
    final CountDownLatch startLatch = new CountDownLatch(1);
    final CountDownLatch endLatch = new CountDownLatch(2);
    final AtomicInteger putSuccessCount = new AtomicInteger(0);
    final AtomicInteger removeSuccessCount = new AtomicInteger(0);
    
    ExecutorService executor = Executors.newFixedThreadPool(2);
    
    // Thread that continuously adds and removes the same value (causing set to become empty)
    executor.submit(() -> {
      try {
        startLatch.await();
        for (int i = 0; i < iterations; i++) {
          multimap.put(testKey, "removeValue");
          if (multimap.remove(testKey, "removeValue")) {
            removeSuccessCount.incrementAndGet();
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        endLatch.countDown();
      }
    });
    
    // Thread that continuously adds values
    executor.submit(() -> {
      try {
        startLatch.await();
        for (int i = 0; i < iterations; i++) {
          if (multimap.put(testKey, "putValue" + i)) {
            putSuccessCount.incrementAndGet();
          }
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } finally {
        endLatch.countDown();
      }
    });
    
    // Start both threads
    startLatch.countDown();
    
    // Wait for completion
    assertTrue("All threads should complete within 10 seconds", 
               endLatch.await(10, TimeUnit.SECONDS));
    
    executor.shutdown();
    
    // Verify that no values were lost
    // All successfully added values should either still be in the map or have been explicitly removed
    Collection<String> remainingValues = multimap.get(testKey);
    
    // Count how many unique put values are still in the map
    int remainingPutValues = 0;
    for (String value : remainingValues) {
      if (value.startsWith("putValue")) {
        remainingPutValues++;
      }
    }
    
    // The key insight: if the race condition existed, values would be lost
    // (added to a removed set). With the fix using compute(), all successfully
    // added values must be accounted for (either still in map or were in it).
    // Since we never explicitly remove "putValue*" entries, they should all be present.
    assertEquals("All put values should be in the multimap", 
                 putSuccessCount.get(), remainingPutValues);
    
    assertTrue("Should have successful puts", putSuccessCount.get() > 0);
    assertTrue("Should have successful removes", removeSuccessCount.get() > 0);
  }
}