/*
 * Copyright 2021 Apollo Authors
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
package com.ctrip.framework.apollo.mockserver;

import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.mockserver.ApolloMockServerSpringIntegrationTest.TestConfiguration;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.ApolloConfigChangeListener;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.google.common.util.concurrent.SettableFuture;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.assertEquals;

/**
 * Create by zhangzheng on 8/16/18 Email:zhangzheng@youzan.com
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = TestConfiguration.class)
public class ApolloMockServerSpringIntegrationTest {

  private static final String otherNamespace = "otherNamespace";

  @ClassRule
  public static EmbeddedApollo embeddedApollo = new EmbeddedApollo();

  @Autowired
  private TestBean testBean;

  @Autowired
  private TestInterestedKeyPrefixesBean testInterestedKeyPrefixesBean;

  @Test
  @DirtiesContext
  public void testPropertyInject() {
    assertEquals("value1", testBean.key1);
    assertEquals("value2", testBean.key2);
  }

  @Test
  @DirtiesContext
  public void testListenerTriggeredByAdd() throws InterruptedException, ExecutionException, TimeoutException {
    embeddedApollo.addOrModifyProperty(otherNamespace, "someKey", "someValue");
    ConfigChangeEvent changeEvent = testBean.futureData.get(5000, TimeUnit.MILLISECONDS);
    assertEquals(otherNamespace, changeEvent.getNamespace());
    assertEquals("someValue", changeEvent.getChange("someKey").getNewValue());
  }

  @Test
  @DirtiesContext
  public void testListenerTriggeredByDel()
      throws InterruptedException, ExecutionException, TimeoutException {
    embeddedApollo.deleteProperty(otherNamespace, "key1");
    ConfigChangeEvent changeEvent = testBean.futureData.get(5000, TimeUnit.MILLISECONDS);
    assertEquals(otherNamespace, changeEvent.getNamespace());
    assertEquals(PropertyChangeType.DELETED, changeEvent.getChange("key1").getChangeType());
  }

  @Test
  @DirtiesContext
  public void shouldNotifyOnInterestedPatterns() throws Exception {
    embeddedApollo.addOrModifyProperty(otherNamespace, "server.port", "8080");
    embeddedApollo.addOrModifyProperty(otherNamespace, "server.path", "/apollo");
    embeddedApollo.addOrModifyProperty(otherNamespace, "spring.application.name", "whatever");
    ConfigChangeEvent changeEvent = testInterestedKeyPrefixesBean.futureData.get(5000, TimeUnit.MILLISECONDS);
    assertEquals(otherNamespace, changeEvent.getNamespace());
    assertEquals("8080", changeEvent.getChange("server.port").getNewValue());
    assertEquals("/apollo", changeEvent.getChange("server.path").getNewValue());
  }

  @Test(expected = TimeoutException.class)
  @DirtiesContext
  public void shouldNotNotifyOnUninterestedPatterns() throws Exception {
    embeddedApollo.addOrModifyProperty(otherNamespace, "spring.application.name", "apollo");
    testInterestedKeyPrefixesBean.futureData.get(5000, TimeUnit.MILLISECONDS);
  }

  @EnableApolloConfig
  @Configuration
  static class TestConfiguration {

    @Bean
    public TestBean testBean() {
      return new TestBean();
    }

    @Bean
    public TestInterestedKeyPrefixesBean testInterestedKeyPrefixesBean() {
      return new TestInterestedKeyPrefixesBean();
    }
  }

  private static class TestBean {

    @Value("${key1:default}")
    private String key1;
    @Value("${key2:default}")
    private String key2;

    private SettableFuture<ConfigChangeEvent> futureData = SettableFuture.create();

    @ApolloConfigChangeListener(otherNamespace)
    private void onChange(ConfigChangeEvent changeEvent) {
      futureData.set(changeEvent);
    }
  }

  private static class TestInterestedKeyPrefixesBean {
    private SettableFuture<ConfigChangeEvent> futureData = SettableFuture.create();

    @ApolloConfigChangeListener(value = otherNamespace, interestedKeyPrefixes = "server.")
    private void onChange(ConfigChangeEvent changeEvent) {
      futureData.set(changeEvent);
    }
  }
}
