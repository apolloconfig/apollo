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
import com.ctrip.framework.apollo.common.dto.InstanceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class InstanceConfigControllerTest {
  private InstanceConfigController instanceConfigController;

  @Mock
  private ReleaseService releaseService;

  @Mock
  private InstanceService instanceService;

  private Pageable pageable;

  @Before
  public void setUp() throws Exception {
    instanceConfigController = new InstanceConfigController(releaseService, instanceService);

    pageable = PageRequest.of(0, 2);
  }

  @Test
  public void getByRelease() throws Exception {
    long someReleaseId = 1;
    long someInstanceId = 1;
    long anotherInstanceId = 2;
    String someReleaseKey = "someKey";
    Release someRelease = new Release();
    someRelease.setReleaseKey(someReleaseKey);
    String someAppId = "someAppId";
    String anotherAppId = "anotherAppId";
    String someCluster = "someCluster";
    String someDataCenter = "someDC";
    String someConfigAppId = "someConfigAppId";
    String someConfigNamespace = "someNamespace";
    String someIp = "someIp";
    Date someReleaseDeliveryTime = new Date();
    Date anotherReleaseDeliveryTime = new Date();

    when(releaseService.findOne(someReleaseId)).thenReturn(someRelease);

    InstanceConfig someInstanceConfig = assembleInstanceConfig(someInstanceId, someConfigAppId,
        someConfigNamespace, someReleaseKey, someReleaseDeliveryTime);
    InstanceConfig anotherInstanceConfig = assembleInstanceConfig(anotherInstanceId,
        someConfigAppId, someConfigNamespace, someReleaseKey, anotherReleaseDeliveryTime);
    List<InstanceConfig> instanceConfigs = Lists.newArrayList(someInstanceConfig,
        anotherInstanceConfig);
    Page<InstanceConfig> instanceConfigPage = new PageImpl<>(instanceConfigs, pageable,
        instanceConfigs.size());

    when(instanceService.findActiveInstanceConfigsByReleaseKey(someReleaseKey, pageable))
        .thenReturn(instanceConfigPage);

    Instance someInstance = assembleInstance(someInstanceId, someAppId,
        someCluster, someDataCenter, someIp);
    Instance anotherInstance = assembleInstance(anotherInstanceId, anotherAppId,
        someCluster, someDataCenter, someIp);
    List<Instance> instances = Lists.newArrayList(someInstance, anotherInstance);

    Set<Long> instanceIds = Sets.newHashSet(someInstanceId, anotherInstanceId);
    when(instanceService.findInstancesByIds(instanceIds))
        .thenReturn(instances);

    PageDTO<InstanceDTO> result = instanceConfigController.getByRelease(someReleaseId, pageable);

    assertEquals(2, result.getContent().size());
    InstanceDTO someInstanceDto = null;
    InstanceDTO anotherInstanceDto = null;

    for (InstanceDTO instanceDTO : result.getContent()) {
      if (instanceDTO.getId() == someInstanceId) {
        someInstanceDto = instanceDTO;
      } else if (instanceDTO.getId() == anotherInstanceId) {
        anotherInstanceDto = instanceDTO;
      }
    }

    verifyInstance(someInstance, someInstanceDto);
    verifyInstance(anotherInstance, anotherInstanceDto);

    assertEquals(1, someInstanceDto.getConfigs().size());
    assertEquals(someReleaseDeliveryTime, someInstanceDto.getConfigs().get(0).getReleaseDeliveryTime());

    assertEquals(1, anotherInstanceDto.getConfigs().size());
    assertEquals(anotherReleaseDeliveryTime, anotherInstanceDto.getConfigs().get(0).getReleaseDeliveryTime());
  }

  @Test(expected = NotFoundException.class)
  public void testGetByReleaseWhenReleaseIsNotFound() throws Exception {
    long someReleaseIdNotExists = 1;

    when(releaseService.findOne(someReleaseIdNotExists)).thenReturn(null);

    instanceConfigController.getByRelease(someReleaseIdNotExists, pageable);
  }

  @Test
  public void testGetByReleasesNotIn() throws Exception {
    String someConfigAppId = "someConfigAppId";
    String someConfigClusterName = "someConfigClusterName";
    String someConfigNamespaceName = "someConfigNamespaceName";
    long someReleaseId = 1;
    long anotherReleaseId = 2;
    String releaseIds = Joiner.on(",").join(someReleaseId, anotherReleaseId);

    Date someReleaseDeliveryTime = new Date();
    Date anotherReleaseDeliveryTime = new Date();

    Release someRelease = mock(Release.class);
    Release anotherRelease = mock(Release.class);
    String someReleaseKey = "someReleaseKey";
    String anotherReleaseKey = "anotherReleaseKey";
    when(someRelease.getReleaseKey()).thenReturn(someReleaseKey);
    when(anotherRelease.getReleaseKey()).thenReturn(anotherReleaseKey);

    when(releaseService.findByReleaseIds(Sets.newHashSet(someReleaseId, anotherReleaseId)))
        .thenReturn(Lists.newArrayList(someRelease, anotherRelease));

    long someInstanceId = 1;
    long anotherInstanceId = 2;
    String someInstanceConfigReleaseKey = "someInstanceConfigReleaseKey";
    String anotherInstanceConfigReleaseKey = "anotherInstanceConfigReleaseKey";
    InstanceConfig someInstanceConfig = mock(InstanceConfig.class);
    InstanceConfig anotherInstanceConfig = mock(InstanceConfig.class);
    when(someInstanceConfig.getInstanceId()).thenReturn(someInstanceId);
    when(anotherInstanceConfig.getInstanceId()).thenReturn(anotherInstanceId);
    when(someInstanceConfig.getReleaseKey()).thenReturn(someInstanceConfigReleaseKey);
    when(anotherInstanceConfig.getReleaseKey()).thenReturn(anotherInstanceConfigReleaseKey);
    when(someInstanceConfig.getReleaseDeliveryTime()).thenReturn(someReleaseDeliveryTime);
    when(anotherInstanceConfig.getReleaseDeliveryTime()).thenReturn(anotherReleaseDeliveryTime);
    when(instanceService.findInstanceConfigsByNamespaceWithReleaseKeysNotIn(someConfigAppId,
        someConfigClusterName, someConfigNamespaceName, Sets.newHashSet(someReleaseKey,
            anotherReleaseKey))).thenReturn(Lists.newArrayList(someInstanceConfig,
        anotherInstanceConfig));

    String someInstanceAppId = "someInstanceAppId";
    String someInstanceClusterName = "someInstanceClusterName";
    String someInstanceNamespaceName = "someInstanceNamespaceName";
    String someIp = "someIp";
    String anotherIp = "anotherIp";
    Instance someInstance = assembleInstance(someInstanceId, someInstanceAppId,
        someInstanceClusterName,
        someInstanceNamespaceName, someIp);
    Instance anotherInstance = assembleInstance(anotherInstanceId, someInstanceAppId,
        someInstanceClusterName,
        someInstanceNamespaceName, anotherIp);
    when(instanceService.findInstancesByIds(Sets.newHashSet(someInstanceId, anotherInstanceId)))
        .thenReturn(Lists.newArrayList(someInstance, anotherInstance));

    Release someInstanceConfigRelease = new Release();
    someInstanceConfigRelease.setReleaseKey(someInstanceConfigReleaseKey);
    Release anotherInstanceConfigRelease = new Release();
    anotherInstanceConfigRelease.setReleaseKey(anotherInstanceConfigReleaseKey);
    when(releaseService.findByReleaseKeys(Sets.newHashSet(someInstanceConfigReleaseKey,
        anotherInstanceConfigReleaseKey))).thenReturn(Lists.newArrayList(someInstanceConfigRelease,
        anotherInstanceConfigRelease));

    List<InstanceDTO> result = instanceConfigController.getByReleasesNotIn(someConfigAppId,
        someConfigClusterName, someConfigNamespaceName, releaseIds);

    assertEquals(2, result.size());
    InstanceDTO someInstanceDto = null;
    InstanceDTO anotherInstanceDto = null;

    for (InstanceDTO instanceDTO : result) {
      if (instanceDTO.getId() == someInstanceId) {
        someInstanceDto = instanceDTO;
      } else if (instanceDTO.getId() == anotherInstanceId) {
        anotherInstanceDto = instanceDTO;
      }
    }

    verifyInstance(someInstance, someInstanceDto);
    verifyInstance(anotherInstance, anotherInstanceDto);

    assertEquals(someInstanceConfigReleaseKey, someInstanceDto.getConfigs().get(0).getRelease()
        .getReleaseKey());
    assertEquals(anotherInstanceConfigReleaseKey, anotherInstanceDto.getConfigs().get(0)
        .getRelease()
        .getReleaseKey());

    assertEquals(someReleaseDeliveryTime, someInstanceDto.getConfigs().get(0).getReleaseDeliveryTime());
    assertEquals(anotherReleaseDeliveryTime, anotherInstanceDto.getConfigs().get(0)
        .getReleaseDeliveryTime());
  }

  @Test
  public void testGetInstancesByNamespace() throws Exception {
    String someAppId = "someAppId";
    String someClusterName = "someClusterName";
    String someNamespaceName = "someNamespaceName";
    String someIp = "someIp";
    long someInstanceId = 1;
    long anotherInstanceId = 2;

    Instance someInstance = assembleInstance(someInstanceId, someAppId, someClusterName,
        someNamespaceName, someIp);
    Instance anotherInstance = assembleInstance(anotherInstanceId, someAppId, someClusterName,
        someNamespaceName, someIp);

    Page<Instance> instances = new PageImpl<>(Lists.newArrayList(someInstance, anotherInstance),
        pageable, 2);
    when(instanceService.findInstancesByNamespace(someAppId, someClusterName, someNamespaceName,
        pageable)).thenReturn(instances);

    PageDTO<InstanceDTO> result = instanceConfigController.getInstancesByNamespace(someAppId,
        someClusterName, someNamespaceName, null, pageable);

    assertEquals(2, result.getContent().size());
    InstanceDTO someInstanceDto = null;
    InstanceDTO anotherInstanceDto = null;

    for (InstanceDTO instanceDTO : result.getContent()) {
      if (instanceDTO.getId() == someInstanceId) {
        someInstanceDto = instanceDTO;
      } else if (instanceDTO.getId() == anotherInstanceId) {
        anotherInstanceDto = instanceDTO;
      }
    }

    verifyInstance(someInstance, someInstanceDto);
    verifyInstance(anotherInstance, anotherInstanceDto);
  }

  @Test
  public void testGetInstancesByNamespaceAndInstanceAppId() throws Exception {
    String someInstanceAppId = "someInstanceAppId";
    String someAppId = "someAppId";
    String someClusterName = "someClusterName";
    String someNamespaceName = "someNamespaceName";
    String someIp = "someIp";
    long someInstanceId = 1;
    long anotherInstanceId = 2;

    Instance someInstance = assembleInstance(someInstanceId, someAppId, someClusterName,
        someNamespaceName, someIp);
    Instance anotherInstance = assembleInstance(anotherInstanceId, someAppId, someClusterName,
        someNamespaceName, someIp);

    Page<Instance> instances = new PageImpl<>(Lists.newArrayList(someInstance, anotherInstance),
        pageable, 2);
    when(instanceService.findInstancesByNamespaceAndInstanceAppId(someInstanceAppId, someAppId,
        someClusterName, someNamespaceName, pageable)).thenReturn(instances);

    PageDTO<InstanceDTO> result = instanceConfigController.getInstancesByNamespace(someAppId,
        someClusterName, someNamespaceName, someInstanceAppId, pageable);

    assertEquals(2, result.getContent().size());
    InstanceDTO someInstanceDto = null;
    InstanceDTO anotherInstanceDto = null;

    for (InstanceDTO instanceDTO : result.getContent()) {
      if (instanceDTO.getId() == someInstanceId) {
        someInstanceDto = instanceDTO;
      } else if (instanceDTO.getId() == anotherInstanceId) {
        anotherInstanceDto = instanceDTO;
      }
    }

    verifyInstance(someInstance, someInstanceDto);
    verifyInstance(anotherInstance, anotherInstanceDto);
  }


  @Test
  public void testGetInstancesCountByNamespace() throws Exception {
    String someAppId = "someAppId";
    String someClusterName = "someClusterName";
    String someNamespaceName = "someNamespaceName";

    Page<Instance> instances = new PageImpl<>(Collections.emptyList(), pageable, 2);

    when(instanceService.findInstancesByNamespace(eq(someAppId), eq(someClusterName),
        eq(someNamespaceName), any(Pageable.class))).thenReturn(instances);

    long result = instanceConfigController.getInstancesCountByNamespace(someAppId,
        someClusterName, someNamespaceName);

    assertEquals(2, result);
  }

  private void verifyInstance(Instance instance, InstanceDTO instanceDTO) {
    assertEquals(instance.getId(), instanceDTO.getId());
    assertEquals(instance.getAppId(), instanceDTO.getAppId());
    assertEquals(instance.getClusterName(), instanceDTO.getClusterName());
    assertEquals(instance.getDataCenter(), instanceDTO.getDataCenter());
    assertEquals(instance.getIp(), instanceDTO.getIp());
    assertEquals(instance.getDataChangeCreatedTime(), instanceDTO.getDataChangeCreatedTime());
  }

  private Instance assembleInstance(long instanceId, String appId, String clusterName, String
      dataCenter, String ip) {
    Instance instance = new Instance();
    instance.setId(instanceId);
    instance.setAppId(appId);
    instance.setIp(ip);
    instance.setClusterName(clusterName);
    instance.setDataCenter(dataCenter);
    instance.setDataChangeCreatedTime(new Date());

    return instance;
  }

  private InstanceConfig assembleInstanceConfig(long instanceId, String configAppId, String
      configNamespaceName, String releaseKey, Date releaseDeliveryTime) {
    InstanceConfig instanceConfig = new InstanceConfig();
    instanceConfig.setInstanceId(instanceId);
    instanceConfig.setConfigAppId(configAppId);
    instanceConfig.setConfigNamespaceName(configNamespaceName);
    instanceConfig.setReleaseKey(releaseKey);
    instanceConfig.setDataChangeLastModifiedTime(new Date());
    instanceConfig.setReleaseDeliveryTime(releaseDeliveryTime);
    return instanceConfig;
  }
}
