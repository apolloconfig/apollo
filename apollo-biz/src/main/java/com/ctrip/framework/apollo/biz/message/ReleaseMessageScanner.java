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
package com.ctrip.framework.apollo.biz.message;

import com.google.common.collect.Maps;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.repository.ReleaseMessageRepository;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.google.common.collect.Lists;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ReleaseMessageScanner implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(ReleaseMessageScanner.class);
  private static final int missingReleaseMessageMaxAge = 10; // hardcoded to 10, could be configured via BizConfig if necessary
  private final BizConfig bizConfig;
  private final ReleaseMessageRepository releaseMessageRepository;
  private int databaseScanInterval;
  private final List<ReleaseMessageListener> listeners;
  private final ScheduledExecutorService executorService;
  private final Map<Long, Integer> missingReleaseMessages; // missing release message id => age counter
  private long maxIdScanned;

  public ReleaseMessageScanner(final BizConfig bizConfig,
      final ReleaseMessageRepository releaseMessageRepository) {
    this.bizConfig = bizConfig;
    this.releaseMessageRepository = releaseMessageRepository;
    listeners = Lists.newCopyOnWriteArrayList();
    executorService = Executors.newScheduledThreadPool(1, ApolloThreadFactory
        .create("ReleaseMessageScanner", true));
    missingReleaseMessages = Maps.newHashMap();
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    databaseScanInterval = bizConfig.releaseMessageScanIntervalInMilli();
    maxIdScanned = loadLargestMessageId();
    executorService.scheduleWithFixedDelay(() -> {
      Transaction transaction = Tracer.newTransaction("Apollo.ReleaseMessageScanner", "scanMessage");
      try {
        scanMissingMessages();
        scanMessages();
        transaction.setStatus(Transaction.SUCCESS);
      } catch (Throwable ex) {
        transaction.setStatus(ex);
        logger.error("Scan and send message failed", ex);
      } finally {
        transaction.complete();
      }
    }, databaseScanInterval, databaseScanInterval, TimeUnit.MILLISECONDS);

  }

  /**
   * add message listeners for release message
   * @param listener
   */
  public void addMessageListener(ReleaseMessageListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Scan messages, continue scanning until there is no more messages
   */
  private void scanMessages() {
    boolean hasMoreMessages = true;
    while (hasMoreMessages && !Thread.currentThread().isInterrupted()) {
      hasMoreMessages = scanAndSendMessages();
    }
  }

  /**
   * scan messages and send
   *
   * @return whether there are more messages
   */
  private boolean scanAndSendMessages() {
    //current batch is 500
    List<ReleaseMessage> releaseMessages =
        releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(maxIdScanned);
    if (CollectionUtils.isEmpty(releaseMessages)) {
      return false;
    }
    fireMessageScanned(releaseMessages);
    int messageScanned = releaseMessages.size();
    long newMaxIdScanned = releaseMessages.get(messageScanned - 1).getId();
    // check id gaps, possible reasons are release message not committed yet or already rolled back
    if (newMaxIdScanned - maxIdScanned > messageScanned) {
      recordMissingReleaseMessageIds(releaseMessages, maxIdScanned);
    }
    maxIdScanned = newMaxIdScanned;
    return messageScanned == 500;
  }

  private void scanMissingMessages() {
    Set<Long> missingReleaseMessageIds = missingReleaseMessages.keySet();
    Iterable<ReleaseMessage> releaseMessages = releaseMessageRepository
        .findAllById(missingReleaseMessageIds);
    fireMessageScanned(releaseMessages);
    releaseMessages.forEach(releaseMessage -> {
      missingReleaseMessageIds.remove(releaseMessage.getId());
    });
    growAndCleanMissingMessages();
  }

  private void growAndCleanMissingMessages() {
    Iterator<Entry<Long, Integer>> iterator = missingReleaseMessages.entrySet()
        .iterator();
    while (iterator.hasNext()) {
      Entry<Long, Integer> entry = iterator.next();
      if (entry.getValue() > missingReleaseMessageMaxAge) {
        iterator.remove();
      } else {
        entry.setValue(entry.getValue() + 1);
      }
    }
  }

  private void recordMissingReleaseMessageIds(List<ReleaseMessage> messages, long startId) {
    for (ReleaseMessage message : messages) {
      long currentId = message.getId();
      if (currentId - startId > 1) {
        for (long i = startId + 1; i < currentId; i++) {
          missingReleaseMessages.putIfAbsent(i, 1);
        }
      }
      startId = currentId;
    }
  }

  /**
   * find largest message id as the current start point
   * @return current largest message id
   */
  private long loadLargestMessageId() {
    ReleaseMessage releaseMessage = releaseMessageRepository.findTopByOrderByIdDesc();
    return releaseMessage == null ? 0 : releaseMessage.getId();
  }

  /**
   * Notify listeners with messages loaded
   * @param messages
   */
  private void fireMessageScanned(Iterable<ReleaseMessage> messages) {
    for (ReleaseMessage message : messages) {
      for (ReleaseMessageListener listener : listeners) {
        try {
          listener.handleMessage(message, Topics.APOLLO_RELEASE_TOPIC);
        } catch (Throwable ex) {
          Tracer.logError(ex);
          logger.error("Failed to invoke message listener {}", listener.getClass(), ex);
        }
      }
    }
  }
}
