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

import java.util.LinkedHashMap;
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
    AppDTO app = restTemplate.getForObject(appBaseUrl(), AppDTO.class, appId);

    Assert.assertNotNull(app);
    ClusterDTO cluster = restTemplate.getForObject(clusterBaseUrl(), ClusterDTO.class, app.getAppId(), "default");

    Assert.assertNotNull(cluster);
    NamespaceDTO namespace =
        restTemplate.getForObject(namespaceBaseUrl(), NamespaceDTO.class, app.getAppId(), cluster.getName(), "application");

    Assert.assertNotNull(namespace);
    Assert.assertEquals("someAppId", app.getAppId());
    Assert.assertEquals("default", cluster.getName());
    Assert.assertEquals("application", namespace.getNamespaceName());

    ItemDTO[] items = restTemplate.getForObject(itemBaseUrl(),
            ItemDTO[].class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertNotNull(items);
    Assert.assertEquals(3, items.length);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
    parameters.add("name", "someReleaseName");
    parameters.add("comment", "someComment");
    parameters.add("operator", "test");
    HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(parameters, headers);
    ResponseEntity<ReleaseDTO> response = restTemplate.postForEntity(url("/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases"),
        entity, ReleaseDTO.class, app.getAppId(), cluster.getName(), namespace.getNamespaceName());
    Assert.assertEquals(HttpStatus.OK, response.getStatusCode());
    ReleaseDTO release = response.getBody();
    Assert.assertNotNull(release);
    Assert.assertEquals("someReleaseName", release.getName());
    Assert.assertEquals("someComment", release.getComment());
    Assert.assertEquals("someAppId", release.getAppId());
    Assert.assertEquals("default", release.getClusterName());
    Assert.assertEquals("application", release.getNamespaceName());

    Map<String, String> configurations = new LinkedHashMap<>();
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
