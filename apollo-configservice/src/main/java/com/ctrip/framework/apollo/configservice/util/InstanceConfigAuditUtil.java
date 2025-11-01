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
package com.ctrip.framework.apollo.configservice.util;

import com.ctrip.framework.apollo.biz.config.BizConfig;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;

import com.ctrip.framework.apollo.biz.entity.Instance;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;
import com.ctrip.framework.apollo.biz.service.InstanceService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.tracer.Tracer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.GuavaCacheMetrics;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Service
public class InstanceConfigAuditUtil implements InitializingBean {

  private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
  private final ExecutorService auditExecutorService;
  private final AtomicBoolean auditStopped;
  private BlockingQueue<InstanceConfigAuditModel> audits;
  private Cache<String, Long> instanceCache;
  private Cache<String, String> instanceConfigReleaseKeyCache;

  private final InstanceService instanceService;
  private final BizConfig bizConfig;
  private final MeterRegistry meterRegistry;

  public InstanceConfigAuditUtil(final InstanceService instanceService, final BizConfig bizConfig,
      final MeterRegistry meterRegistry) {
    this.instanceService = instanceService;
    this.bizConfig = bizConfig;
    this.meterRegistry = meterRegistry;

    audits = Queues.newLinkedBlockingQueue(this.bizConfig.getInstanceConfigAuditMaxSize());
    auditExecutorService = Executors
        .newSingleThreadExecutor(ApolloThreadFactory.create("InstanceConfigAuditUtil", true));
    auditStopped = new AtomicBoolean(false);
    buildInstanceCache();
    buildInstanceConfigReleaseKeyCache();
  }

  public boolean audit(String appId, String clusterName, String dataCenter, String ip,
      String configAppId, String configClusterName, String configNamespace, String releaseKey) {
    return this.audits.offer(new InstanceConfigAuditModel(appId, clusterName, dataCenter, ip,
        configAppId, configClusterName, configNamespace, releaseKey));
  }

  void doAudit(InstanceConfigAuditModel auditModel) {
    String instanceCacheKey = assembleInstanceKey(auditModel.getAppId(),
        auditModel.getClusterName(), auditModel.getIp(), auditModel.getDataCenter());
    Long instanceId = instanceCache.getIfPresent(instanceCacheKey);
    if (instanceId == null) {
      instanceId = prepareInstanceId(auditModel);
      instanceCache.put(instanceCacheKey, instanceId);
    }

    // load instance config release key from cache, and check if release key is the same
    String instanceConfigCacheKey = assembleInstanceConfigKey(instanceId,
        auditModel.getConfigAppId(), auditModel.getConfigNamespace());
    String cacheReleaseKey = instanceConfigReleaseKeyCache.getIfPresent(instanceConfigCacheKey);

    // if release key is the same, then skip audit
    if (cacheReleaseKey != null && Objects.equals(cacheReleaseKey, auditModel.getReleaseKey())) {
      return;
    }

    instanceConfigReleaseKeyCache.put(instanceConfigCacheKey, auditModel.getReleaseKey());

    // if release key is not the same or cannot find in cache, then do audit
    InstanceConfig instanceConfig = instanceService.findInstanceConfig(instanceId,
        auditModel.getConfigAppId(), auditModel.getConfigNamespace());

    if (instanceConfig != null) {
      if (!Objects.equals(instanceConfig.getReleaseKey(), auditModel.getReleaseKey())) {
        instanceConfig.setConfigClusterName(auditModel.getConfigClusterName());
        instanceConfig.setReleaseKey(auditModel.getReleaseKey());
        instanceConfig.setReleaseDeliveryTime(auditModel.getOfferTime());
      } else if (offerTimeAndLastModifiedTimeCloseEnough(auditModel.getOfferTime(),
          instanceConfig.getDataChangeLastModifiedTime())) {
        // when releaseKey is the same, optimize to reduce writes if the record was updated not long
        // ago
        return;
      }
      // we need to update no matter the release key is the same or not, to ensure the
      // last modified time is updated each day
      instanceConfig.setDataChangeLastModifiedTime(auditModel.getOfferTime());
      instanceService.updateInstanceConfig(instanceConfig);
      return;
    }

    instanceConfig = new InstanceConfig();
    instanceConfig.setInstanceId(instanceId);
    instanceConfig.setConfigAppId(auditModel.getConfigAppId());
    instanceConfig.setConfigClusterName(auditModel.getConfigClusterName());
    instanceConfig.setConfigNamespaceName(auditModel.getConfigNamespace());
    instanceConfig.setReleaseKey(auditModel.getReleaseKey());
    instanceConfig.setReleaseDeliveryTime(auditModel.getOfferTime());
    instanceConfig.setDataChangeCreatedTime(auditModel.getOfferTime());

    try {
      instanceService.createInstanceConfig(instanceConfig);
    } catch (DataIntegrityViolationException ex) {
      // concurrent insertion, safe to ignore
    }
  }

