/*
 * Copyright 2022 Apollo Authors
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.ctrip.framework.apollo.common.exception.BeanUtilsException;

@RunWith(MockitoJUnitRunner.class)
public class BeanUtilsTest {

  @InjectMocks
  private BeanUtils beanUtils;

  @Test
  public void testBatchTransformListNotEmpty() {
    List<Integer> someList = new ArrayList<>();
    someList.add(77);
    assertNotNull(BeanUtils.batchTransform(String.class, someList));
  }

  @Test
  public void testBatchTransformListIsEmpty() {
    List<Integer> someList = new ArrayList<>();
    assertNotNull(BeanUtils.batchTransform(String.class, someList));
  }

  @Test(expected = BeanUtilsException.class)
  public void testBatchTransformBeanUtilsException() {
    List<Integer> someList = new ArrayList<>();
    someList.add(77);
    assertNotNull(BeanUtils.batchTransform(null, someList));
  }

  @Test
  public void testBatchTransformSrcIsNull() {
    List<Integer> someList = new ArrayList<>();
    someList.add(null);
    assertNotNull(BeanUtils.batchTransform(String.class, someList));
  }

  @Test
  public void testMapByKeyEmptyList() {
    List<Integer> someList = new ArrayList<>();
    assertNotNull(BeanUtils.mapByKey(null, someList));

  }

  class KeyClass {
    String keys;
  }

  @Test
  public void testMapByKeyNotEmptyList() {
    List<KeyClass> someList = new ArrayList<>();
    someList.add(new KeyClass());
    assertNotNull(BeanUtils.mapByKey("keys", someList));

  }

  @Test(expected = BeanUtilsException.class)
  public void testMapByKeyNotEmptyListThrowsEx() {
    List<KeyClass> someList = new ArrayList<>();
    someList.add(new KeyClass());
    assertNotNull(BeanUtils.mapByKey("wrongKey", someList));

  }

  @Test
  public void testAggByKeyToListEmpty() {
    List<KeyClass> someList = new ArrayList<>();
    assertNotNull(BeanUtils.aggByKeyToList("keys", someList));

  }

  @Test
  public void testAggByKeyToListNotEmpty() {
    List<KeyClass> someList = new ArrayList<>();
    someList.add(new KeyClass());
    assertNotNull(BeanUtils.aggByKeyToList("keys", someList));

  }

  @Test(expected = BeanUtilsException.class)
  public void testAggByKeyToListNotEmptyThrowsEx() {
    List<KeyClass> someList = new ArrayList<>();
    someList.add(new KeyClass());
    assertNotNull(BeanUtils.aggByKeyToList("wrongKey", someList));

  }

  @Test
  public void testToPropertySetEmpty() {
    List<KeyClass> someList = new ArrayList<>();
    assertNotNull(BeanUtils.toPropertySet("keys", someList));

  }

  @Test
  public void testToPropertySetNotEmpty() {
    List<KeyClass> someList = new ArrayList<>();
    someList.add(new KeyClass());
    assertNotNull(BeanUtils.toPropertySet("keys", someList));

  }

  @Test(expected = BeanUtilsException.class)
  public void testToPropertySetNotEmptyThrowsEx() {
    List<KeyClass> someList = new ArrayList<>();
    someList.add(new KeyClass());
    assertNotNull(BeanUtils.toPropertySet("wrongKey", someList));

  }

  @Test
  public void testGetAndsetProperty() {
    BeanUtils.setProperty(new KeyClass(), "keys", "value");
    assertNull(BeanUtils.getProperty(new KeyClass(), "keys"));

  }



}
