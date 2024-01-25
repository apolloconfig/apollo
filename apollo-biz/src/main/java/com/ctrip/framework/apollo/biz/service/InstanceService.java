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

import com.ctrip.framework.apollo.biz.entity.Instance;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;
import com.ctrip.framework.apollo.biz.repository.InstanceConfigRepository;
import com.ctrip.framework.apollo.biz.repository.InstanceRepository;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigInteger;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Service
public class InstanceService {
  private final InstanceRepository instanceRepository;
  private final InstanceConfigRepository instanceConfigRepository;

  public InstanceService(
      final InstanceRepository instanceRepository,
      final InstanceConfigRepository instanceConfigRepository) {
    this.instanceRepository = instanceRepository;
    this.instanceConfigRepository = instanceConfigRepository;
  }

  public Instance findInstance(String appId, String clusterName, String dataCenter, String ip) {
    return instanceRepository.findByAppIdAndClusterNameAndDataCenterAndIp(appId, clusterName,
        dataCenter, ip);
  }

  public List<Instance> findInstancesByIds(Set<Long> instanceIds) {
    Iterable<Instance> instances = instanceRepository.findAllById(instanceIds);
    return Lists.newArrayList(instances);
  }

  @Transactional
  public Instance createInstance(Instance instance) {
    instance.setId(0); //protection

    return instanceRepository.save(instance);
  }

  public InstanceConfig findInstanceConfig(long instanceId, String configAppId, String
      configNamespaceName) {
    return instanceConfigRepository
        .findByInstanceIdAndConfigAppIdAndConfigNamespaceName(
            instanceId, configAppId, configNamespaceName);
  }

  public Page<InstanceConfig> findActiveInstanceConfigsByReleaseKey(String releaseKey, Pageable
      pageable) {
    return instanceConfigRepository.findByReleaseKeyAndDataChangeLastModifiedTimeAfter(releaseKey,
            getValidInstanceConfigDate(), pageable);
  }

  public Page<Instance> findInstancesByNamespace(String appId, String clusterName, String
      namespaceName, Pageable pageable) {
    Page<InstanceConfig> instanceConfigs = instanceConfigRepository.
        findByConfigAppIdAndConfigClusterNameAndConfigNamespaceNameAndDataChangeLastModifiedTimeAfter(appId, clusterName,
            namespaceName, getValidInstanceConfigDate(), pageable);

    List<Instance> instances = Collections.emptyList();
    if (instanceConfigs.hasContent()) {
      Set<Long> instanceIds = instanceConfigs.getContent().stream().map
          (InstanceConfig::getInstanceId).collect(Collectors.toSet());
      instances = findInstancesByIds(instanceIds);
    }

    return new PageImpl<>(instances, pageable, instanceConfigs.getTotalElements());
  }

  public Page<Instance> findInstancesByNamespaceAndInstanceAppId(String instanceAppId, String
      appId, String clusterName, String
                                                                     namespaceName, Pageable
                                                                     pageable) {
    Page<Object> instanceIdResult = instanceConfigRepository
        .findInstanceIdsByNamespaceAndInstanceAppId(instanceAppId, appId, clusterName,
            namespaceName, getValidInstanceConfigDate(), pageable);

    List<Instance> instances = Collections.emptyList();
    if (instanceIdResult.hasContent()) {
      Set<Long> instanceIds = instanceIdResult.getContent().stream().map((Object o) -> {
        if (o == null) {
          return null;
        }

        if (o instanceof Integer) {
          return ((Integer)o).longValue();
        }

        if (o instanceof Long) {
          return (Long) o;
        }

        //for h2 test
        if (o instanceof BigInteger) {
          return ((BigInteger) o).longValue();
        }

        return null;
      }).filter(Objects::nonNull).collect(Collectors.toSet());
      instances = findInstancesByIds(instanceIds);
    }

    return new PageImpl<>(instances, pageable, instanceIdResult.getTotalElements());
  }

  public List<InstanceConfig> findInstanceConfigsByNamespaceWithReleaseKeysNotIn(String appId,
                                                                                 String clusterName,
                                                                                 String
                                                                                     namespaceName,
                                                                                 Set<String>
                                                                                     releaseKeysNotIn) {
    List<InstanceConfig> instanceConfigs = instanceConfigRepository.
        findByConfigAppIdAndConfigClusterNameAndConfigNamespaceNameAndDataChangeLastModifiedTimeAfterAndReleaseKeyNotIn(appId, clusterName,
            namespaceName, getValidInstanceConfigDate(), releaseKeysNotIn);

    if (CollectionUtils.isEmpty(instanceConfigs)) {
      return Collections.emptyList();
    }

    return instanceConfigs;
  }

  /**
   * Currently the instance config is expired by 1 day, add one more hour to avoid possible time
   * difference
   */
  private Date getValidInstanceConfigDate() {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DATE, -1);
    cal.add(Calendar.HOUR, -1);
    return cal.getTime();
  }

  @Transactional
  public InstanceConfig createInstanceConfig(InstanceConfig instanceConfig) {
    instanceConfig.setId(0); //protection

    return instanceConfigRepository.save(instanceConfig);
  }

  @Transactional
  public InstanceConfig updateInstanceConfig(InstanceConfig instanceConfig) {
    InstanceConfig existedInstanceConfig = instanceConfigRepository.findById(instanceConfig.getId()).orElse(null);
    Preconditions.checkArgument(existedInstanceConfig != null, String.format(
        "Instance config %d doesn't exist", instanceConfig.getId()));

    existedInstanceConfig.setConfigClusterName(instanceConfig.getConfigClusterName());
    existedInstanceConfig.setReleaseKey(instanceConfig.getReleaseKey());
    existedInstanceConfig.setReleaseDeliveryTime(instanceConfig.getReleaseDeliveryTime());
    existedInstanceConfig.setDataChangeLastModifiedTime(instanceConfig
        .getDataChangeLastModifiedTime());

    return instanceConfigRepository.save(existedInstanceConfig);
  }

  @Transactional
  public int batchDeleteInstanceConfig(String configAppId, String configClusterName, String configNamespaceName){
    return instanceConfigRepository.batchDelete(configAppId, configClusterName, configNamespaceName);
  }
}
