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
package com.ctrip.framework.apollo.portal.controller;

/**
 * @author hujiyuan 2024-08-10
 */

import com.ctrip.framework.apollo.common.http.SearchResponseEntity;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.vo.ItemInfo;
import com.ctrip.framework.apollo.portal.service.GlobalSearchService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(MockitoJUnitRunner.class)
public class GlobalSearchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PortalConfig portalConfig;

    @Mock
    private GlobalSearchService globalSearchService;

    @InjectMocks
    private GlobalSearchController globalSearchController;

    private final int perEnvSearchMaxResults = 200;

    @Before
    public void setUp() {
        when(portalConfig.getPerEnvSearchMaxResults()).thenReturn(perEnvSearchMaxResults);
        mockMvc = MockMvcBuilders.standaloneSetup(globalSearchController).build();
    }

    @Test
    public void testGet_ItemInfo_BySearch_WithKeyAndValueAndActiveEnvs_ReturnEmptyItemInfos() throws Exception {
        when(globalSearchService.getAllEnvItemInfoBySearch(anyString(), anyString(),eq(0),eq(perEnvSearchMaxResults))).thenReturn(SearchResponseEntity.ok(new ArrayList<>()));
        mockMvc.perform(MockMvcRequestBuilders.get("/global-search/item-info/by-key-or-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("key", "query-key")
                        .param("value", "query-value"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"body\":[],\"hasMoreData\":false,\"message\":\"OK\",\"code\":200}"));
        verify(portalConfig,times(1)).getPerEnvSearchMaxResults();
        verify(globalSearchService,times(1)).getAllEnvItemInfoBySearch(anyString(), anyString(),eq(0),eq(perEnvSearchMaxResults));
    }

    @Test
    public void testGet_ItemInfo_BySearch_WithKeyAndValueAndActiveEnvs_ReturnExpectedItemInfos_ButOverPerEnvLimit() throws Exception {
        List<ItemInfo> allEnvMockItemInfos = new ArrayList<>();
        allEnvMockItemInfos.add(new ItemInfo("appid1","env1","cluster1","namespace1","query-key","query-value"));
        allEnvMockItemInfos.add(new ItemInfo("appid2","env2","cluster2","namespace2","query-key","query-value"));
        when(globalSearchService.getAllEnvItemInfoBySearch(eq("query-key"), eq("query-value"),eq(0),eq(perEnvSearchMaxResults))).thenReturn(SearchResponseEntity.okWithMessage(allEnvMockItemInfos,"In DEV , PRO , more than "+perEnvSearchMaxResults+" items found (Exceeded the maximum search quantity for a single environment). Please enter more precise criteria to narrow down the search scope."));
        mockMvc.perform(MockMvcRequestBuilders.get("/global-search/item-info/by-key-or-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("key", "query-key")
                        .param("value", "query-value"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"body\":[" +
                        "            { \"appId\": \"appid1\",\n" +
                        "            \"envName\": \"env1\",\n" +
                        "            \"clusterName\": \"cluster1\",\n" +
                        "            \"namespaceName\": \"namespace1\",\n" +
                        "            \"key\": \"query-key\",\n" +
                        "            \"value\": \"query-value\"}," +
                        "            { \"appId\": \"appid2\",\n" +
                        "            \"envName\": \"env2\",\n" +
                        "            \"clusterName\": \"cluster2\",\n" +
                        "            \"namespaceName\": \"namespace2\",\n" +
                        "            \"key\": \"query-key\",\n" +
                        "            \"value\": \"query-value\"}],\"hasMoreData\":true,\"message\":\"In DEV , PRO , more than 200 items found (Exceeded the maximum search quantity for a single environment). Please enter more precise criteria to narrow down the search scope.\",\"code\":200}"));
        verify(portalConfig,times(1)).getPerEnvSearchMaxResults();
        verify(globalSearchService, times(1)).getAllEnvItemInfoBySearch(eq("query-key"), eq("query-value"),eq(0),eq(perEnvSearchMaxResults));
    }

    @Test
    public void testGet_ItemInfo_BySearch_WithKeyAndValueAndActiveEnvs_ReturnExpectedItemInfos() throws Exception {
        List<ItemInfo> allEnvMockItemInfos = new ArrayList<>();
        allEnvMockItemInfos.add(new ItemInfo("appid1","env1","cluster1","namespace1","query-key","query-value"));
        allEnvMockItemInfos.add(new ItemInfo("appid2","env2","cluster2","namespace2","query-key","query-value"));
        when(globalSearchService.getAllEnvItemInfoBySearch(eq("query-key"), eq("query-value"),eq(0),eq(perEnvSearchMaxResults))).thenReturn(SearchResponseEntity.ok(allEnvMockItemInfos));

        mockMvc.perform(MockMvcRequestBuilders.get("/global-search/item-info/by-key-or-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .param("key", "query-key")
                        .param("value", "query-value"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"body\":[" +
                        "            { \"appId\": \"appid1\",\n" +
                        "            \"envName\": \"env1\",\n" +
                        "            \"clusterName\": \"cluster1\",\n" +
                        "            \"namespaceName\": \"namespace1\",\n" +
                        "            \"key\": \"query-key\",\n" +
                        "            \"value\": \"query-value\"}," +
                        "            { \"appId\": \"appid2\",\n" +
                        "            \"envName\": \"env2\",\n" +
                        "            \"clusterName\": \"cluster2\",\n" +
                        "            \"namespaceName\": \"namespace2\",\n" +
                        "            \"key\": \"query-key\",\n" +
                        "            \"value\": \"query-value\"}],\"hasMoreData\":false,\"message\":\"OK\",\"code\":200}"));
        verify(globalSearchService, times(1)).getAllEnvItemInfoBySearch(eq("query-key"), eq("query-value"),eq(0),eq(perEnvSearchMaxResults));
    }

}
