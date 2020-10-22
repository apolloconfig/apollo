package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.Cluster;
import com.ctrip.framework.apollo.biz.service.ClusterService;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.core.ConfigConsts;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;

public class ClusterControllerTest extends AbstractControllerTest {
  private ClusterController clusterController;

  @Mock
  private ClusterService clusterService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    clusterController = new ClusterController(clusterService);
  }


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
        restTemplate.postForEntity(baseUrl() + "/apps/{appId}/clusters", cluster, ClusterDTO.class, cluster.getAppId());
    ClusterDTO createdCluster = response.getBody();
    Assert.assertNotNull(createdCluster);
    Assert.assertEquals(cluster.getAppId(), createdCluster.getAppId());
    Assert.assertEquals(cluster.getName(), createdCluster.getName());

    cluster.setName("invalid app name");
    try {
      restTemplate.postForEntity(baseUrl() + "/apps/{appId}/clusters", cluster, ClusterDTO.class, cluster.getAppId());
      Assert.fail("Should throw");
    } catch (HttpClientErrorException e) {
      Assert.assertThat(new String(e.getResponseBodyAsByteArray()), containsString(InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
    }
  }

  private String baseUrl() {
    return "http://localhost:" + port;
  }
}
