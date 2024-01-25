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
package com.ctrip.framework.apollo.biz.service;

import static com.ctrip.framework.apollo.biz.config.BizConfig.DEFAULT_RELEASE_HISTORY_RETENTION_SIZE;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.Audit;
import com.ctrip.framework.apollo.biz.entity.ReleaseHistory;
import com.ctrip.framework.apollo.biz.repository.ReleaseHistoryRepository;
import com.ctrip.framework.apollo.biz.repository.ReleaseRepository;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.google.common.collect.Queues;
import com.google.gson.Gson;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Service
public class ReleaseHistoryService {
  private static final Logger logger = LoggerFactory.getLogger(ReleaseHistoryService.class);
  private static final Gson GSON = new Gson();
  private static final int CLEAN_QUEUE_MAX_SIZE = 100;
  private final BlockingQueue<ReleaseHistory> releaseClearQueue = Queues.newLinkedBlockingQueue(CLEAN_QUEUE_MAX_SIZE);
  private final ExecutorService cleanExecutorService = Executors.newSingleThreadExecutor(
      ApolloThreadFactory.create("ReleaseHistoryService", true));
  private final AtomicBoolean cleanStopped = new AtomicBoolean(false);

  private final ReleaseHistoryRepository releaseHistoryRepository;
  private final ReleaseRepository releaseRepository;
  private final AuditService auditService;
  private final BizConfig bizConfig;
  private final TransactionTemplate transactionManager;

  public ReleaseHistoryService(
      final ReleaseHistoryRepository releaseHistoryRepository,
      final ReleaseRepository releaseRepository,
      final AuditService auditService,
      final BizConfig bizConfig,
      final TransactionTemplate transactionManager) {
    this.releaseHistoryRepository = releaseHistoryRepository;
    this.releaseRepository = releaseRepository;
    this.auditService = auditService;
    this.bizConfig = bizConfig;
    this.transactionManager = transactionManager;
  }

  @PostConstruct
  private void initialize() {
    cleanExecutorService.submit(() -> {
      while (!cleanStopped.get() && !Thread.currentThread().isInterrupted()) {
        try {
          ReleaseHistory releaseHistory = releaseClearQueue.poll(1, TimeUnit.SECONDS);
          if (releaseHistory != null) {
            this.cleanReleaseHistory(releaseHistory);
          } else {
            TimeUnit.MINUTES.sleep(1);
          }
        } catch (Throwable ex) {
          logger.error("Clean releaseHistory failed", ex);
          Tracer.logError(ex);
        }
      }
    });
  }

  public Page<ReleaseHistory> findReleaseHistoriesByNamespace(String appId, String clusterName,
                                                              String namespaceName, Pageable
                                                                  pageable) {
    return releaseHistoryRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(appId, clusterName,
                                                                                           namespaceName, pageable);
  }

  public Page<ReleaseHistory> findByReleaseIdAndOperation(long releaseId, int operation, Pageable page) {
    return releaseHistoryRepository.findByReleaseIdAndOperationOrderByIdDesc(releaseId, operation, page);
  }

  public Page<ReleaseHistory> findByPreviousReleaseIdAndOperation(long previousReleaseId, int operation, Pageable page) {
    return releaseHistoryRepository.findByPreviousReleaseIdAndOperationOrderByIdDesc(previousReleaseId, operation, page);
  }

  public Page<ReleaseHistory> findByReleaseIdAndOperationInOrderByIdDesc(long releaseId, Set<Integer> operations, Pageable page) {
    return releaseHistoryRepository.findByReleaseIdAndOperationInOrderByIdDesc(releaseId, operations, page);
  }

