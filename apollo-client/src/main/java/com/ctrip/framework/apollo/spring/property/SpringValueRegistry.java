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
package com.ctrip.framework.apollo.spring.property;

import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.collect.Multimaps;
import org.springframework.beans.factory.BeanFactory;

public class SpringValueRegistry {
  private static final long CLEAN_INTERVAL_IN_SECONDS = 5;
  private final Map<BeanFactory, Multimap<String, SpringValue>> registry = Maps.newConcurrentMap();
  private final AtomicBoolean initialized = new AtomicBoolean(false);
  private final Object LOCK = new Object();

  public void register(BeanFactory beanFactory, String key, SpringValue springValue) {
    if (!registry.containsKey(beanFactory)) {
      synchronized (LOCK) {
        if (!registry.containsKey(beanFactory)) {
          registry.put(beanFactory, Multimaps.synchronizedListMultimap(LinkedListMultimap.<String, SpringValue>create()));
        }
      }
    }

    registry.get(beanFactory).put(key, springValue);

    // lazy initialize
    if (initialized.compareAndSet(false, true)) {
      initialize();
    }
  }

  public Collection<SpringValue> get(BeanFactory beanFactory, String key) {
    Multimap<String, SpringValue> beanFactorySpringValues = registry.get(beanFactory);
    if (beanFactorySpringValues == null) {
      return null;
    }
    return beanFactorySpringValues.get(key);
  }

  private void initialize() {
    Executors.newSingleThreadScheduledExecutor(ApolloThreadFactory.create("SpringValueRegistry", true)).scheduleAtFixedRate(
        new Runnable() {
          @Override
          public void run() {
            try {
              scanAndClean();
            } catch (Throwable ex) {
              ex.printStackTrace();
            }
          }
        }, CLEAN_INTERVAL_IN_SECONDS, CLEAN_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
  }

  private void scanAndClean() {
    Iterator<Multimap<String, SpringValue>> iterator = registry.values().iterator();
    while (!Thread.currentThread().isInterrupted() && iterator.hasNext()) {
      Multimap<String, SpringValue> springValues = iterator.next();
      Iterator<Entry<String, SpringValue>> springValueIterator = springValues.entries().iterator();
      while (springValueIterator.hasNext()) {
        Entry<String, SpringValue> springValue = springValueIterator.next();
        if (!springValue.getValue().isTargetBeanValid()) {
          // clear unused spring values
          springValueIterator.remove();
        }
      }
    }
  }
}
