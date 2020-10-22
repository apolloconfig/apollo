package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.message.MessageSender;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.repository.ReleaseRepository;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.biz.service.ReleaseService;
import com.ctrip.framework.apollo.common.dto.AppDTO;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

public class ReleaseControllerTest extends AbstractControllerTest {

  private static final  Gson GSON = new Gson();
  @Autowired
  ReleaseRepository releaseRepository;

  @Test
  @Sql(scripts = "/controller/test-release.sql", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/controller/cleanup.sql", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
  public void testReleaseBuild() {
    String appId = "someAppId";
    AppDTO app =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + appId, AppDTO.class);

    ClusterDTO cluster = restTemplate.getForObject(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/default",
        ClusterDTO.class);

    NamespaceDTO namespace =
        restTemplate.getForObject("http://localhost:" + port + "/apps/" + app.getAppId()
            + "/clusters/" + cluster.getName() + "/namespaces/application", NamespaceDTO.class);

    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    ItemDTO[] items =
        restTemplate.getForObject(
            "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/"
                + cluster.getName() + "/namespaces/" + namespace.getNamespaceName() + "/items",
            ItemDTO[].class);
    Assert.assertEquals(3, items.length);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add("name", "someReleaseName");
    parameters.add("comment", "someComment");
    parameters.add("operator", "test");
    HttpEntity<MultiValueMap<String, String>> entity =
        new HttpEntity<>(parameters, headers);
    ResponseEntity<ReleaseDTO> response = restTemplate.postForEntity(
        "http://localhost:" + port + "/apps/" + app.getAppId() + "/clusters/" + cluster.getName()
            + "/namespaces/" + namespace.getNamespaceName() + "/releases",
        entity, ReleaseDTO.class);
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    ReleaseDTO release = response.getBody();
    Assert.assertEquals("someReleaseName", release.getName());
    Assert.assertEquals("someComment", release.getComment());
    Assert.assertEquals("someAppId", release.getAppId());
    Assert.assertEquals("default", release.getClusterName());
    Assert.assertEquals("application", release.getNamespaceName());

    Map<String, String> configurations = new HashMap<>();
    configurations.put("k1", "v1");
    configurations.put("k2", "v2");
    configurations.put("k3", "v3");

    Assert.assertEquals(GSON.toJson(configurations), release.getConfigurations());
  }

  @Test
  public void testMessageSendAfterBuildRelease() throws Exception {
    String someAppId = "someAppId";
    String someNamespaceName = "someNamespace";
    String someCluster = "someCluster";
    String someName = "someName";
    String someComment = "someComment";
    String someUserName = "someUser";

    NamespaceService someNamespaceService = mock(NamespaceService.class);
    ReleaseService someReleaseService = mock(ReleaseService.class);
    MessageSender someMessageSender = mock(MessageSender.class);
    Namespace someNamespace = mock(Namespace.class);

    ReleaseController releaseController = new ReleaseController(someReleaseService, someNamespaceService, someMessageSender, null);

    when(someNamespaceService.findOne(someAppId, someCluster, someNamespaceName))
        .thenReturn(someNamespace);

    releaseController
        .publish(someAppId, someCluster, someNamespaceName, someName, someComment, "test", false);

    verify(someMessageSender, times(1))
        .sendMessage(Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR)
                .join(someAppId, someCluster, someNamespaceName),
            Topics.APOLLO_RELEASE_TOPIC);

  }
}
