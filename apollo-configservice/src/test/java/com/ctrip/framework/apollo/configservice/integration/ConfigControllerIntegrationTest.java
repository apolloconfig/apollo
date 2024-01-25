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
package com.ctrip.framework.apollo.configservice.integration;

import com.ctrip.framework.apollo.configservice.service.AppNamespaceServiceWithCache;

import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfig;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpStatusCodeException;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class ConfigControllerIntegrationTest extends AbstractBaseIntegrationTest {
  private String someAppId;
  private String somePublicAppId;
  private String someCluster;
  private String someNamespace;
  private String somePublicNamespace;
  private String someDC;
  private String someDefaultCluster;
  private String someClientIp;
  private ExecutorService executorService;

  @Autowired
  private AppNamespaceServiceWithCache appNamespaceServiceWithCache;

  @Before
  public void setUp() throws Exception {
    ReflectionTestUtils.invokeMethod(appNamespaceServiceWithCache, "reset");

    someAppId = "someAppId";
    someCluster = "someCluster";
    someNamespace = "someNamespace";
    somePublicAppId = "somePublicAppId";
    somePublicNamespace = "somePublicNamespace";
    someDC = "someDC";
    someDefaultCluster = ConfigConsts.CLUSTER_NAME_DEFAULT;
    someClientIp = "1.1.1.1";
    executorService = Executors.newFixedThreadPool(1);
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryConfigWithDefaultClusterAndDefaultNamespaceOK() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}", ApolloConfig.class,
            getHostUrl(), someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, ConfigConsts.NAMESPACE_APPLICATION);
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-RELEASE-KEY1", result.getReleaseKey());
    assertEquals("v1", result.getConfigurations().get("k1"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryConfigWithDefaultClusterAndDefaultNamespaceAndIncorrectCase() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}", ApolloConfig.class,
            getHostUrl(), someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT,
            ConfigConsts.NAMESPACE_APPLICATION.toUpperCase());
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-RELEASE-KEY1", result.getReleaseKey());
    assertEquals("v1", result.getConfigurations().get("k1"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-gray-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryGrayConfigWithDefaultClusterAndDefaultNamespaceOK() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    periodicSendMessage(executorService, assembleKey(someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, ConfigConsts.NAMESPACE_APPLICATION),
        stop);

    TimeUnit.MILLISECONDS.sleep(500);

    stop.set(true);

    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?ip={clientIp}", ApolloConfig.class,
            getHostUrl(), someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, ConfigConsts.NAMESPACE_APPLICATION, someClientIp);
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-GRAY-RELEASE-KEY1", result.getReleaseKey());
    assertEquals("v1-gray", result.getConfigurations().get("k1"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-gray-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryGrayConfigWithDefaultClusterAndDefaultNamespaceAndIncorrectCase() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    periodicSendMessage(executorService, assembleKey(someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, ConfigConsts.NAMESPACE_APPLICATION),
        stop);

    TimeUnit.MILLISECONDS.sleep(500);

    stop.set(true);

    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?ip={clientIp}", ApolloConfig.class,
            getHostUrl(), someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT,
            ConfigConsts.NAMESPACE_APPLICATION.toUpperCase(), someClientIp);
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-GRAY-RELEASE-KEY1", result.getReleaseKey());
    assertEquals("v1-gray", result.getConfigurations().get("k1"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryConfigFileWithDefaultClusterAndDefaultNamespaceOK() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}", ApolloConfig.class,
            getHostUrl(), someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, ConfigConsts.NAMESPACE_APPLICATION + ".properties");
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-RELEASE-KEY1", result.getReleaseKey());
    assertEquals("v1", result.getConfigurations().get("k1"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryConfigWithNamespaceOK() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}", ApolloConfig.class,
            getHostUrl(), someAppId, someCluster, someNamespace);
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-RELEASE-KEY2", result.getReleaseKey());
    assertEquals("v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryConfigFileWithNamespaceOK() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}", ApolloConfig.class,
            getHostUrl(), someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, someNamespace + ".xml");
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-RELEASE-KEY5", result.getReleaseKey());
    assertEquals("v1-file", result.getConfigurations().get("k1"));
    assertEquals("v2-file", result.getConfigurations().get("k2"));
  }

  @Test
  public void testQueryConfigError() throws Exception {
    String someNamespaceNotExists = "someNamespaceNotExists";

    HttpStatusCodeException httpException = null;
    try {
      ResponseEntity<ApolloConfig> response = restTemplate
          .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}", ApolloConfig.class,
              getHostUrl(), someAppId, someCluster, someNamespaceNotExists);
    } catch (HttpStatusCodeException ex) {
      httpException = ex;
    }

    assertEquals(HttpStatus.NOT_FOUND, httpException.getStatusCode());
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryConfigNotModified() throws Exception {
    String releaseKey = "TEST-RELEASE-KEY2";
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?releaseKey={releaseKey}",
            ApolloConfig.class,
            getHostUrl(), someAppId, someCluster, someNamespace, releaseKey);

    assertEquals(HttpStatus.NOT_MODIFIED, response.getStatusCode());
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-gray-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicGrayConfigWithNoOverride() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    periodicSendMessage(executorService, assembleKey(somePublicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace),
        stop);

    TimeUnit.MILLISECONDS.sleep(500);

    stop.set(true);

    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?ip={clientIp}", ApolloConfig.class,
            getHostUrl(), someAppId, someCluster, somePublicNamespace, someClientIp);
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals("TEST-GRAY-RELEASE-KEY2", result.getReleaseKey());
    assertEquals("gray-v1", result.getConfigurations().get("k1"));
    assertEquals("gray-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicConfigWithDataCenterFoundAndNoOverride() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?dataCenter={dateCenter}",
            ApolloConfig.class,
            getHostUrl(), someAppId, someCluster, somePublicNamespace, someDC);
    ApolloConfig result = response.getBody();

    assertEquals("TEST-RELEASE-KEY4", result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someCluster, result.getCluster());
    assertEquals(somePublicNamespace, result.getNamespaceName());
    assertEquals("someDC-v1", result.getConfigurations().get("k1"));
    assertEquals("someDC-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-public-dc-override.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicConfigWithDataCenterFoundAndOverride() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?dataCenter={dateCenter}",
            ApolloConfig.class,
            getHostUrl(), someAppId, someDefaultCluster, somePublicNamespace, someDC);
    ApolloConfig result = response.getBody();

    assertEquals(
        "TEST-RELEASE-KEY6" + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + "TEST-RELEASE-KEY4",
        result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someDC, result.getCluster());
    assertEquals(somePublicNamespace, result.getNamespaceName());
    assertEquals("override-someDC-v1", result.getConfigurations().get("k1"));
    assertEquals("someDC-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-public-dc-override.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicConfigWithIncorrectCaseAndDataCenterFoundAndOverride() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?dataCenter={dateCenter}",
            ApolloConfig.class,
            getHostUrl(), someAppId, someDefaultCluster, somePublicNamespace.toUpperCase(), someDC);
    ApolloConfig result = response.getBody();

    assertEquals(
        "TEST-RELEASE-KEY6" + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + "TEST-RELEASE-KEY4",
        result.getReleaseKey());
    assertEquals("override-someDC-v1", result.getConfigurations().get("k1"));
    assertEquals("someDC-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicConfigWithDataCenterNotFoundAndNoOverride() throws Exception {
    String someDCNotFound = "someDCNotFound";
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?dataCenter={dateCenter}",
            ApolloConfig.class,
            getHostUrl(), someAppId, someCluster, somePublicNamespace, someDCNotFound);
    ApolloConfig result = response.getBody();

    assertEquals("TEST-RELEASE-KEY3", result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someCluster, result.getCluster());
    assertEquals(somePublicNamespace, result.getNamespaceName());
    assertEquals("default-v1", result.getConfigurations().get("k1"));
    assertEquals("default-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-public-default-override.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicConfigWithDataCenterNotFoundAndOverride() throws Exception {
    String someDCNotFound = "someDCNotFound";
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?dataCenter={dateCenter}",
            ApolloConfig.class,
            getHostUrl(), someAppId, someDefaultCluster, somePublicNamespace, someDCNotFound);
    ApolloConfig result = response.getBody();

    assertEquals(
        "TEST-RELEASE-KEY5" + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + "TEST-RELEASE-KEY3",
        result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(someDefaultCluster, result.getCluster());
    assertEquals(somePublicNamespace, result.getNamespaceName());
    assertEquals("override-v1", result.getConfigurations().get("k1"));
    assertEquals("default-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-public-default-override.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-gray-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicGrayConfigWithOverride() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    periodicSendMessage(executorService, assembleKey(somePublicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace),
        stop);

    TimeUnit.MILLISECONDS.sleep(500);

    stop.set(true);

    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?ip={clientIp}", ApolloConfig.class,
            getHostUrl(), someAppId, someDefaultCluster, somePublicNamespace, someClientIp);
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "TEST-RELEASE-KEY5" + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + "TEST-GRAY-RELEASE-KEY2",
        result.getReleaseKey());
    assertEquals("override-v1", result.getConfigurations().get("k1"));
    assertEquals("gray-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-public-default-override.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-gray-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicGrayConfigWithIncorrectCaseAndOverride() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    periodicSendMessage(executorService, assembleKey(somePublicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace),
        stop);

    TimeUnit.MILLISECONDS.sleep(500);

    stop.set(true);

    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?ip={clientIp}", ApolloConfig.class,
            getHostUrl(), someAppId, someDefaultCluster, somePublicNamespace.toUpperCase(), someClientIp);
    ApolloConfig result = response.getBody();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(
        "TEST-RELEASE-KEY5" + ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR + "TEST-GRAY-RELEASE-KEY2",
        result.getReleaseKey());
    assertEquals("override-v1", result.getConfigurations().get("k1"));
    assertEquals("gray-v2", result.getConfigurations().get("k2"));
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPrivateConfigFileWithPublicNamespaceExists() throws Exception {
    String namespaceName = "anotherNamespace";
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}",
            ApolloConfig.class,
            getHostUrl(), someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, namespaceName);
    ApolloConfig result = response.getBody();

    assertEquals("TEST-RELEASE-KEY6", result.getReleaseKey());
    assertEquals(someAppId, result.getAppId());
    assertEquals(ConfigConsts.CLUSTER_NAME_DEFAULT, result.getCluster());
    assertEquals(namespaceName, result.getNamespaceName());
    assertEquals("v1-file", result.getConfigurations().get("k1"));
    assertEquals(null, result.getConfigurations().get("k2"));
  }

  @Test
  public void testQueryConfigForNoAppIdPlaceHolderWithPrivateNamespace() throws Exception {

    HttpStatusCodeException httpException = null;
    try {
      ResponseEntity<ApolloConfig> response = restTemplate
          .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}", ApolloConfig.class,
              getHostUrl(), ConfigConsts.NO_APPID_PLACEHOLDER, someCluster, ConfigConsts.NAMESPACE_APPLICATION);
    } catch (HttpStatusCodeException ex) {
      httpException = ex;
    }

    assertEquals(HttpStatus.NOT_FOUND, httpException.getStatusCode());
  }

  @Test
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testQueryPublicConfigForNoAppIdPlaceHolder() throws Exception {
    ResponseEntity<ApolloConfig> response = restTemplate
        .getForEntity("http://{baseurl}/configs/{appId}/{clusterName}/{namespace}?dataCenter={dateCenter}",
            ApolloConfig.class,
            getHostUrl(), ConfigConsts.NO_APPID_PLACEHOLDER, someCluster, somePublicNamespace, someDC);
    ApolloConfig result = response.getBody();

    assertEquals("TEST-RELEASE-KEY4", result.getReleaseKey());
    assertEquals(ConfigConsts.NO_APPID_PLACEHOLDER, result.getAppId());
    assertEquals(someCluster, result.getCluster());
    assertEquals(somePublicNamespace, result.getNamespaceName());
    assertEquals("someDC-v1", result.getConfigurations().get("k1"));
    assertEquals("someDC-v2", result.getConfigurations().get("k2"));
  }

}
