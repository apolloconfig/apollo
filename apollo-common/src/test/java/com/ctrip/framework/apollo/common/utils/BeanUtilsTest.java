/*
 * Copyright 2024 Apollo Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *
 */
package com.ctrip.framework.apollo.common.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import com.ctrip.framework.apollo.common.exception.BeanUtilsException;

@RunWith(MockitoJUnitRunner.class)
public class BeanUtilsTest {

  @InjectMocks
  private BeanUtils beanUtils;
  List<Integer> someList;
  List<KeyClass> someAnotherList;

  @Before
  public void setUp() {
    someList = new ArrayList<>();
    someAnotherList = new ArrayList<>();
  }

  @Test
  public void testBatchTransformListNotEmpty() {
    someList.add(77);
    assertNotNull(BeanUtils.batchTransform(String.class, someList));
  }

  @Test
  public void testBatchTransformListIsEmpty() {
    assertNotNull(BeanUtils.batchTransform(String.class, someList));
  }

  @Test(expected = BeanUtilsException.class)
  public void testBatchTransformBeanUtilsException() {
    someList.add(77);
    assertNotNull(BeanUtils.batchTransform(null, someList));
  }

  @Test
  public void testBatchTransformSrcIsNull() {
    someList.add(null);
    assertNotNull(BeanUtils.batchTransform(String.class, someList));
  }

  @Test
  public void testMapByKeyEmptyList() {
    assertNotNull(BeanUtils.mapByKey(null, someList));
  }

  class KeyClass {
    String keys;
  }

  @Test
  public void testMapByKeyNotEmptyList() {
    someAnotherList.add(new KeyClass());
    assertNotNull(BeanUtils.mapByKey("keys", someAnotherList));
  }

  @Test(expected = BeanUtilsException.class)
  public void testMapByKeyNotEmptyListThrowsEx() {
    someAnotherList.add(new KeyClass());
    assertNotNull(BeanUtils.mapByKey("wrongKey", someAnotherList));
  }

  @Test
  public void testAggByKeyToListEmpty() {
    assertNotNull(BeanUtils.aggByKeyToList("keys", someAnotherList));
  }

  @Test
  public void testAggByKeyToListNotEmpty() {
    someAnotherList.add(new KeyClass());
    assertNotNull(BeanUtils.aggByKeyToList("keys", someAnotherList));
  }

  @Test(expected = BeanUtilsException.class)
  public void testAggByKeyToListNotEmptyThrowsEx() {
    someAnotherList.add(new KeyClass());
    assertNotNull(BeanUtils.aggByKeyToList("wrongKey", someAnotherList));
  }

  @Test
  public void testToPropertySetEmpty() {
    assertNotNull(BeanUtils.toPropertySet("keys", someAnotherList));
  }

  @Test
  public void testToPropertySetNotEmpty() {
    someAnotherList.add(new KeyClass());
    assertNotNull(BeanUtils.toPropertySet("keys", someAnotherList));
  }

  @Test(expected = BeanUtilsException.class)
  public void testToPropertySetNotEmptyThrowsEx() {
    someAnotherList.add(new KeyClass());
    assertNotNull(BeanUtils.toPropertySet("wrongKey", someAnotherList));
  }

  @Test
  public void testGetAndsetProperty() {
    BeanUtils.setProperty(new KeyClass(), "keys", "value");
    assertNull(BeanUtils.getProperty(new KeyClass(), "keys"));
  }

}
