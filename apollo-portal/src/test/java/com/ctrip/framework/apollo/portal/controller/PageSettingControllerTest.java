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
package com.ctrip.framework.apollo.portal.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.vo.PageSetting;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PageSettingControllerTest {

  @Mock
  private PortalConfig portalConfig;

  @InjectMocks
  private PageSettingController pageSettingController;

  @Test
  public void shouldBuildPageSettingFromPortalConfig() {
    when(portalConfig.wikiAddress()).thenReturn("https://wiki.example.com");
    when(portalConfig.canAppAdminCreatePrivateNamespace()).thenReturn(true);

    PageSetting result = pageSettingController.getPageSetting();

    assertEquals("https://wiki.example.com", result.getWikiAddress());
    assertEquals(true, result.isCanAppAdminCreatePrivateNamespace());
  }
}
