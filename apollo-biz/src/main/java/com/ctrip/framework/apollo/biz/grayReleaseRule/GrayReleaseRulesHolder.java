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
package com.ctrip.framework.apollo.biz.grayReleaseRule;

import com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.ctrip.framework.apollo.biz.entity.GrayReleaseRule;
import com.ctrip.framework.apollo.biz.entity.ReleaseMessage;
import com.ctrip.framework.apollo.biz.message.ReleaseMessageListener;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.repository.GrayReleaseRuleRepository;
import com.ctrip.framework.apollo.common.constants.NamespaceBranchStatus;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.common.utils.GrayReleaseRuleItemTransformer;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;

import com.google.common.collect.TreeMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class GrayReleaseRulesHolder implements ReleaseMessageListener, InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(GrayReleaseRulesHolder.class);
  private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);

  private final GrayReleaseRuleRepository grayReleaseRuleRepository;
  private final BizConfig bizConfig;

  private int databaseScanInterval;
  private ScheduledExecutorService executorService;
  //store configAppId+configCluster+configNamespace -> GrayReleaseRuleCache map
  private Multimap<String, GrayReleaseRuleCache> grayReleaseRuleCache;
  //store clientAppId+clientNamespace+ip -> ruleId map
  private Multimap<String, Long> reversedGrayReleaseRuleCache;
  //store clientAppId+clientNamespace+label -> ruleId map
  private Multimap<String, Long> reversedGrayReleaseRuleLabelCache;
  //an auto increment version to indicate the age of rules
  private AtomicLong loadVersion;

  public GrayReleaseRulesHolder(final GrayReleaseRuleRepository grayReleaseRuleRepository,
      final BizConfig bizConfig) {
    this.grayReleaseRuleRepository = grayReleaseRuleRepository;
    this.bizConfig = bizConfig;
    loadVersion = new AtomicLong();
    grayReleaseRuleCache = Multimaps.synchronizedSetMultimap(
        TreeMultimap.create(String.CASE_INSENSITIVE_ORDER, Ordering.natural()));
    reversedGrayReleaseRuleCache = Multimaps.synchronizedSetMultimap(
        TreeMultimap.create(String.CASE_INSENSITIVE_ORDER, Ordering.natural()));
    reversedGrayReleaseRuleLabelCache = Multimaps.synchronizedSetMultimap(
        TreeMultimap.create(String.CASE_INSENSITIVE_ORDER, Ordering.natural()));
    executorService = Executors.newScheduledThreadPool(1, ApolloThreadFactory
        .create("GrayReleaseRulesHolder", true));
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    populateDataBaseInterval();
    //force sync load for the first time
    periodicScanRules();
    executorService.scheduleWithFixedDelay(this::periodicScanRules,
        getDatabaseScanIntervalSecond(), getDatabaseScanIntervalSecond(), getDatabaseScanTimeUnit()
    );
  }

  @Override
  public void handleMessage(ReleaseMessage message, String channel) {
    logger.info("message received - channel: {}, message: {}", channel, message);
    String releaseMessage = message.getMessage();
    if (!Topics.APOLLO_RELEASE_TOPIC.equals(channel) || Strings.isNullOrEmpty(releaseMessage)) {
      return;
    }
    List<String> keys = ReleaseMessageKeyGenerator.messageToList(releaseMessage);
    //message should be appId+cluster+namespace
    if (CollectionUtils.isEmpty(keys)) {
      return;
    }
    String appId = keys.get(0);
    String cluster = keys.get(1);
    String namespace = keys.get(2);

    List<GrayReleaseRule> rules = grayReleaseRuleRepository
        .findByAppIdAndClusterNameAndNamespaceName(appId, cluster, namespace);

    mergeGrayReleaseRules(rules);
  }

  private void periodicScanRules() {
    Transaction transaction = Tracer.newTransaction("Apollo.GrayReleaseRulesScanner",
        "scanGrayReleaseRules");
    try {
      loadVersion.incrementAndGet();
      scanGrayReleaseRules();
      transaction.setStatus(Transaction.SUCCESS);
    } catch (Throwable ex) {
      transaction.setStatus(ex);
      logger.error("Scan gray release rule failed", ex);
    } finally {
      transaction.complete();
    }
  }

  public Long findReleaseIdFromGrayReleaseRule(String clientAppId, String clientIp, String clientLabel, String
      configAppId, String configCluster, String configNamespaceName) {
    String key = assembleGrayReleaseRuleKey(configAppId, configCluster, configNamespaceName);
    if (!grayReleaseRuleCache.containsKey(key)) {
      return null;
    }
    //create a new list to avoid ConcurrentModificationException
    List<GrayReleaseRuleCache> rules = Lists.newArrayList(grayReleaseRuleCache.get(key));
    for (GrayReleaseRuleCache rule : rules) {
      //check branch status
      if (rule.getBranchStatus() != NamespaceBranchStatus.ACTIVE) {
        continue;
      }
      if (rule.matches(clientAppId, clientIp, clientLabel)) {
        return rule.getReleaseId();
      }
    }
    return null;
  }

  /**
   * Check whether there are gray release rules for the clientAppId, clientIp, clientLabel, namespace combination.
   * Please note that even there are gray release rules, it doesn't mean it will always load gray
   * releases. Because gray release rules actually apply to one more dimension - cluster.
   */
  public boolean hasGrayReleaseRule(String clientAppId, String clientIp, String clientLabel,
      String namespaceName) {
    // check ip gray rule
    if (reversedGrayReleaseRuleCache.containsKey(assembleReversedGrayReleaseRuleKey(clientAppId,
        namespaceName, clientIp)) || reversedGrayReleaseRuleCache.containsKey
        (assembleReversedGrayReleaseRuleKey(clientAppId, namespaceName, GrayReleaseRuleItemDTO
            .ALL_IP))) {
      return true;
    }
    // check label gray rule
    if (!Strings.isNullOrEmpty(clientLabel) &&
        (reversedGrayReleaseRuleLabelCache.containsKey(
            assembleReversedGrayReleaseRuleKey(clientAppId, namespaceName, clientLabel)) ||
            reversedGrayReleaseRuleLabelCache.containsKey(
                assembleReversedGrayReleaseRuleKey(clientAppId, namespaceName,
                    GrayReleaseRuleItemDTO.ALL_Label)))) {
      return true;
    }
    return false;
  }

  private void scanGrayReleaseRules() {
    long maxIdScanned = 0;
    boolean hasMore = true;

    while (hasMore && !Thread.currentThread().isInterrupted()) {
      List<GrayReleaseRule> grayReleaseRules = grayReleaseRuleRepository
          .findFirst500ByIdGreaterThanOrderByIdAsc(maxIdScanned);
      if (CollectionUtils.isEmpty(grayReleaseRules)) {
        break;
      }
      mergeGrayReleaseRules(grayReleaseRules);
      int rulesScanned = grayReleaseRules.size();
      maxIdScanned = grayReleaseRules.get(rulesScanned - 1).getId();
      //batch is 500
      hasMore = rulesScanned == 500;
    }
  }

  private void mergeGrayReleaseRules(List<GrayReleaseRule> grayReleaseRules) {
    if (CollectionUtils.isEmpty(grayReleaseRules)) {
      return;
    }
    for (GrayReleaseRule grayReleaseRule : grayReleaseRules) {
      if (grayReleaseRule.getReleaseId() == null || grayReleaseRule.getReleaseId() == 0) {
        //filter rules with no release id, i.e. never released
        continue;
      }
      String key = assembleGrayReleaseRuleKey(grayReleaseRule.getAppId(), grayReleaseRule
          .getClusterName(), grayReleaseRule.getNamespaceName());
      //create a new list to avoid ConcurrentModificationException
      List<GrayReleaseRuleCache> rules = Lists.newArrayList(grayReleaseRuleCache.get(key));
      GrayReleaseRuleCache oldRule = null;
      for (GrayReleaseRuleCache ruleCache : rules) {
        if (ruleCache.getBranchName().equals(grayReleaseRule.getBranchName())) {
          oldRule = ruleCache;
          break;
        }
      }

      //if old rule is null and new rule's branch status is not active, ignore
      if (oldRule == null && grayReleaseRule.getBranchStatus() != NamespaceBranchStatus.ACTIVE) {
        continue;
      }

      //use id comparison to avoid synchronization
      if (oldRule == null || grayReleaseRule.getId() > oldRule.getRuleId()) {
        addCache(key, transformRuleToRuleCache(grayReleaseRule));
        if (oldRule != null) {
          removeCache(key, oldRule);
        }
      } else {
        if (oldRule.getBranchStatus() == NamespaceBranchStatus.ACTIVE) {
          //update load version
          oldRule.setLoadVersion(loadVersion.get());
        } else if ((loadVersion.get() - oldRule.getLoadVersion()) > 1) {
          //remove outdated inactive branch rule after 2 update cycles
          removeCache(key, oldRule);
        }
      }
    }
  }

  private void addCache(String key, GrayReleaseRuleCache ruleCache) {
    if (ruleCache.getBranchStatus() == NamespaceBranchStatus.ACTIVE) {
      for (GrayReleaseRuleItemDTO ruleItemDTO : ruleCache.getRuleItems()) {
        for (String clientIp : ruleItemDTO.getClientIpList()) {
          reversedGrayReleaseRuleCache.put(assembleReversedGrayReleaseRuleKey(ruleItemDTO
              .getClientAppId(), ruleCache.getNamespaceName(), clientIp), ruleCache.getRuleId());
        }
        for (String label : ruleItemDTO.getClientLabelList()) {
          reversedGrayReleaseRuleLabelCache.put(assembleReversedGrayReleaseRuleKey(ruleItemDTO
              .getClientAppId(), ruleCache.getNamespaceName(), label), ruleCache.getRuleId());
        }
      }
    }
    grayReleaseRuleCache.put(key, ruleCache);
  }

  private void removeCache(String key, GrayReleaseRuleCache ruleCache) {
    grayReleaseRuleCache.remove(key, ruleCache);
    for (GrayReleaseRuleItemDTO ruleItemDTO : ruleCache.getRuleItems()) {
      for (String clientIp : ruleItemDTO.getClientIpList()) {
        reversedGrayReleaseRuleCache.remove(assembleReversedGrayReleaseRuleKey(ruleItemDTO
            .getClientAppId(), ruleCache.getNamespaceName(), clientIp), ruleCache.getRuleId());
      }
      for (String label : ruleItemDTO.getClientLabelList()) {
        reversedGrayReleaseRuleLabelCache.remove(assembleReversedGrayReleaseRuleKey(ruleItemDTO
            .getClientAppId(), ruleCache.getNamespaceName(), label), ruleCache.getRuleId());
      }
    }
  }

  private GrayReleaseRuleCache transformRuleToRuleCache(GrayReleaseRule grayReleaseRule) {
    Set<GrayReleaseRuleItemDTO> ruleItems;
    try {
      ruleItems = GrayReleaseRuleItemTransformer.batchTransformFromJSON(grayReleaseRule.getRules());
    } catch (Throwable ex) {
      ruleItems = Sets.newHashSet();
      Tracer.logError(ex);
      logger.error("parse rule for gray release rule {} failed", grayReleaseRule.getId(), ex);
    }

    GrayReleaseRuleCache ruleCache = new GrayReleaseRuleCache(grayReleaseRule.getId(),
        grayReleaseRule.getBranchName(), grayReleaseRule.getNamespaceName(), grayReleaseRule
        .getReleaseId(), grayReleaseRule.getBranchStatus(), loadVersion.get(), ruleItems);

    return ruleCache;
  }

  private void populateDataBaseInterval() {
    databaseScanInterval = bizConfig.grayReleaseRuleScanInterval();
  }

  private int getDatabaseScanIntervalSecond() {
    return databaseScanInterval;
  }

  private TimeUnit getDatabaseScanTimeUnit() {
    return TimeUnit.SECONDS;
  }

  private String assembleGrayReleaseRuleKey(String configAppId, String configCluster, String
      configNamespaceName) {
    return STRING_JOINER.join(configAppId, configCluster, configNamespaceName);
  }

  private String assembleReversedGrayReleaseRuleKey(String clientAppId, String
      clientNamespaceName, String clientIpOrLabel) {
    return STRING_JOINER.join(clientAppId, clientNamespaceName, clientIpOrLabel);
  }

}
