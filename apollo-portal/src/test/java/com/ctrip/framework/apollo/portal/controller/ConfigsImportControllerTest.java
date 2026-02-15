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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.portal.environment.Env;
import com.ctrip.framework.apollo.portal.service.ConfigsImportService;
import com.ctrip.framework.apollo.portal.util.ConfigFileUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@RunWith(MockitoJUnitRunner.class)
public class ConfigsImportControllerTest {

  @Mock
  private ConfigsImportService configsImportService;

  @InjectMocks
  private ConfigsImportController configsImportController;

  @Captor
  private ArgumentCaptor<Boolean> ignoreConflictCaptor;

  @Captor
  private ArgumentCaptor<java.util.List<Env>> envsCaptor;

  private MockMvc mockMvc;

  @Before
  public void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(configsImportController).build();
  }

  @Test
  public void shouldImportConfigFileWithStandardizedFilename() throws IOException {
    MockMultipartFile file =
        new MockMultipartFile("file", "application.yml", "application/yaml", "a: b".getBytes());

    configsImportController.importConfigFile("SampleApp", "DEV", "default", "application", file);

    String expected = ConfigFileUtils.toFilename("SampleApp", "default", "application",
        com.ctrip.framework.apollo.core.enums.ConfigFileFormat.YML);
    verify(configsImportService).forceImportNamespaceFromFile(eq(Env.DEV), eq(expected),
        any(java.io.InputStream.class));
  }

  @Test
  public void shouldUseDefaultIgnoreConflictActionWhenImportingAllConfigs() throws Exception {
    MockMultipartFile zip = new MockMultipartFile("file", "all.zip", "application/zip",
        zipBytes("apollo/demo.txt", "x"));

    mockMvc
        .perform(
            MockMvcRequestBuilders.multipart("/configs/import").file(zip).param("envs", "DEV,FAT"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    verify(configsImportService).importDataFromZipFile(envsCaptor.capture(),
        any(java.util.zip.ZipInputStream.class), ignoreConflictCaptor.capture());
    assertEquals(Arrays.asList(Env.DEV, Env.FAT), envsCaptor.getValue());
    assertEquals(Boolean.TRUE, ignoreConflictCaptor.getValue());
  }

  @Test
  public void shouldParseCoverConflictActionWhenImportingAllConfigs() throws Exception {
    MockMultipartFile zip = new MockMultipartFile("file", "all.zip", "application/zip",
        zipBytes("apollo/demo.txt", "x"));

    mockMvc.perform(MockMvcRequestBuilders.multipart("/configs/import").file(zip)
        .param("envs", "DEV").param("conflictAction", "cover")).andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    verify(configsImportService).importDataFromZipFile(eq(Collections.singletonList(Env.DEV)),
        any(java.util.zip.ZipInputStream.class), eq(false));
  }

  @Test(expected = BadRequestException.class)
  public void shouldRejectInvalidConflictActionWhenImportingAllConfigs() throws IOException {
    MockMultipartFile zip = new MockMultipartFile("file", "all.zip", "application/zip",
        zipBytes("apollo/demo.txt", "x"));

    configsImportController.importConfigByZip("DEV", "invalid", zip);
  }

  @Test
  public void shouldUseDefaultIgnoreConflictActionWhenImportingAppConfigs() throws Exception {
    MockMultipartFile zip = new MockMultipartFile("file", "app.zip", "application/zip",
        zipBytes("SampleApp/DEV/demo", "x"));

    mockMvc
        .perform(MockMvcRequestBuilders
            .multipart("/apps/{appId}/envs/{env}/clusters/{clusterName}/import", "SampleApp", "DEV",
                "default")
            .file(zip))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

    verify(configsImportService).importAppConfigFromZipFile(eq("SampleApp"), eq(Env.DEV),
        eq("default"), any(java.util.zip.ZipInputStream.class), eq(true));
  }

  @Test(expected = BadRequestException.class)
  public void shouldRejectInvalidConflictActionWhenImportingAppConfigs() throws IOException {
    MockMultipartFile zip = new MockMultipartFile("file", "app.zip", "application/zip",
        zipBytes("SampleApp/DEV/demo", "x"));

    configsImportController.importAppConfigByZip("SampleApp", "DEV", "default", "bad", zip);
  }

  private byte[] zipBytes(String entryName, String content) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    try (ZipOutputStream zipOutputStream = new ZipOutputStream(output)) {
      zipOutputStream.putNextEntry(new ZipEntry(entryName));
      zipOutputStream.write(content.getBytes());
      zipOutputStream.closeEntry();
    }
    return output.toByteArray();
  }
}
