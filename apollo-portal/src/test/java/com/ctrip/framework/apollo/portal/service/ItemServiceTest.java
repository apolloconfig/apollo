/*
 * Copyright 2025 Apollo Authors
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.ItemAPI;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.NamespaceAPI;
import com.ctrip.framework.apollo.portal.api.AdminServiceAPI.ReleaseAPI;
import com.ctrip.framework.apollo.portal.component.txtresolver.ConfigTextResolver;
import com.ctrip.framework.apollo.portal.environment.Env;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

  @Mock
  private NamespaceAPI namespaceAPI;

  @Mock
  private ItemAPI itemAPI;

  @Mock
  private ReleaseAPI releaseAPI;

  @Mock
  private ConfigTextResolver fileTextResolver;

  @Mock
  private ConfigTextResolver propertyResolver;

  private ItemService itemService;

  @BeforeEach
  void setUp() {
    itemService =
        new ItemService(namespaceAPI, itemAPI, releaseAPI, fileTextResolver, propertyResolver);
  }

  @Test
  void loadItemShouldUseEncodedAdminEndpointForPathSeparatorKeys() {
    ItemDTO item = new ItemDTO();
    item.setKey("feature/with-path");
    when(itemAPI.loadItemByEncodeKey(Env.DEV, "app", "default", "application", "feature/with-path"))
        .thenReturn(item);

    ItemDTO result =
        itemService.loadItem(Env.DEV, "app", "default", "application", "feature/with-path");

    assertThat(result).isSameAs(item);
    verify(itemAPI).loadItemByEncodeKey(Env.DEV, "app", "default", "application",
        "feature/with-path");
  }

  @Test
  void loadItemShouldUsePlainAdminEndpointForNormalKeys() {
    ItemDTO item = new ItemDTO();
    item.setKey("feature.enabled");
    when(itemAPI.loadItem(Env.DEV, "app", "default", "application", "feature.enabled"))
        .thenReturn(item);

    ItemDTO result =
        itemService.loadItem(Env.DEV, "app", "default", "application", "feature.enabled");

    assertThat(result).isSameAs(item);
    verify(itemAPI).loadItem(Env.DEV, "app", "default", "application", "feature.enabled");
  }
}
