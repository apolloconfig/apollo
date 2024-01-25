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

import com.google.common.collect.Lists;

import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.PageDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.portal.component.PortalSettings;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.AppService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * @author lepdou 2021-09-13
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchControllerTest {

  @Mock
  private AppService       appService;
  @Mock
  private NamespaceService namespaceService;
  @Mock
  private PortalSettings   portalSettings;
  @Mock
  private PortalConfig     portalConfig;
  @InjectMocks
  private SearchController searchController;

  @Test
  public void testSearchByEmptyKey() {
    PageRequest request = PageRequest.of(0, 20);
    searchController.search("", request);
    verify(appService, times(1)).findAll(request);
  }

  @Test
  public void testSearchApp() {
    String query = "timeout";
    PageRequest request = PageRequest.of(0, 20);

    PageDTO<App> apps = genPageApp(10, request, 100);

    when(appService.searchByAppIdOrAppName(query, request)).thenReturn(apps);

    searchController.search(query, request);

    verify(appService, times(0)).findAll(request);
    verify(appService, times(1)).searchByAppIdOrAppName(query, request);
  }

  @Test
  public void testSearchItemSwitch() {
    String query = "timeout";
    PageRequest request = PageRequest.of(0, 20);

    PageDTO<App> apps = new PageDTO<>(Lists.newLinkedList(), request, 0);

    when(appService.searchByAppIdOrAppName(query, request)).thenReturn(apps);
    when(portalConfig.supportSearchByItem()).thenReturn(false);

    PageDTO<App> result = searchController.search(query, request);

    Assert.assertFalse(result.hasContent());
    verify(appService, times(0)).findAll(request);
    verify(appService, times(1)).searchByAppIdOrAppName(query, request);
  }

  @Test
  public void testSearchItem() {
    String query = "timeout";
    PageRequest request = PageRequest.of(0, 20);

    PageDTO<App> apps = new PageDTO<>(Lists.newLinkedList(), request, 0);
    PageDTO<NamespaceDTO> devNamespaces = genPageNamespace(10, request, 20);
    PageDTO<NamespaceDTO> fatNamespaces = genPageNamespace(15, request, 30);

    when(appService.searchByAppIdOrAppName(query, request)).thenReturn(apps);
    when(portalConfig.supportSearchByItem()).thenReturn(true);
    when(portalSettings.getActiveEnvs()).thenReturn(Lists.newArrayList(Env.DEV, Env.FAT));
    when(namespaceService.findNamespacesByItem(Env.DEV, query, request)).thenReturn(devNamespaces);
    when(namespaceService.findNamespacesByItem(Env.FAT, query, request)).thenReturn(fatNamespaces);

    PageDTO<App> result = searchController.search(query, request);

    Assert.assertTrue(result.hasContent());
    Assert.assertEquals(25, result.getContent().size());
    Assert.assertEquals(30, result.getTotal());
    verify(appService, times(0)).findAll(request);
    verify(appService, times(1)).searchByAppIdOrAppName(query, request);
    verify(namespaceService).findNamespacesByItem(Env.DEV, query, request);
    verify(namespaceService).findNamespacesByItem(Env.FAT, query, request);
  }

  private PageDTO<App> genPageApp(int size, Pageable pageable, int total) {
    List<App> result = Lists.newLinkedList();
    for (int i = 0; i < size; i++) {
      App app = new App();
      result.add(app);
    }
    return new PageDTO<>(result, pageable, total);
  }

  private PageDTO<NamespaceDTO> genPageNamespace(int size, Pageable pageable, int total) {
    List<NamespaceDTO> result = Lists.newLinkedList();
    for (int i = 0; i < size; i++) {
      NamespaceDTO namespaceDTO = new NamespaceDTO();
      result.add(namespaceDTO);
    }
    return new PageDTO<>(result, pageable, total);
  }
}
