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
package com.ctrip.framework.apollo.biz.utils;

import static org.junit.Assert.assertEquals;

import com.ctrip.framework.apollo.biz.MockBeanFactory;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.google.common.collect.Sets;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ReleaseKeyGeneratorTest {
  private static final Logger logger = LoggerFactory.getLogger(ReleaseKeyGeneratorTest.class);

  @Test
  public void testGenerateReleaseKey() throws Exception {
    String someAppId = "someAppId";
    String someCluster = "someCluster";
    String someNamespace = "someNamespace";

    String anotherAppId = "anotherAppId";

    Namespace namespace = MockBeanFactory.mockNamespace(someAppId, someCluster, someNamespace);
    Namespace anotherNamespace =
        MockBeanFactory.mockNamespace(anotherAppId, someCluster, someNamespace);
    int generateTimes = 50000;
    Set<String> releaseKeys = Sets.newConcurrentHashSet();

    ExecutorService executorService = Executors.newFixedThreadPool(2);
    CountDownLatch latch = new CountDownLatch(1);

    executorService.submit(generateReleaseKeysTask(namespace, releaseKeys, generateTimes, latch));
    executorService.submit(
        generateReleaseKeysTask(anotherNamespace, releaseKeys, generateTimes, latch));

    latch.countDown();

    executorService.shutdown();
    executorService.awaitTermination(10, TimeUnit.SECONDS);

    // make sure keys are unique
    assertEquals(generateTimes * 2, releaseKeys.size());
  }

  private Runnable generateReleaseKeysTask(
      Namespace namespace, Set<String> releaseKeys, int generateTimes, CountDownLatch latch) {
    return () -> {
      try {
        latch.await();
      } catch (InterruptedException e) {
        // ignore
      }
      for (int i = 0; i < generateTimes; i++) {
        releaseKeys.add(ReleaseKeyGenerator.generateReleaseKey(namespace));
      }
    };
  }
}
