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

import com.ctrip.framework.apollo.biz.AbstractUnitTest;
import com.ctrip.framework.apollo.biz.MockBeanFactory;
import com.ctrip.framework.apollo.biz.entity.ServerConfig;
import com.ctrip.framework.apollo.biz.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.core.ConfigConsts;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
public class BizDBPropertySourceTest extends AbstractUnitTest {

  @Mock
  private ServerConfigRepository serverConfigRepository;

  @Mock
  private DataSource dataSource;

  @Mock
  private Environment environment;

  private BizDBPropertySource propertySource;

  private String clusterConfigKey = "clusterKey";
  private String clusterConfigValue = "clusterValue";
  private String dcConfigKey = "dcKey";
  private String dcConfigValue = "dcValue";
  private String defaultKey = "defaultKey";
  private String defaultValue = "defaultValue";

  @Before
  public void initTestData() {
    propertySource = spy(new BizDBPropertySource(serverConfigRepository, dataSource, environment));

    List<ServerConfig> configs = Lists.newLinkedList();

    //cluster config
    String cluster = "cluster";
    configs.add(MockBeanFactory.mockServerConfig(clusterConfigKey, clusterConfigValue, cluster));
    String dc = "dc";
    configs.add(MockBeanFactory.mockServerConfig(clusterConfigKey, clusterConfigValue + "dc", dc));
    configs.add(MockBeanFactory.mockServerConfig(clusterConfigKey, clusterConfigValue + ConfigConsts.CLUSTER_NAME_DEFAULT,
                                   ConfigConsts.CLUSTER_NAME_DEFAULT));

    //dc config
    configs.add(MockBeanFactory.mockServerConfig(dcConfigKey, dcConfigValue, dc));
    configs.add(MockBeanFactory.mockServerConfig(dcConfigKey, dcConfigValue + ConfigConsts.CLUSTER_NAME_DEFAULT,
                                   ConfigConsts.CLUSTER_NAME_DEFAULT));

    //default config
    configs.add(MockBeanFactory.mockServerConfig(defaultKey, defaultValue, ConfigConsts.CLUSTER_NAME_DEFAULT));

    System.setProperty(ConfigConsts.APOLLO_CLUSTER_KEY, cluster);

    when(propertySource.getCurrentDataCenter()).thenReturn(dc);
    when(serverConfigRepository.findAll()).thenReturn(configs);
  }

  @After
  public void clear() {
    System.clearProperty(ConfigConsts.APOLLO_CLUSTER_KEY);
  }

  @Test
  public void testGetClusterConfig() {

    propertySource.refresh();

    assertEquals(propertySource.getProperty(clusterConfigKey), clusterConfigValue);
  }

  @Test
  public void testGetDcConfig() {
    propertySource.refresh();

    assertEquals(propertySource.getProperty(dcConfigKey), dcConfigValue);
  }

  @Test
  public void testGetDefaultConfig() {
    propertySource.refresh();


    assertEquals(propertySource.getProperty(defaultKey), defaultValue);
  }

  @Test
  public void testGetNull() {
    propertySource.refresh();
    assertNull(propertySource.getProperty("noKey"));
  }


}