  @Transactional
  public ReleaseHistory createReleaseHistory(String appId, String clusterName, String
      namespaceName, String branchName, long releaseId, long previousReleaseId, int operation,
                                             Map<String, Object> operationContext, String operator) {
    ReleaseHistory releaseHistory = new ReleaseHistory();
    releaseHistory.setAppId(appId);
    releaseHistory.setClusterName(clusterName);
    releaseHistory.setNamespaceName(namespaceName);
    releaseHistory.setBranchName(branchName);
    releaseHistory.setReleaseId(releaseId);
    releaseHistory.setPreviousReleaseId(previousReleaseId);
    releaseHistory.setOperation(operation);
    if (operationContext == null) {
      releaseHistory.setOperationContext("{}"); //default empty object
    } else {
      releaseHistory.setOperationContext(GSON.toJson(operationContext));
    }
    releaseHistory.setDataChangeCreatedTime(new Date());
    releaseHistory.setDataChangeCreatedBy(operator);
    releaseHistory.setDataChangeLastModifiedBy(operator);

    releaseHistoryRepository.save(releaseHistory);

    auditService.audit(ReleaseHistory.class.getSimpleName(), releaseHistory.getId(),
                       Audit.OP.INSERT, releaseHistory.getDataChangeCreatedBy());

    int releaseHistoryRetentionLimit = this.getReleaseHistoryRetentionLimit(releaseHistory);
    if (releaseHistoryRetentionLimit != DEFAULT_RELEASE_HISTORY_RETENTION_SIZE) {
      if (!releaseClearQueue.offer(releaseHistory)) {
        logger.warn("releaseClearQueue is full, failed to add task to clean queue, " +
            "clean queue max size:{}", CLEAN_QUEUE_MAX_SIZE);
      }
    }
    return releaseHistory;
  }

  @Transactional
  public int batchDelete(String appId, String clusterName, String namespaceName, String operator) {
    return releaseHistoryRepository.batchDelete(appId, clusterName, namespaceName, operator);
  }

  private Optional<Long> releaseHistoryRetentionMaxId(ReleaseHistory releaseHistory, int releaseHistoryRetentionSize) {
    Page<ReleaseHistory> releaseHistoryPage = releaseHistoryRepository.findByAppIdAndClusterNameAndNamespaceNameAndBranchNameOrderByIdDesc(
        releaseHistory.getAppId(),
        releaseHistory.getClusterName(),
        releaseHistory.getNamespaceName(),
        releaseHistory.getBranchName(),
        PageRequest.of(releaseHistoryRetentionSize, 1)
    );
    if (releaseHistoryPage.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(
        releaseHistoryPage
        .getContent()
        .get(0)
        .getId()
    );
  }

  private void cleanReleaseHistory(ReleaseHistory cleanRelease) {
    String appId = cleanRelease.getAppId();
    String clusterName = cleanRelease.getClusterName();
    String namespaceName = cleanRelease.getNamespaceName();
    String branchName = cleanRelease.getBranchName();

    int retentionLimit = this.getReleaseHistoryRetentionLimit(cleanRelease);
    //Second check, if retentionLimit is default value, do not clean
    if (retentionLimit == DEFAULT_RELEASE_HISTORY_RETENTION_SIZE) {
      return;
    }

    Optional<Long> maxId = this.releaseHistoryRetentionMaxId(cleanRelease, retentionLimit);
    if (!maxId.isPresent()) {
      return;
    }

    boolean hasMore = true;
    while (hasMore && !Thread.currentThread().isInterrupted()) {
      List<ReleaseHistory> cleanReleaseHistoryList = releaseHistoryRepository.findFirst100ByAppIdAndClusterNameAndNamespaceNameAndBranchNameAndIdLessThanEqualOrderByIdAsc(
          appId, clusterName, namespaceName, branchName, maxId.get());
      Set<Long> releaseIds = cleanReleaseHistoryList.stream()
          .map(ReleaseHistory::getReleaseId)
          .collect(Collectors.toSet());

      transactionManager.execute(new TransactionCallbackWithoutResult() {
        @Override
        protected void doInTransactionWithoutResult(TransactionStatus status) {
          releaseHistoryRepository.deleteAll(cleanReleaseHistoryList);
          releaseRepository.deleteAllById(releaseIds);
        }
      });
      hasMore = cleanReleaseHistoryList.size() == 100;
    }
  }

  private int getReleaseHistoryRetentionLimit(ReleaseHistory releaseHistory) {
    String overrideKey = String.format("%s+%s+%s+%s", releaseHistory.getAppId(),
        releaseHistory.getClusterName(), releaseHistory.getNamespaceName(), releaseHistory.getBranchName());

    Map<String, Integer> overrideMap = bizConfig.releaseHistoryRetentionSizeOverride();
    return overrideMap.getOrDefault(overrideKey, bizConfig.releaseHistoryRetentionSize());
  }

  @PreDestroy
  void stopClean() {
    cleanStopped.set(true);
  }
}
