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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;

@RunWith(MockitoJUnitRunner.class)
public class ClusterControllerTest {

  @Mock
  private ClusterService clusterService;

  @Mock
  private UserInfoHolder userInfoHolder;

  @InjectMocks
  private ClusterController clusterController;

  @Captor
  private ArgumentCaptor<ClusterDTO> clusterCaptor;

  @Test
  public void shouldCreateClusterWithCurrentOperator() {
    ClusterDTO toCreate = new ClusterDTO();
    toCreate.setAppId("SampleApp");
    toCreate.setName("sampleCluster");

    ClusterDTO created = new ClusterDTO();
    created.setAppId("SampleApp");
    created.setName("sampleCluster");

    when(userInfoHolder.getUser()).thenReturn(new UserInfo("apollo"));
    when(clusterService.createCluster(eq(Env.DEV), clusterCaptor.capture())).thenReturn(created);

    ClusterDTO result = clusterController.createCluster("SampleApp", "DEV", toCreate);

    assertSame(created, result);
    ClusterDTO captured = clusterCaptor.getValue();
    assertEquals("apollo", captured.getDataChangeCreatedBy());
    assertEquals("apollo", captured.getDataChangeLastModifiedBy());
  }

  @Test
  public void shouldDeleteClusterByEnvAndName() {
    ResponseEntity<Void> response =
        clusterController.deleteCluster("SampleApp", "DEV", "sampleCluster");

    assertEquals(200, response.getStatusCodeValue());
    verify(clusterService).deleteCluster(Env.DEV, "SampleApp", "sampleCluster");
  }

  @Test
  public void shouldLoadClusterFromService() {
    ClusterDTO loaded = new ClusterDTO();
    loaded.setName("sampleCluster");
    when(clusterService.loadCluster("SampleApp", Env.DEV, "sampleCluster")).thenReturn(loaded);

    ClusterDTO result = clusterController.loadCluster("SampleApp", "DEV", "sampleCluster");

    assertSame(loaded, result);
    verify(clusterService).loadCluster("SampleApp", Env.DEV, "sampleCluster");
  }
}
