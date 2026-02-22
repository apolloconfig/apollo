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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.portal.entity.bo.ItemBO;
import com.ctrip.framework.apollo.portal.entity.bo.NamespaceBO;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ConfigsExportService;
import com.ctrip.framework.apollo.portal.service.NamespaceService;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@RunWith(MockitoJUnitRunner.class)
public class ConfigsExportControllerTest {

  @Mock
  private ConfigsExportService configsExportService;

  @Mock
  private NamespaceService namespaceService;

  @InjectMocks
  private ConfigsExportController configsExportController;

  @Test
  public void shouldExportNamespaceWithPropertiesSuffixWhenMissing() {
    NamespaceBO namespace = new NamespaceBO();
    namespace.setFormat("properties");
    namespace.setItems(Collections.singletonList(itemBO("timeout", "100")));

    when(namespaceService.loadNamespaceBO("SampleApp", Env.DEV, "default", "application", true,
        false)).thenReturn(namespace);

    MockHttpServletResponse response = new MockHttpServletResponse();

    configsExportController.exportItems("SampleApp", "DEV", "default", "application", response);

    assertEquals("attachment;filename=application.properties",
        response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
    assertTrue(new String(response.getContentAsByteArray()).contains("\"key\":\"timeout\""));
  }

  @Test
  public void shouldExportNamespaceKeepOriginalSuffixWhenFormatIsValid() {
    NamespaceBO namespace = new NamespaceBO();
    namespace.setFormat("yml");
    namespace.setItems(Collections.singletonList(itemBO("content", "a: b")));

    when(namespaceService.loadNamespaceBO("SampleApp", Env.DEV, "default", "application.yml", true,
        false)).thenReturn(namespace);

    MockHttpServletResponse response = new MockHttpServletResponse();

    configsExportController.exportItems("SampleApp", "DEV", "default", "application.yml", response);

    assertEquals("attachment;filename=application.yml",
        response.getHeader(HttpHeaders.CONTENT_DISPOSITION));
    assertTrue(new String(response.getContentAsByteArray()).contains("\"key\":\"content\""));
  }

  @Test(expected = ServiceException.class)
  public void shouldWrapExportNamespaceIOExceptionAsServiceException() throws IOException {
    NamespaceBO namespace = new NamespaceBO();
    namespace.setFormat("properties");
    namespace.setItems(Collections.singletonList(itemBO("timeout", "100")));

    when(namespaceService.loadNamespaceBO("SampleApp", Env.DEV, "default", "application", true,
        false)).thenReturn(namespace);

    HttpServletResponse response = org.mockito.Mockito.mock(HttpServletResponse.class);
    when(response.getOutputStream()).thenReturn(new FailingServletOutputStream());

    configsExportController.exportItems("SampleApp", "DEV", "default", "application", response);
  }

  @Test
  public void shouldExportAllConfigsWithParsedEnvs() throws IOException {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRemoteAddr("127.0.0.1");
    request.setRemoteHost("localhost");
    MockHttpServletResponse response = new MockHttpServletResponse();

    doAnswer(invocation -> {
      OutputStream outputStream = invocation.getArgument(0);
      outputStream.write("ok".getBytes());
      return null;
    }).when(configsExportService).exportData(any(OutputStream.class),
        eq(Arrays.asList(Env.DEV, Env.FAT)));

    configsExportController.exportAll("DEV,FAT", request, response);

    verify(configsExportService).exportData(any(OutputStream.class),
        eq(Arrays.asList(Env.DEV, Env.FAT)));
    assertTrue(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)
        .startsWith("attachment;filename=apollo_config_export_"));
    assertEquals("ok", response.getContentAsString());
  }

  @Test
  public void shouldExportAppConfigByEnvAndCluster() throws IOException {
    HttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    doAnswer(invocation -> {
      OutputStream outputStream = invocation.getArgument(3);
      outputStream.write("app".getBytes());
      return null;
    }).when(configsExportService).exportAppConfigByEnvAndCluster(eq("SampleApp"), eq(Env.DEV),
        eq("default"), any(OutputStream.class));

    configsExportController.exportAppConfig("SampleApp", "DEV", "default", request, response);

    verify(configsExportService).exportAppConfigByEnvAndCluster(eq("SampleApp"), eq(Env.DEV),
        eq("default"), any(OutputStream.class));
    assertTrue(response.getHeader(HttpHeaders.CONTENT_DISPOSITION)
        .startsWith("attachment;filename=SampleApp+DEV+default+"));
    assertEquals("app", response.getContentAsString());
  }

  private ItemBO itemBO(String key, String value) {
    ItemDTO itemDTO = new ItemDTO();
    itemDTO.setKey(key);
    itemDTO.setValue(value);
    ItemBO itemBO = new ItemBO();
    itemBO.setItem(itemDTO);
    return itemBO;
  }

  private static class FailingServletOutputStream extends ServletOutputStream {

    @Override
    public void write(int b) throws IOException {
      throw new IOException("forced write failure");
    }

    @Override
    public boolean isReady() {
      return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
      // no-op
    }
  }
}
