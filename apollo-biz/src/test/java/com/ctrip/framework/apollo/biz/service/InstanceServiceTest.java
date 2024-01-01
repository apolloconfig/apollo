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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.Instance;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.Rollback;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class InstanceServiceTest extends AbstractIntegrationTest {
  @Autowired
  private InstanceService instanceService;

  @Test
  @Rollback
  public void testCreateAndFindInstance() throws Exception {
    String someAppId = "someAppId";
    String someClusterName = "someClusterName";
    String someDataCenter = "someDataCenter";
    String someIp = "someIp";

    Instance instance = instanceService.findInstance(someAppId, someClusterName, someDataCenter,
        someIp);

    assertNull(instance);

    instanceService.createInstance(assembleInstance(someAppId, someClusterName, someDataCenter,
        someIp));

    instance = instanceService.findInstance(someAppId, someClusterName, someDataCenter,
        someIp);

    assertNotEquals(0, instance.getId());
  }

  @Test
  @Rollback
  public void testFindInstancesByIds() throws Exception {
    String someAppId = "someAppId";
    String someClusterName = "someClusterName";
    String someDataCenter = "someDataCenter";
    String someIp = "someIp";
    String anotherIp = "anotherIp";

    Instance someInstance = instanceService.createInstance(assembleInstance(someAppId,
        someClusterName, someDataCenter, someIp));
    Instance anotherInstance = instanceService.createInstance(assembleInstance(someAppId,
        someClusterName, someDataCenter, anotherIp));

    List<Instance> instances = instanceService.findInstancesByIds(Sets.newHashSet(someInstance
        .getId(), anotherInstance.getId()));

    Set<String> ips = instances.stream().map(Instance::getIp).collect(Collectors.toSet());
    assertEquals(2, instances.size());
    assertEquals(Sets.newHashSet(someIp, anotherIp), ips);
  }

  @Test
  @Rollback
  public void testCreateAndFindInstanceConfig() throws Exception {
    long someInstanceId = 1;
    String someConfigAppId = "someConfigAppId";
    String someConfigClusterName = "someConfigClusterName";
    String someConfigNamespaceName = "someConfigNamespaceName";
    String someReleaseKey = "someReleaseKey";
    String anotherReleaseKey = "anotherReleaseKey";

    InstanceConfig instanceConfig = instanceService.findInstanceConfig(someInstanceId,
        someConfigAppId, someConfigNamespaceName);

    assertNull(instanceConfig);

    instanceService.createInstanceConfig(assembleInstanceConfig(someInstanceId, someConfigAppId,
        someConfigClusterName, someConfigNamespaceName, someReleaseKey));

    instanceConfig = instanceService.findInstanceConfig(someInstanceId, someConfigAppId,
        someConfigNamespaceName);

    assertNotEquals(0, instanceConfig.getId());
    assertEquals(someReleaseKey, instanceConfig.getReleaseKey());

    instanceConfig.setReleaseKey(anotherReleaseKey);

    instanceService.updateInstanceConfig(instanceConfig);

    InstanceConfig updated = instanceService.findInstanceConfig(someInstanceId, someConfigAppId,
        someConfigNamespaceName);

    assertEquals(instanceConfig.getId(), updated.getId());
    assertEquals(anotherReleaseKey, updated.getReleaseKey());
  }

  @Test
  @Rollback
  public void testFindActiveInstanceConfigs() throws Exception {
    long someInstanceId = 1;
    long anotherInstanceId = 2;
    String someConfigAppId = "someConfigAppId";
    String someConfigClusterName = "someConfigClusterName";
    String someConfigNamespaceName = "someConfigNamespaceName";
    Date someValidDate = new Date();
    Pageable pageable = PageRequest.of(0, 10);
    String someReleaseKey = "someReleaseKey";

    Calendar calendar = Calendar.getInstance();
    calendar.add(Calendar.DATE, -2);
    Date someInvalidDate = calendar.getTime();

    prepareInstanceConfigForInstance(someInstanceId, someConfigAppId, someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someValidDate);
    prepareInstanceConfigForInstance(anotherInstanceId, someConfigAppId, someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someInvalidDate);

    Page<InstanceConfig> validInstanceConfigs = instanceService
        .findActiveInstanceConfigsByReleaseKey(someReleaseKey, pageable);

    assertEquals(1, validInstanceConfigs.getContent().size());
    assertEquals(someInstanceId, validInstanceConfigs.getContent().get(0).getInstanceId());
  }

  @Test
  @Rollback
  public void testFindInstancesByNamespace() throws Exception {
    String someConfigAppId = "someConfigAppId";
    String someConfigClusterName = "someConfigClusterName";
    String someConfigNamespaceName = "someConfigNamespaceName";
    String someReleaseKey = "someReleaseKey";
    Date someValidDate = new Date();

    String someAppId = "someAppId";
    String someClusterName = "someClusterName";
    String someDataCenter = "someDataCenter";
    String someIp = "someIp";
    String anotherIp = "anotherIp";

    Instance someInstance = instanceService.createInstance(assembleInstance(someAppId,
        someClusterName, someDataCenter, someIp));
    Instance anotherInstance = instanceService.createInstance(assembleInstance(someAppId,
        someClusterName, someDataCenter, anotherIp));

    prepareInstanceConfigForInstance(someInstance.getId(), someConfigAppId, someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someValidDate);
    prepareInstanceConfigForInstance(anotherInstance.getId(), someConfigAppId,
        someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someValidDate);

    Page<Instance> result = instanceService.findInstancesByNamespace(someConfigAppId,
        someConfigClusterName, someConfigNamespaceName, PageRequest.of(0, 10));

    assertEquals(Lists.newArrayList(someInstance, anotherInstance), result.getContent());
  }

  @Test
  @Rollback
  public void testFindInstancesByNamespaceAndInstanceAppId() throws Exception {
    String someConfigAppId = "someConfigAppId";
    String someConfigClusterName = "someConfigClusterName";
    String someConfigNamespaceName = "someConfigNamespaceName";
    String someReleaseKey = "someReleaseKey";
    Date someValidDate = new Date();

    String someAppId = "someAppId";
    String anotherAppId = "anotherAppId";
    String someClusterName = "someClusterName";
    String someDataCenter = "someDataCenter";
    String someIp = "someIp";

    Instance someInstance = instanceService.createInstance(assembleInstance(someAppId,
        someClusterName, someDataCenter, someIp));
    Instance anotherInstance = instanceService.createInstance(assembleInstance(anotherAppId,
        someClusterName, someDataCenter, someIp));

    prepareInstanceConfigForInstance(someInstance.getId(), someConfigAppId, someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someValidDate);
    prepareInstanceConfigForInstance(anotherInstance.getId(), someConfigAppId,
        someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someValidDate);

    Page<Instance> result = instanceService.findInstancesByNamespaceAndInstanceAppId(someAppId,
        someConfigAppId, someConfigClusterName, someConfigNamespaceName, PageRequest.of(0, 10));
    Page<Instance> anotherResult = instanceService.findInstancesByNamespaceAndInstanceAppId(anotherAppId,
        someConfigAppId, someConfigClusterName, someConfigNamespaceName, PageRequest.of(0, 10));

    assertEquals(Lists.newArrayList(someInstance), result.getContent());
    assertEquals(Lists.newArrayList(anotherInstance), anotherResult.getContent());
  }


  @Test
  @Rollback
  public void testFindInstanceConfigsByNamespaceWithReleaseKeysNotIn() throws Exception {
    long someInstanceId = 1;
    long anotherInstanceId = 2;
    long yetAnotherInstanceId = 3;
    String someConfigAppId = "someConfigAppId";
    String someConfigClusterName = "someConfigClusterName";
    String someConfigNamespaceName = "someConfigNamespaceName";
    Date someValidDate = new Date();
    String someReleaseKey = "someReleaseKey";
    String anotherReleaseKey = "anotherReleaseKey";
    String yetAnotherReleaseKey = "yetAnotherReleaseKey";

    InstanceConfig someInstanceConfig = prepareInstanceConfigForInstance(someInstanceId,
        someConfigAppId, someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someValidDate);
    InstanceConfig anotherInstanceConfig = prepareInstanceConfigForInstance(anotherInstanceId,
        someConfigAppId, someConfigClusterName,
        someConfigNamespaceName, someReleaseKey, someValidDate);
    prepareInstanceConfigForInstance(yetAnotherInstanceId, someConfigAppId, someConfigClusterName,
        someConfigNamespaceName, anotherReleaseKey, someValidDate);

    List<InstanceConfig> instanceConfigs = instanceService
        .findInstanceConfigsByNamespaceWithReleaseKeysNotIn(someConfigAppId,
            someConfigClusterName, someConfigNamespaceName, Sets.newHashSet(anotherReleaseKey,
                yetAnotherReleaseKey));

    assertEquals(Lists.newArrayList(someInstanceConfig, anotherInstanceConfig), instanceConfigs);
  }

  private InstanceConfig prepareInstanceConfigForInstance(long instanceId, String configAppId,
                                                          String configClusterName, String
                                                              configNamespace, String releaseKey,
                                                          Date lastModifiedTime) {

    InstanceConfig someConfig = assembleInstanceConfig(instanceId, configAppId, configClusterName,
        configNamespace, releaseKey);
    someConfig.setDataChangeCreatedTime(lastModifiedTime);
    someConfig.setDataChangeLastModifiedTime(lastModifiedTime);

    return instanceService.createInstanceConfig(someConfig);
  }

  private Instance assembleInstance(String appId, String clusterName, String dataCenter, String
      ip) {
    Instance instance = new Instance();
    instance.setAppId(appId);
    instance.setIp(ip);
    instance.setClusterName(clusterName);
    instance.setDataCenter(dataCenter);

    return instance;
  }

  private InstanceConfig assembleInstanceConfig(long instanceId, String configAppId, String
      configClusterName, String configNamespaceName, String releaseKey) {
    InstanceConfig instanceConfig = new InstanceConfig();
    instanceConfig.setInstanceId(instanceId);
    instanceConfig.setConfigAppId(configAppId);
    instanceConfig.setConfigClusterName(configClusterName);
    instanceConfig.setConfigNamespaceName(configNamespaceName);
    instanceConfig.setReleaseKey(releaseKey);
    return instanceConfig;
  }
}
