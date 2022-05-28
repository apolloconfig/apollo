/*
 * Copyright 2022 Apollo Authors
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
package com.ctrip.framework.apollo.util.yaml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.build.MockInjector;
import com.ctrip.framework.apollo.util.OrderedProperties;
import com.ctrip.framework.apollo.util.factory.PropertiesFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ByteArrayResource;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.constructor.DuplicateKeyException;
import org.yaml.snakeyaml.parser.ParserException;

public class YamlParserTest {

  private YamlParser parser;

  @Before
  public void setUp() throws Exception {
    parser = ApolloInjector.getInstance(YamlParser.class);
  }

  @After
  public void tearDown() throws Exception {
    MockInjector.reset();
  }

  @Test
  public void testValidCases() throws Exception {
    test("case1.yaml");
    test("case3.yaml");
    test("case4.yaml");
    test("case5.yaml");
    test("case6.yaml");
    test("case7.yaml");
  }

  @Test(expected = DuplicateKeyException.class)
  public void testcase2() throws Exception {
    testInvalid("case2.yaml");
  }

  @Test(expected = ParserException.class)
  public void testcase8() throws Exception {
    testInvalid("case8.yaml");
  }

  @Test(expected = ConstructorException.class)
  public void testcase9() throws Exception {
    testInvalid("case9.yaml");
  }

  @Test
  public void testOrderProperties() throws IOException {
    String yamlContent = loadYaml("orderedcase.yaml");

    Properties nonOrderedProperties = parser.yamlToProperties(yamlContent);

    PropertiesFactory propertiesFactory = mock(PropertiesFactory.class);
    when(propertiesFactory.getPropertiesInstance()).thenAnswer(new Answer<Properties>() {
      @Override
      public Properties answer(InvocationOnMock invocation) {
        return new OrderedProperties();
      }
    });
    MockInjector.setInstance(PropertiesFactory.class, propertiesFactory);

    parser = new YamlParser(propertiesFactory);

    Properties orderedProperties = parser.yamlToProperties(yamlContent);

    assertTrue(orderedProperties instanceof OrderedProperties);

    checkPropertiesEquals(nonOrderedProperties, orderedProperties);

    String[] propertyNames = orderedProperties.stringPropertyNames().toArray(new String[0]);

    assertEquals("k2", propertyNames[0]);
    assertEquals("k4", propertyNames[1]);
    assertEquals("k1", propertyNames[2]);
  }

  private void test(String caseName) throws Exception {
    String yamlContent = loadYaml(caseName);

    check(yamlContent);
  }

  private String loadYaml(String caseName) throws IOException {
    File file = new File("src/test/resources/yaml/" + caseName);

    return Files.asCharSource(file, Charsets.UTF_8).read();
  }

  private void testInvalid(String caseName) throws Exception {
    File file = new File("src/test/resources/yaml/" + caseName);

    String yamlContent = Files.asCharSource(file, Charsets.UTF_8).read();

    parser.yamlToProperties(yamlContent);
  }

  private void check(String yamlContent) {
    YamlPropertiesFactoryBean yamlPropertiesFactoryBean = new YamlPropertiesFactoryBean();
    yamlPropertiesFactoryBean.setResources(new ByteArrayResource(yamlContent.getBytes()));
    Properties expected = yamlPropertiesFactoryBean.getObject();

    Properties actual = parser.yamlToProperties(yamlContent);

    assertTrue("expected: " + expected + " actual: " + actual, checkPropertiesEquals(expected, actual));
  }

  private boolean checkPropertiesEquals(Properties expected, Properties actual) {
    if (expected == actual)
      return true;

    if (expected.size() != actual.size())
      return false;

    for (Object key : expected.keySet()) {
      if (!expected.getProperty((String) key).equals(actual.getProperty((String) key))) {
        return false;
      }
    }

    return true;
  }
}
