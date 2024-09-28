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
package com.ctrip.framework.apollo.portal.service;

/**
 * @author hujiyuan 2024-08-10
 */

import com.ctrip.framework.apollo.common.dto.ItemInfoDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.http.SearchResponseEntity;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.entity.vo.ItemInfo;
import com.ctrip.framework.apollo.portal.environment.Env;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GlobalSearchServiceTest {

    @Mock
    private AdminServiceAPI.ItemAPI itemAPI;

    @Mock
    private PortalSettings portalSettings;

    @InjectMocks
    private GlobalSearchService globalSearchService;

    private final List<Env> activeEnvs = new ArrayList<>();

    @Before
    public void setUp() {
        when(portalSettings.getActiveEnvs()).thenReturn(activeEnvs);
    }

    @Test
    public void testGet_PerEnv_ItemInfo_BySearch_withKeyAndValue_ReturnExpectedItemInfos() {
        activeEnvs.add(Env.DEV);
        activeEnvs.add(Env.PRO);

        ItemInfoDTO itemInfoDTO = new ItemInfoDTO("TestApp","TestCluster","TestNamespace","TestKey","TestValue");
        List<ItemInfoDTO> mockItemInfoDTOs = new ArrayList<>();
        mockItemInfoDTOs.add(itemInfoDTO);
        Mockito.when(itemAPI.getPerEnvItemInfoBySearch(any(Env.class), eq("TestKey"), eq("TestValue"), eq(0), eq(1))).thenReturn(new PageDTO<>(mockItemInfoDTOs, PageRequest.of(0, 1), 1L));
        SearchResponseEntity<List<ItemInfo>> mockItemInfos = globalSearchService.getAllEnvItemInfoBySearch("TestKey", "TestValue", 0, 1);
        assertEquals(2, mockItemInfos.getBody().size());

        List<ItemInfo> devMockItemInfos = new ArrayList<>();
        List<ItemInfo> proMockItemInfos = new ArrayList<>();
        List<ItemInfo> allEnvMockItemInfos = new ArrayList<>();
        devMockItemInfos.add(new ItemInfo("TestApp", Env.DEV.getName(), "TestCluster", "TestNamespace", "TestKey", "TestValue"));
        proMockItemInfos.add(new ItemInfo("TestApp", Env.PRO.getName(), "TestCluster", "TestNamespace", "TestKey", "TestValue"));
        allEnvMockItemInfos.addAll(devMockItemInfos);
        allEnvMockItemInfos.addAll(proMockItemInfos);

        verify(itemAPI,times(2)).getPerEnvItemInfoBySearch(any(Env.class), eq("TestKey"), eq("TestValue"), eq(0), eq(1));
        verify(portalSettings,times(1)).getActiveEnvs();
        assertEquals(allEnvMockItemInfos.toString(), mockItemInfos.getBody().toString());
    }

    @Test
    public void testGet_PerEnv_ItemInfo_withKeyAndValue_BySearch_ReturnEmptyItemInfos() {
        activeEnvs.add(Env.DEV);
        activeEnvs.add(Env.PRO);
        Mockito.when(itemAPI.getPerEnvItemInfoBySearch(any(Env.class), anyString(), anyString(), eq(0), eq(1)))
                .thenReturn(new PageDTO<>(new ArrayList<>(), PageRequest.of(0, 1), 0L));
        SearchResponseEntity<List<ItemInfo>> result = globalSearchService.getAllEnvItemInfoBySearch("NonExistentKey", "NonExistentValue", 0, 1);
        assertEquals(0, result.getBody().size());
    }

    @Test
    public void testGet_PerEnv_ItemInfo_BySearch_withKeyAndValue_ReturnExpectedItemInfos_ButOverPerEnvLimit() {
        activeEnvs.add(Env.DEV);
        activeEnvs.add(Env.PRO);

        ItemInfoDTO itemInfoDTO = new ItemInfoDTO("TestApp","TestCluster","TestNamespace","TestKey","TestValue");
        List<ItemInfoDTO> mockItemInfoDTOs = new ArrayList<>();
        mockItemInfoDTOs.add(itemInfoDTO);
        Mockito.when(itemAPI.getPerEnvItemInfoBySearch(any(Env.class), eq("TestKey"), eq("TestValue"), eq(0), eq(1))).thenReturn(new PageDTO<>(mockItemInfoDTOs, PageRequest.of(0, 1), 2L));
        SearchResponseEntity<List<ItemInfo>> mockItemInfos = globalSearchService.getAllEnvItemInfoBySearch("TestKey", "TestValue", 0, 1);
        assertEquals(2, mockItemInfos.getBody().size());

        List<ItemInfo> devMockItemInfos = new ArrayList<>();
        List<ItemInfo> proMockItemInfos = new ArrayList<>();
        List<ItemInfo> allEnvMockItemInfos = new ArrayList<>();
        devMockItemInfos.add(new ItemInfo("TestApp", Env.DEV.getName(), "TestCluster", "TestNamespace", "TestKey", "TestValue"));
        proMockItemInfos.add(new ItemInfo("TestApp", Env.PRO.getName(), "TestCluster", "TestNamespace", "TestKey", "TestValue"));
        allEnvMockItemInfos.addAll(devMockItemInfos);
        allEnvMockItemInfos.addAll(proMockItemInfos);
        String message = "In DEV , PRO , more than 1 items found (Exceeded the maximum search quantity for a single environment). Please enter more precise criteria to narrow down the search scope.";
        verify(itemAPI,times(2)).getPerEnvItemInfoBySearch(any(Env.class), eq("TestKey"), eq("TestValue"), eq(0), eq(1));
        verify(portalSettings,times(1)).getActiveEnvs();
        assertEquals(allEnvMockItemInfos.toString(), mockItemInfos.getBody().toString());
        assertEquals(message, mockItemInfos.getMessage());
    }

}
