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
package com.ctrip.framework.apollo.common.dto;


import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ItemInfoDTOTest {

    private ItemInfoDTO itemInfoDTO;

    @Before
    public void setUp() {
        itemInfoDTO = new ItemInfoDTO("testAppId", "testClusterName", "testNamespaceName", "testKey", "testValue");
    }

    @Test
    public void testGetAppId_ShouldReturnCorrectAppId() {
        assertEquals("testAppId", itemInfoDTO.getAppId());
    }

    @Test
    public void testGetClusterName_ShouldReturnCorrectClusterName() {
        assertEquals("testClusterName", itemInfoDTO.getClusterName());
    }

    @Test
    public void testGetNamespaceName_ShouldReturnCorrectNamespaceName() {
        assertEquals("testNamespaceName", itemInfoDTO.getNamespaceName());
    }

    @Test
    public void testGetKey_ShouldReturnCorrectKey() {
        assertEquals("testKey", itemInfoDTO.getKey());
    }

    @Test
    public void testGetValue_ShouldReturnCorrectValue() {
        assertEquals("testValue", itemInfoDTO.getValue());
    }

    @Test
    public void testToString_ShouldReturnExpectedString() {
        assertEquals("ItemInfoDTO{appId='testAppId', clusterName='testClusterName', namespaceName='testNamespaceName', key='testKey', value='testValue'}", itemInfoDTO.toString());
    }
}
