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
package com.ctrip.framework.apollo.biz.repository;

import com.ctrip.framework.apollo.biz.AbstractIntegrationTest;
import com.ctrip.framework.apollo.biz.entity.Instance;
import com.ctrip.framework.apollo.biz.entity.InstanceConfig;
import java.util.Date;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by kezhenxu94 at 2019/1/18 15:33.
 *
 * @author kezhenxu94 (kezhenxu94 at 163 dot com)
 */
public class InstanceConfigRepositoryTest extends AbstractIntegrationTest {
  @Autowired
  private InstanceConfigRepository instanceConfigRepository;
  @Autowired
  private InstanceRepository instanceRepository;

  @Rollback
  @Test
  public void shouldPaginated() {
    for (int i = 0; i < 25; i++) {
      Instance instance = new Instance();
      instance.setAppId("appId");
      instanceRepository.save(instance);

      final InstanceConfig instanceConfig = new InstanceConfig();
      instanceConfig.setConfigAppId("appId");
      instanceConfig.setInstanceId(instance.getId());
      instanceConfig.setConfigClusterName("cluster");
      instanceConfig.setConfigNamespaceName("namespace");
      instanceConfigRepository.save(instanceConfig);
    }
    Page<Object> ids = instanceConfigRepository.findInstanceIdsByNamespaceAndInstanceAppId(
        "appId", "appId", "cluster", "namespace", new Date(0), PageRequest.of(0, 10)
    );
    assertThat(ids.getContent(), hasSize(10));

    ids = instanceConfigRepository.findInstanceIdsByNamespaceAndInstanceAppId(
        "appId", "appId", "cluster", "namespace", new Date(0), PageRequest.of(1, 10)
    );
    assertThat(ids.getContent(), hasSize(10));

    ids = instanceConfigRepository.findInstanceIdsByNamespaceAndInstanceAppId(
        "appId", "appId", "cluster", "namespace", new Date(0), PageRequest.of(2, 10)
    );
    assertThat(ids.getContent(), hasSize(5));
  }
}
