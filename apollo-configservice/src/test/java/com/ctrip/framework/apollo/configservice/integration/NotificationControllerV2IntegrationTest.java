package com.ctrip.framework.apollo.configservice.integration;

import com.ctrip.framework.apollo.core.dto.ApolloNotificationMessages;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import com.ctrip.framework.apollo.configservice.service.ReleaseMessageServiceWithCache;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.dto.ApolloConfigNotification;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class NotificationControllerV2IntegrationTest extends AbstractBaseIntegrationTest {
  @Autowired
  private Gson gson;

  @Autowired
  private ReleaseMessageServiceWithCache releaseMessageServiceWithCache;

  private String someAppId;
  private String someCluster;
  private String defaultNamespace;
  private String somePublicNamespace;
  private ExecutorService executorService;
  private ParameterizedTypeReference<List<ApolloConfigNotification>> typeReference;

  @Before
  public void setUp() throws Exception {
    ReflectionTestUtils.invokeMethod(releaseMessageServiceWithCache, "reset");
    someAppId = "someAppId";
    someCluster = ConfigConsts.CLUSTER_NAME_DEFAULT;
    defaultNamespace = ConfigConsts.NAMESPACE_APPLICATION;
    somePublicNamespace = "somePublicNamespace";
    executorService = Executors.newSingleThreadExecutor();
    typeReference = new ParameterizedTypeReference<List<ApolloConfigNotification>>() {
    };
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithDefaultNamespace() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(someAppId, someCluster, defaultNamespace);
    periodicSendMessage(executorService, key, stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(defaultNamespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER));

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(defaultNamespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithDefaultNamespaceAsFile() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(someAppId, someCluster, defaultNamespace);
    periodicSendMessage(executorService, key,
        stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(defaultNamespace + ".properties",
            ConfigConsts.NOTIFICATION_ID_PLACEHOLDER));

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(defaultNamespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultipleNamespaces() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(someAppId, someCluster, somePublicNamespace);
    periodicSendMessage(executorService, key, stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(defaultNamespace + ".properties",
            ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, defaultNamespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER,
            somePublicNamespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER));

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultipleNamespacesAndIncorrectCase() throws Exception {
    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(someAppId, someCluster, somePublicNamespace);
    periodicSendMessage(executorService, key, stop);

    String someDefaultNamespaceWithIncorrectCase = defaultNamespace.toUpperCase();
    String somePublicNamespaceWithIncorrectCase = somePublicNamespace.toUpperCase();

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(defaultNamespace + ".properties",
            ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, someDefaultNamespaceWithIncorrectCase,
            ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, somePublicNamespaceWithIncorrectCase,
            ConfigConsts.NOTIFICATION_ID_PLACEHOLDER));

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespaceWithIncorrectCase, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithPrivateNamespaceAsFile() throws Exception {
    String namespace = "someNamespace.xml";
    AtomicBoolean stop = new AtomicBoolean();

    String key = assembleKey(someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, namespace);
    periodicSendMessage(executorService, key, stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(namespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER));

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(namespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithDefaultNamespaceWithNotificationIdOutDated()
      throws Exception {
    long someOutDatedNotificationId = 1;
    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(defaultNamespace, someOutDatedNotificationId));

    long newNotificationId = 10;

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(defaultNamespace, notifications.get(0).getNamespaceName());
    assertEquals(newNotificationId, notifications.get(0).getNotificationId());

    String key = assembleKey(someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, ConfigConsts.NAMESPACE_APPLICATION);
    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertEquals(newNotificationId, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWthPublicNamespaceAndNoDataCenter() throws Exception {
    String publicAppId = "somePublicAppId";

    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);
    periodicSendMessage(executorService, key, stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER));

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWthPublicNamespaceAndDataCenter() throws Exception {
    String publicAppId = "somePublicAppId";
    String someDC = "someDC";

    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(publicAppId, someDC, somePublicNamespace);
    periodicSendMessage(executorService, key, stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}&dataCenter={dataCenter}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER),
        someDC);

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWthMultipleNamespacesAndMultipleNamespacesChanged()
      throws Exception {
    String publicAppId = "somePublicAppId";
    String someDC = "someDC";

    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(publicAppId, someDC, somePublicNamespace);
    periodicSendMessage(executorService, key, stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}&dataCenter={dataCenter}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(defaultNamespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER,
            somePublicNamespace, ConfigConsts.NOTIFICATION_ID_PLACEHOLDER),
        someDC);

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWthPublicNamespaceAsFile() throws Exception {
    String publicAppId = "somePublicAppId";
    String someDC = "someDC";

    AtomicBoolean stop = new AtomicBoolean();
    String key = assembleKey(publicAppId, someDC, somePublicNamespace);
    periodicSendMessage(executorService, key, stop);

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}&dataCenter={dataCenter}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespace + ".properties",
            ConfigConsts.NOTIFICATION_ID_PLACEHOLDER), someDC);

    stop.set(true);

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertNotEquals(0, notifications.get(0).getNotificationId());

    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertNotEquals(ConfigConsts.NOTIFICATION_ID_PLACEHOLDER, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithPublicNamespaceWithNotificationIdOutDated() throws Exception {
    String publicAppId = "somePublicAppId";
    long someOutDatedNotificationId = 1;

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespace,
            someOutDatedNotificationId));

    long newNotificationId = 20;

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertEquals(newNotificationId, notifications.get(0).getNotificationId());

    String key = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);
    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertEquals(newNotificationId, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultiplePublicNamespaceWithIncorrectCaseWithNotificationIdOutDated() throws Exception {
    String publicAppId = "somePublicAppId";
    long someOutDatedNotificationId = 1;
    long newNotificationId = 20;

    String somePublicNameWithIncorrectCase = somePublicNamespace.toUpperCase();

    //the same namespace with difference character case, and difference notification id
    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespace, newNotificationId,
            somePublicNameWithIncorrectCase, someOutDatedNotificationId));


    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNameWithIncorrectCase, notifications.get(0).getNamespaceName());
    assertEquals(newNotificationId, notifications.get(0).getNotificationId());

    String key = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);
    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertEquals(newNotificationId, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultiplePublicNamespaceWithIncorrectCase2WithNotificationIdOutDated() throws Exception {
    String publicAppId = "somePublicAppId";
    long someOutDatedNotificationId = 1;
    long newNotificationId = 20;

    String somePublicNameWithIncorrectCase = somePublicNamespace.toUpperCase();

    //the same namespace with difference character case, and difference notification id
    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNameWithIncorrectCase, someOutDatedNotificationId,
            somePublicNamespace, newNotificationId));


    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNameWithIncorrectCase, notifications.get(0).getNamespaceName());
    assertEquals(newNotificationId, notifications.get(0).getNotificationId());

    String key = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);
    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertEquals(newNotificationId, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultiplePublicNamespaceWithIncorrectCase3WithNotificationIdOutDated() throws Exception {
    String publicAppId = "somePublicAppId";
    long someOutDatedNotificationId = 1;
    long newNotificationId = 20;

    String somePublicNameWithIncorrectCase = somePublicNamespace.toUpperCase();

    //the same namespace with difference character case, and difference notification id
    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNameWithIncorrectCase, newNotificationId,
            somePublicNamespace, someOutDatedNotificationId));


    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertEquals(newNotificationId, notifications.get(0).getNotificationId());

    String key = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);
    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertEquals(newNotificationId, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultiplePublicNamespaceWithIncorrectCase4WithNotificationIdOutDated() throws Exception {
    String publicAppId = "somePublicAppId";
    long someOutDatedNotificationId = 1;
    long newNotificationId = 20;

    String somePublicNameWithIncorrectCase = somePublicNamespace.toUpperCase();

    //the same namespace with difference character case, and difference notification id
    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespace, someOutDatedNotificationId,
            somePublicNameWithIncorrectCase, newNotificationId));


    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(1, notifications.size());
    assertEquals(somePublicNamespace, notifications.get(0).getNamespaceName());
    assertEquals(newNotificationId, notifications.get(0).getNotificationId());

    String key = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);
    ApolloNotificationMessages messages = result.getBody().get(0).getMessages();
    assertEquals(1, messages.getDetails().size());
    assertTrue(messages.has(key));
    assertEquals(newNotificationId, messages.get(key).longValue());
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultipleNamespacesAndNotificationIdsOutDated()
      throws Exception {
    String publicAppId = "somePublicAppId";
    long someOutDatedNotificationId = 1;
    long newDefaultNamespaceNotificationId = 10;
    long newPublicNamespaceNotification = 20;

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespace,
            someOutDatedNotificationId, defaultNamespace, someOutDatedNotificationId));

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(2, notifications.size());

    Set<String> outDatedNamespaces =
        Sets.newHashSet(notifications.get(0).getNamespaceName(), notifications.get(1).getNamespaceName());
    assertEquals(Sets.newHashSet(defaultNamespace, somePublicNamespace), outDatedNamespaces);

    Set<Long> newNotificationIds = Sets.newHashSet(
        notifications.get(0).getNotificationId(), notifications.get(1).getNotificationId());
    assertEquals(Sets.newHashSet(newDefaultNamespaceNotificationId, newPublicNamespaceNotification),
        newNotificationIds);

    String defaultNamespaceKey = assembleKey(someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT,
        ConfigConsts.NAMESPACE_APPLICATION);
    String publicNamespaceKey = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);

    ApolloNotificationMessages firstMessages = notifications.get(0).getMessages();
    ApolloNotificationMessages secondMessages = notifications.get(1).getMessages();

    assertEquals(1, firstMessages.getDetails().size());
    assertEquals(1, secondMessages.getDetails().size());

    assertTrue(
        (firstMessages.has(defaultNamespaceKey) && firstMessages.get(defaultNamespaceKey).equals(newDefaultNamespaceNotificationId)) ||
            (firstMessages.has(publicNamespaceKey) && firstMessages.get(publicNamespaceKey).equals(newPublicNamespaceNotification))
    );
    assertTrue(
        (secondMessages.has(defaultNamespaceKey) && secondMessages.get(defaultNamespaceKey).equals(newDefaultNamespaceNotificationId)) ||
            (secondMessages.has(publicNamespaceKey) && secondMessages.get(publicNamespaceKey).equals(newPublicNamespaceNotification))
    );
  }

  @Test(timeout = 5000L)
  @Sql(scripts = "/integration-test/test-release.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/test-release-message.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
  @Sql(scripts = "/integration-test/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
  public void testPollNotificationWithMultipleNamespacesAndNotificationIdsOutDatedAndIncorrectCase()
      throws Exception {
    String publicAppId = "somePublicAppId";
    long someOutDatedNotificationId = 1;
    long newDefaultNamespaceNotificationId = 10;
    long newPublicNamespaceNotification = 20;

    String someDefaultNamespaceWithIncorrectCase = defaultNamespace.toUpperCase();
    String somePublicNamespaceWithIncorrectCase = somePublicNamespace.toUpperCase();

    ResponseEntity<List<ApolloConfigNotification>> result = restTemplate.exchange(
        "http://{baseurl}/notifications/v2?appId={appId}&cluster={clusterName}&notifications={notifications}",
        HttpMethod.GET, null, typeReference,
        getHostUrl(), someAppId, someCluster,
        transformApolloConfigNotificationsToString(somePublicNamespaceWithIncorrectCase,
            someOutDatedNotificationId, someDefaultNamespaceWithIncorrectCase, someOutDatedNotificationId));

    List<ApolloConfigNotification> notifications = result.getBody();
    assertEquals(HttpStatus.OK, result.getStatusCode());
    assertEquals(2, notifications.size());

    Set<String> outDatedNamespaces =
        Sets.newHashSet(notifications.get(0).getNamespaceName(), notifications.get(1).getNamespaceName());
    assertEquals(Sets.newHashSet(someDefaultNamespaceWithIncorrectCase, somePublicNamespaceWithIncorrectCase),
        outDatedNamespaces);

    Set<Long> newNotificationIds = Sets.newHashSet(
        notifications.get(0).getNotificationId(), notifications.get(1).getNotificationId());
    assertEquals(Sets.newHashSet(newDefaultNamespaceNotificationId, newPublicNamespaceNotification),
        newNotificationIds);

    String defaultNamespaceKey = assembleKey(someAppId, ConfigConsts.CLUSTER_NAME_DEFAULT,
        ConfigConsts.NAMESPACE_APPLICATION);
    String publicNamespaceKey = assembleKey(publicAppId, ConfigConsts.CLUSTER_NAME_DEFAULT, somePublicNamespace);

    ApolloNotificationMessages firstMessages = notifications.get(0).getMessages();
    ApolloNotificationMessages secondMessages = notifications.get(1).getMessages();

    assertEquals(1, firstMessages.getDetails().size());
    assertEquals(1, secondMessages.getDetails().size());

    assertTrue(
        (firstMessages.has(defaultNamespaceKey) && firstMessages.get(defaultNamespaceKey).equals(newDefaultNamespaceNotificationId)) ||
            (firstMessages.has(publicNamespaceKey) && firstMessages.get(publicNamespaceKey).equals(newPublicNamespaceNotification))
    );
    assertTrue(
        (secondMessages.has(defaultNamespaceKey) && secondMessages.get(defaultNamespaceKey).equals(newDefaultNamespaceNotificationId)) ||
            (secondMessages.has(publicNamespaceKey) && secondMessages.get(publicNamespaceKey).equals(newPublicNamespaceNotification))
    );
  }

  private String assembleKey(String appId, String cluster, String namespace) {
    return Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).join(appId, cluster, namespace);
  }

  private String transformApolloConfigNotificationsToString(
      String namespace, long notificationId) {
    List<ApolloConfigNotification> notifications =
        Lists.newArrayList(assembleApolloConfigNotification(namespace, notificationId));
    return gson.toJson(notifications);
  }

  private String transformApolloConfigNotificationsToString(String namespace, long notificationId,
                                                            String anotherNamespace,
                                                            long anotherNotificationId) {
    List<ApolloConfigNotification> notifications =
        Lists.newArrayList(assembleApolloConfigNotification(namespace, notificationId),
            assembleApolloConfigNotification(anotherNamespace, anotherNotificationId));
    return gson.toJson(notifications);
  }

  private String transformApolloConfigNotificationsToString(String namespace, long notificationId,
                                                            String anotherNamespace,
                                                            long anotherNotificationId,
                                                            String yetAnotherNamespace,
                                                            long yetAnotherNotificationId) {
    List<ApolloConfigNotification> notifications =
        Lists.newArrayList(assembleApolloConfigNotification(namespace, notificationId),
            assembleApolloConfigNotification(anotherNamespace, anotherNotificationId),
            assembleApolloConfigNotification(yetAnotherNamespace, yetAnotherNotificationId));
    return gson.toJson(notifications);
  }

  private ApolloConfigNotification assembleApolloConfigNotification(String namespace,
                                                                    long notificationId) {
    ApolloConfigNotification notification = new ApolloConfigNotification(namespace, notificationId);
    return notification;
  }
}
