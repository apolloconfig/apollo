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
package com.ctrip.framework.foundation.internals;

import com.ctrip.framework.apollo.core.spi.Ordered;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class ServiceBootstrap {
  public static <S> S loadFirst(Class<S> clazz) {
    Iterator<S> iterator = loadAll(clazz);
    if (!iterator.hasNext()) {
      throw new IllegalStateException(
          String.format(
              "No implementation defined in /META-INF/services/%s, please check whether the file exists and has the right implementation class!",
              clazz.getName()));
    }
    return iterator.next();
  }

  public static <S> Iterator<S> loadAll(Class<S> clazz) {
    ServiceLoader<S> loader = ServiceLoader.load(clazz);

    return loader.iterator();
  }

  public static <S extends Ordered> List<S> loadAllOrdered(Class<S> clazz) {
    Iterator<S> iterator = loadAll(clazz);
    List<S> candidates = Lists.newArrayList(iterator);
    Collections.sort(
        candidates,
        new Comparator<S>() {
          @Override
          public int compare(S o1, S o2) {
            // the smaller order has higher priority
            return Integer.compare(o1.getOrder(), o2.getOrder());
          }
        });

    return candidates;
  }

  public static <S extends Ordered> S loadPrimary(Class<S> clazz) {
    List<S> candidates = loadAllOrdered(clazz);

    if (candidates.isEmpty()) {
      throw new IllegalStateException(
          String.format(
              "No implementation defined in /META-INF/services/%s, please check whether the file exists and has the right implementation class!",
              clazz.getName()));
    }

    return candidates.get(0);
  }
}