  private boolean offerTimeAndLastModifiedTimeCloseEnough(Date offerTime, Date lastModifiedTime) {
    return (offerTime.getTime() - lastModifiedTime.getTime()) < this.bizConfig
        .getInstanceConfigAuditTimeThresholdInMilli();
  }

  private long prepareInstanceId(InstanceConfigAuditModel auditModel) {
    Instance instance = instanceService.findInstance(auditModel.getAppId(),
        auditModel.getClusterName(), auditModel.getDataCenter(), auditModel.getIp());
    if (instance != null) {
      return instance.getId();
    }
    instance = new Instance();
    instance.setAppId(auditModel.getAppId());
    instance.setClusterName(auditModel.getClusterName());
    instance.setDataCenter(auditModel.getDataCenter());
    instance.setIp(auditModel.getIp());


    try {
      return instanceService.createInstance(instance).getId();
    } catch (DataIntegrityViolationException ex) {
      // return the one exists
      return instanceService.findInstance(instance.getAppId(), instance.getClusterName(),
          instance.getDataCenter(), instance.getIp()).getId();
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    auditExecutorService.submit(() -> {
      while (!auditStopped.get() && !Thread.currentThread().isInterrupted()) {
        try {
          InstanceConfigAuditModel model = audits.take();
          doAudit(model);
        } catch (Throwable ex) {
          Tracer.logError(ex);
        }
      }
    });
  }

  private void buildInstanceCache() {
    CacheBuilder<Object, Object> instanceCacheBuilder = CacheBuilder.newBuilder()
        .expireAfterAccess(1, TimeUnit.HOURS).maximumSize(this.bizConfig.getInstanceCacheMaxSize());
    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      instanceCacheBuilder.recordStats();
    }
    instanceCache = instanceCacheBuilder.build();
    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      GuavaCacheMetrics.monitor(meterRegistry, instanceCache, "instance_cache");
    }
  }

  private void buildInstanceConfigReleaseKeyCache() {
    CacheBuilder<Object, Object> instanceConfigReleaseKeyCacheBuilder =
        CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(this.bizConfig.getInstanceConfigCacheMaxSize());
    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      instanceConfigReleaseKeyCacheBuilder.recordStats();
    }
    instanceConfigReleaseKeyCache = instanceConfigReleaseKeyCacheBuilder.build();
    if (bizConfig.isConfigServiceCacheStatsEnabled()) {
      GuavaCacheMetrics.monitor(meterRegistry, instanceConfigReleaseKeyCache,
          "instance_config_cache");
    }
  }

  private String assembleInstanceKey(String appId, String cluster, String ip, String datacenter) {
    List<String> keyParts = Lists.newArrayList(appId, cluster, ip);
    if (!Strings.isNullOrEmpty(datacenter)) {
      keyParts.add(datacenter);
    }
    return STRING_JOINER.join(keyParts);
  }

  private String assembleInstanceConfigKey(long instanceId, String configAppId,
      String configNamespace) {
    return STRING_JOINER.join(instanceId, configAppId, configNamespace);
  }

  public static class InstanceConfigAuditModel {
    private String appId;
    private String clusterName;
    private String dataCenter;
    private String ip;
    private String configAppId;
    private String configClusterName;
    private String configNamespace;
    private String releaseKey;
    private Date offerTime;

    public InstanceConfigAuditModel(String appId, String clusterName, String dataCenter,
        String clientIp, String configAppId, String configClusterName, String configNamespace,
        String releaseKey) {
      this.offerTime = new Date();
      this.appId = appId;
      this.clusterName = clusterName;
      this.dataCenter = Strings.isNullOrEmpty(dataCenter) ? "" : dataCenter;
      this.ip = clientIp;
      this.configAppId = configAppId;
      this.configClusterName = configClusterName;
      this.configNamespace = configNamespace;
      this.releaseKey = releaseKey;
    }

    public String getAppId() {
      return appId;
    }

    public String getClusterName() {
      return clusterName;
    }

    public String getDataCenter() {
      return dataCenter;
    }

    public String getIp() {
      return ip;
    }

    public String getConfigAppId() {
      return configAppId;
    }

    public String getConfigNamespace() {
      return configNamespace;
    }

    public String getReleaseKey() {
      return releaseKey;
    }

    public String getConfigClusterName() {
      return configClusterName;
    }

    public Date getOfferTime() {
      return offerTime;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      InstanceConfigAuditModel model = (InstanceConfigAuditModel) o;
      return Objects.equals(appId, model.appId) && Objects.equals(clusterName, model.clusterName)
          && Objects.equals(dataCenter, model.dataCenter) && Objects.equals(ip, model.ip)
          && Objects.equals(configAppId, model.configAppId)
          && Objects.equals(configClusterName, model.configClusterName)
          && Objects.equals(configNamespace, model.configNamespace)
          && Objects.equals(releaseKey, model.releaseKey);
    }

    @Override
    public int hashCode() {
      return Objects.hash(appId, clusterName, dataCenter, ip, configAppId, configClusterName,
          configNamespace, releaseKey);
    }
  }
}
