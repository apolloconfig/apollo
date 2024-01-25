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

import com.ctrip.framework.apollo.biz.entity.Cluster;
import com.ctrip.framework.apollo.biz.service.ClusterService;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.core.ConfigConsts;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

public class ClusterControllerTest extends AbstractControllerTest {

  @InjectMocks
  private ClusterController clusterController;

  @Mock
  private ClusterService clusterService;

  @Test(expected = BadRequestException.class)
  public void testDeleteDefaultFail() {
    Cluster cluster = new Cluster();
    cluster.setName(ConfigConsts.CLUSTER_NAME_DEFAULT);
    when(clusterService.findOne(any(String.class), any(String.class))).thenReturn(cluster);
    clusterController.delete("1", "2", "d");
  }

  @Test
  public void testDeleteSuccess() {
    Cluster cluster = new Cluster();
    when(clusterService.findOne(any(String.class), any(String.class))).thenReturn(cluster);
    clusterController.delete("1", "2", "d");
    verify(clusterService, times(1)).findOne("1", "2");
  }

  @Test
  public void shouldFailWhenRequestBodyInvalid() {
    ClusterDTO cluster = new ClusterDTO();
    cluster.setAppId("valid");
    cluster.setName("notBlank");
    ResponseEntity<ClusterDTO> response =
        restTemplate.postForEntity(url("/apps/{appId}/clusters"), cluster, ClusterDTO.class, cluster.getAppId());
    ClusterDTO createdCluster = response.getBody();
    Assert.assertNotNull(createdCluster);
    Assert.assertEquals(cluster.getAppId(), createdCluster.getAppId());
    Assert.assertEquals(cluster.getName(), createdCluster.getName());

    cluster.setName("invalid app name");
    try {
      restTemplate.postForEntity(url("/apps/{appId}/clusters"), cluster, ClusterDTO.class, cluster.getAppId());
      Assert.fail("Should throw");
    } catch (HttpClientErrorException e) {
      Assert.assertThat(new String(e.getResponseBodyAsByteArray()), containsString(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
    }
  }
}
