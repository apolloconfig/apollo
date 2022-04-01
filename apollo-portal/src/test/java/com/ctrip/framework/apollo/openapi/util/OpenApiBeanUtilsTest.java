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
package com.ctrip.framework.apollo.openapi.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleDTO;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.dto.NamespaceLockDTO;
import com.ctrip.framework.apollo.common.dto.ReleaseDTO;
import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.openapi.dto.OpenAppNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenClusterDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenGrayReleaseRuleDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenGrayReleaseRuleItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenItemDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenNamespaceLockDTO;
import com.ctrip.framework.apollo.openapi.dto.OpenReleaseDTO;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;

@RunWith(MockitoJUnitRunner.class)
public class OpenApiBeanUtilsTest {

  @InjectMocks
  private OpenApiBeanUtils openApiBeanUtils;

  @Test
  public void testTransformFromItemDTODNotNull() {
    ItemDTO dto = new ItemDTO();
    OpenItemDTO dto1 = OpenApiBeanUtils.transformFromItemDTO(dto);
    assertTrue(dto1 instanceof OpenItemDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformFromItemDTONull() {
    assertNotNull(OpenApiBeanUtils.transformFromItemDTO(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformToItemDTONull() {
    assertNotNull(OpenApiBeanUtils.transformToItemDTO(null));
  }

  @Test
  public void testTransformToItemDTONotNull() {
    OpenItemDTO dto = new OpenItemDTO();
    ItemDTO dto1 = OpenApiBeanUtils.transformToItemDTO(dto);
    assertTrue(dto1 instanceof ItemDTO);
  }

  @Test
  public void testTransformToOpenAppNamespaceDTONotNull() {
    AppNamespace app = new AppNamespace();
    OpenAppNamespaceDTO app1 = OpenApiBeanUtils.transformToOpenAppNamespaceDTO(app);
    assertTrue(app1 instanceof OpenAppNamespaceDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformToOpenAppNamespaceDTONull() {
    assertNotNull(OpenApiBeanUtils.transformToOpenAppNamespaceDTO(null));
  }

  @Test
  public void testTransformToAppNamespaceNotNull() {
    OpenAppNamespaceDTO app = new OpenAppNamespaceDTO();
    AppNamespace app1 = OpenApiBeanUtils.transformToAppNamespace(app);
    assertTrue(app1 instanceof AppNamespace);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformToAppNamespace() {
    assertNotNull(OpenApiBeanUtils.transformToAppNamespace(null));
  }

  @Test
  public void testTransformFromReleaseDTONotNull() {
    ReleaseDTO dto = new ReleaseDTO();
    OpenReleaseDTO dto1 = OpenApiBeanUtils.transformFromReleaseDTO(dto);
    assertTrue(dto1 instanceof OpenReleaseDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformFromReleaseDTONull() {
    assertNotNull(OpenApiBeanUtils.transformFromReleaseDTO(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformFromNamespaceBO() {
    NamespaceBO bo = new NamespaceBO();
    bo.setFormat("sample format");
    bo.setPublic(false);
    bo.setComment("Some comment");
    bo.setBaseInfo(Mockito.mock(NamespaceDTO.class));
    OpenNamespaceDTO dto = OpenApiBeanUtils.transformFromNamespaceBO(bo);
    assertTrue(dto instanceof OpenNamespaceDTO);
    List<ItemBO> someList = new ArrayList<>();
    someList.add(new ItemBO());
    bo.setItems(someList);
    OpenNamespaceDTO dto1 = OpenApiBeanUtils.transformFromNamespaceBO(bo);
    assertTrue(dto1 instanceof OpenNamespaceDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformFromNamespaceBONull() {
    assertNotNull(OpenApiBeanUtils.transformFromNamespaceBO(null));
  }

  @Test
  public void testBatchTransformFromNamespaceBOs() {
    List<NamespaceBO> namespaceBOs = new ArrayList<>();
    NamespaceBO bo = new NamespaceBO();
    bo.setFormat("sample format");
    bo.setPublic(false);
    bo.setComment("Some comment");
    bo.setBaseInfo(Mockito.mock(NamespaceDTO.class));
    namespaceBOs.add(bo);
    List<OpenNamespaceDTO> dto = OpenApiBeanUtils.batchTransformFromNamespaceBOs(namespaceBOs);
    assertNotNull(dto instanceof OpenNamespaceDTO);
  }

  @Test
  public void testBatchTransformFromNamespaceBOsEmptyCollection() {
    List<NamespaceBO> namespaceBOs = new ArrayList<>();
    assertNotNull(OpenApiBeanUtils.batchTransformFromNamespaceBOs(namespaceBOs));
  }

  @Test
  public void testTransformFromNamespaceLockDTO() {
    String namespaceName = "someNamespace";
    NamespaceLockDTO namespaceLock = new NamespaceLockDTO();
    OpenNamespaceLockDTO lock =
        OpenApiBeanUtils.transformFromNamespaceLockDTO(namespaceName, namespaceLock);
    assertTrue(lock instanceof OpenNamespaceLockDTO);
  }

  @Test
  public void testTransformFromNamespaceLockDTONull() {
    assertNotNull(OpenApiBeanUtils.transformFromNamespaceLockDTO("someNamespace", null));
  }

  @Test
  public void testTransformFromGrayReleaseRuleDTO() {
    OpenGrayReleaseRuleDTO dto1 =
        OpenApiBeanUtils.transformFromGrayReleaseRuleDTO(Mockito.mock(GrayReleaseRuleDTO.class));
    assertTrue(dto1 instanceof OpenGrayReleaseRuleDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformFromGrayReleaseRuleDTONull() {
    assertNotNull(OpenApiBeanUtils.transformFromGrayReleaseRuleDTO(null));
  }

  @Test
  public void testTransformToGrayReleaseRuleDTO() {
    OpenGrayReleaseRuleDTO dto = new OpenGrayReleaseRuleDTO();
    Set<OpenGrayReleaseRuleItemDTO> set = new HashSet<>();
    OpenGrayReleaseRuleItemDTO item = new OpenGrayReleaseRuleItemDTO();
    item.setClientAppId("77");
    item.setClientIpList(null);
    item.setClientLabelList(null);
    set.add(item);
    dto.setRuleItems(set);
    GrayReleaseRuleDTO dto1 = OpenApiBeanUtils.transformToGrayReleaseRuleDTO(dto);
    assertTrue(dto1 instanceof GrayReleaseRuleDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformToGrayReleaseRuleDTONull() {
    assertNotNull(OpenApiBeanUtils.transformToGrayReleaseRuleDTO(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformFromAppsNotEmpty() {
    List<App> apps = new ArrayList<>();
    App ap = new App();
    apps.add(ap);
    assertNotNull(OpenApiBeanUtils.transformFromApps(apps));
    apps.add(null);
    assertNotNull(OpenApiBeanUtils.transformFromApps(apps));
  }

  @Test
  public void testTransformFromAppsEmpty() {
    List<App> apps = new ArrayList<>();
    assertNotNull(OpenApiBeanUtils.transformFromApps(apps));
  }

  @Test
  public void testTransformFromClusterDTO() {
    ClusterDTO dto = new ClusterDTO();
    OpenClusterDTO dto1 = OpenApiBeanUtils.transformFromClusterDTO(dto);
    assertTrue(dto1 instanceof OpenClusterDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformFromClusterDTONull() {
    assertNotNull(OpenApiBeanUtils.transformFromClusterDTO(null));
  }

  @Test
  public void testTransformToClusterDTO() {
    OpenClusterDTO dto = new OpenClusterDTO();
    ClusterDTO dto1 = OpenApiBeanUtils.transformToClusterDTO(dto);
    assertTrue(dto1 instanceof ClusterDTO);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testTransformToClusterDTONull() {
    assertNotNull(OpenApiBeanUtils.transformToClusterDTO(null));
  }
}
