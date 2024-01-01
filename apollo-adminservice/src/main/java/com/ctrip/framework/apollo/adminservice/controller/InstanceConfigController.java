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
package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.Instance;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;
import com.ctrip.framework.apollo.biz.entity.Release;
import com.ctrip.framework.apollo.biz.service.InstanceService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.common.dto.InstanceConfigDTO;
import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RestController
@RequestMapping("/instances")
public class InstanceConfigController {
  private static final Splitter RELEASES_SPLITTER = Splitter.on(",").omitEmptyStrings()
      .trimResults();
  private final ReleaseService releaseService;
  private final InstanceService instanceService;

  public InstanceConfigController(final ReleaseService releaseService, final InstanceService instanceService) {
    this.releaseService = releaseService;
    this.instanceService = instanceService;
  }

  @GetMapping("/by-release")
  public PageDTO<InstanceDTO> getByRelease(@RequestParam("releaseId") long releaseId,
                                           Pageable pageable) {
    Release release = releaseService.findOne(releaseId);
    if (release == null) {
      throw NotFoundException.releaseNotFound(releaseId);
    }
    Page<InstanceConfig> instanceConfigsPage = instanceService.findActiveInstanceConfigsByReleaseKey
        (release.getReleaseKey(), pageable);

    List<InstanceDTO> instanceDTOs = Collections.emptyList();

    if (instanceConfigsPage.hasContent()) {
      Multimap<Long, InstanceConfig> instanceConfigMap = HashMultimap.create();
      Set<String> otherReleaseKeys = Sets.newHashSet();

      for (InstanceConfig instanceConfig : instanceConfigsPage.getContent()) {
        instanceConfigMap.put(instanceConfig.getInstanceId(), instanceConfig);
        otherReleaseKeys.add(instanceConfig.getReleaseKey());
      }

      Set<Long> instanceIds = instanceConfigMap.keySet();

      List<Instance> instances = instanceService.findInstancesByIds(instanceIds);

      if (!CollectionUtils.isEmpty(instances)) {
        instanceDTOs = BeanUtils.batchTransform(InstanceDTO.class, instances);
      }

      for (InstanceDTO instanceDTO : instanceDTOs) {
        Collection<InstanceConfig> configs = instanceConfigMap.get(instanceDTO.getId());
        List<InstanceConfigDTO> configDTOs = configs.stream().map(instanceConfig -> {
          InstanceConfigDTO instanceConfigDTO = new InstanceConfigDTO();
          //to save some space
          instanceConfigDTO.setRelease(null);
          instanceConfigDTO.setReleaseDeliveryTime(instanceConfig.getReleaseDeliveryTime());
          instanceConfigDTO.setDataChangeLastModifiedTime(instanceConfig
              .getDataChangeLastModifiedTime());
          return instanceConfigDTO;
        }).collect(Collectors.toList());
        instanceDTO.setConfigs(configDTOs);
      }
    }

    return new PageDTO<>(instanceDTOs, pageable, instanceConfigsPage.getTotalElements());
  }

  @GetMapping("/by-namespace-and-releases-not-in")
  public List<InstanceDTO> getByReleasesNotIn(@RequestParam("appId") String appId,
                                              @RequestParam("clusterName") String clusterName,
                                              @RequestParam("namespaceName") String namespaceName,
                                              @RequestParam("releaseIds") String releaseIds) {
    Set<Long> releaseIdSet = RELEASES_SPLITTER.splitToList(releaseIds).stream().map(Long::parseLong)
        .collect(Collectors.toSet());

    List<Release> releases = releaseService.findByReleaseIds(releaseIdSet);

    if (CollectionUtils.isEmpty(releases)) {
      throw NotFoundException.releaseNotFound(releaseIds);
    }

    Set<String> releaseKeys = releases.stream().map(Release::getReleaseKey).collect(Collectors
        .toSet());

    List<InstanceConfig> instanceConfigs = instanceService
        .findInstanceConfigsByNamespaceWithReleaseKeysNotIn(appId, clusterName, namespaceName,
            releaseKeys);

    Multimap<Long, InstanceConfig> instanceConfigMap = HashMultimap.create();
    Set<String> otherReleaseKeys = Sets.newHashSet();

    for (InstanceConfig instanceConfig : instanceConfigs) {
      instanceConfigMap.put(instanceConfig.getInstanceId(), instanceConfig);
      otherReleaseKeys.add(instanceConfig.getReleaseKey());
    }

    List<Instance> instances = instanceService.findInstancesByIds(instanceConfigMap.keySet());

    if (CollectionUtils.isEmpty(instances)) {
      return Collections.emptyList();
    }

    List<InstanceDTO> instanceDTOs = BeanUtils.batchTransform(InstanceDTO.class, instances);

    List<Release> otherReleases = releaseService.findByReleaseKeys(otherReleaseKeys);
    Map<String, ReleaseDTO> releaseMap = Maps.newHashMap();

    for (Release release : otherReleases) {
      //unset configurations to save space
      release.setConfigurations(null);
      ReleaseDTO releaseDTO = BeanUtils.transform(ReleaseDTO.class, release);
      releaseMap.put(release.getReleaseKey(), releaseDTO);
    }

    for (InstanceDTO instanceDTO : instanceDTOs) {
      Collection<InstanceConfig> configs = instanceConfigMap.get(instanceDTO.getId());
      List<InstanceConfigDTO> configDTOs = configs.stream().map(instanceConfig -> {
        InstanceConfigDTO instanceConfigDTO = new InstanceConfigDTO();
        instanceConfigDTO.setRelease(releaseMap.get(instanceConfig.getReleaseKey()));
        instanceConfigDTO.setReleaseDeliveryTime(instanceConfig.getReleaseDeliveryTime());
        instanceConfigDTO.setDataChangeLastModifiedTime(instanceConfig
            .getDataChangeLastModifiedTime());
        return instanceConfigDTO;
      }).collect(Collectors.toList());
      instanceDTO.setConfigs(configDTOs);
    }

    return instanceDTOs;
  }

  @GetMapping("/by-namespace")
  public PageDTO<InstanceDTO> getInstancesByNamespace(
      @RequestParam("appId") String appId, @RequestParam("clusterName") String clusterName,
      @RequestParam("namespaceName") String namespaceName,
      @RequestParam(value = "instanceAppId", required = false) String instanceAppId,
      Pageable pageable) {
    Page<Instance> instances;
    if (Strings.isNullOrEmpty(instanceAppId)) {
      instances = instanceService.findInstancesByNamespace(appId, clusterName,
          namespaceName, pageable);
    } else {
      instances = instanceService.findInstancesByNamespaceAndInstanceAppId(instanceAppId, appId,
          clusterName, namespaceName, pageable);
    }

    List<InstanceDTO> instanceDTOs = BeanUtils.batchTransform(InstanceDTO.class, instances.getContent());
    return new PageDTO<>(instanceDTOs, pageable, instances.getTotalElements());
  }

  @GetMapping("/by-namespace/count")
  public long getInstancesCountByNamespace(@RequestParam("appId") String appId,
                                          @RequestParam("clusterName") String clusterName,
                                          @RequestParam("namespaceName") String namespaceName) {
    Page<Instance> instances = instanceService.findInstancesByNamespace(appId, clusterName,
        namespaceName, PageRequest.of(0, 1));
    return instances.getTotalElements();
  }
}
